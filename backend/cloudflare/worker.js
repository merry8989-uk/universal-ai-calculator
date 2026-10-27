/**
 * Nexus Calc — AI backend (Cloudflare Worker, Workers AI).
 *
 * The model runs ON Cloudflare, billed to your Cloudflare account, so there is
 * no third-party API key anywhere — not in the app, not in this repo, and no
 * secret to rotate.
 *
 * Cloudflare retires Workers AI models regularly, so this Worker tries a list
 * of current models and uses the first one that answers. Set AI_MODEL in
 * wrangler.toml (or as a Worker variable) to pin a specific one.
 */

// Tried in order. The first one that answers wins.
const FALLBACK_MODELS = [
  "@cf/google/gemma-4-26b-a4b-it",
  "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
  "@cf/meta/llama-3.1-8b-instruct-fp8",
  "@cf/qwen/qwen3-30b-a3b-fp8",
  "@cf/ibm-granite/granite-4.0-h-micro",
];

// Speech-to-text, tried in order for an uploaded audio clip.
const WHISPER_MODELS = [
  "@cf/openai/whisper-large-v3-turbo",
  "@cf/openai/whisper",
];

// Vision models, tried in order for a photo. Workers AI retires models, so
// several are listed.
// Only this one works for reading arithmetic: llava rejects every input shape
// Workers AI accepts here, and the other vision models are gone or gated.
const VISION_MODELS = [
  "@cf/meta/llama-3.2-11b-vision-instruct",
];

// A caller may name one of these to compare readers. Anything outside the list
// is ignored, so the endpoint cannot be used as a general model proxy.
const VISION_ALLOWED = [
  "@cf/meta/llama-3.2-11b-vision-instruct",
];

// The photo prompt: we want a bare calculator expression back, nothing else.
const OCR_SYSTEM = [
  "You read arithmetic from a photo for a calculator app.",
  "Reply with ONLY a calculator expression made of digits and the characters",
  "+ - * / ^ ( ) % . and the names sqrt, cbrt, sin, cos, tan, ln, log, pi, e.",
  "Use * for multiplication and ^ for powers.",
  "Read the digits carefully, one at a time.",
  "Never add a digit that is not clearly in the image. Do not complete a sum.",
  "If a character is unclear, leave it out rather than guessing.",
  "If there is an equals sign, return only the part BEFORE it, never what follows.",
  "Ignore any question mark, box, tick or handwriting that is not a digit or operator.",
  "If you cannot read any arithmetic, reply with exactly: NONE",
  "Do not explain. Do not use Markdown. Output one line only.",
].join(" ");

// Used when a whole page should be transcribed, not just one calculation.
const TRANSCRIBE_SYSTEM = [
  "You transcribe pages for a calculator app.",
  "Write out the text and any mathematics on the page as plain text.",
  "Keep formulas readable: use digits and + - * / ^ ( ) %, and write powers with ^.",
  "Do not explain anything. Do not use Markdown. Output the page content only.",
  "If the page has no readable text, reply with exactly: NONE",
].join(" ");

const SYSTEM = [
  "You are the built-in assistant of the Nexus Calc Android app.",
  "Answer maths, physics, chemistry, finance, economics, health and everyday",
  "questions clearly and briefly.",
  "Reply in PLAIN TEXT only. Never use Markdown (no **, no *, no #, no backticks)",
  "and never use LaTeX or maths notation (no $, no \\text{}, no \\times, no \\frac).",
  "Write symbols literally, for example: Total = 1200 x 2500 = Rs 30,00,000.",
  "Keep it short: at most a few lines of working, then the final answer on its own line.",
  "Prefer Indian conventions (rupees, lakh/crore, metric, IST) unless the user asks",
  "otherwise. Never claim to be a doctor or a financial adviser; add a short",
  "caution for medical or financial questions.",
].join(" ");

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

/**
 * The most a single request may send, in bytes.
 *
 * The biggest legitimate payload is an audio clip, which the app already caps at
 * forty million characters of base64; this sits above that and still well under
 * anything that would trouble the runtime.
 */
const MAX_BODY_BYTES = 48_000_000;

/** How many requests one caller may make in a minute. */
const RATE_LIMIT = 40;
const RATE_WINDOW_SECONDS = 60;

/** The longest `context` the app may attach to a question. */
const MAX_CONTEXT_CHARS = 2000;

/**
 * Whether this caller is still inside the allowance.
 *
 * Cloudflare's own rate-limiting binding is used when the deployment has one.
 * Otherwise a counter is kept in the sync KV namespace, which every deployment
 * of this Worker already has. If neither is available the request is allowed —
 * a calculator that refuses to work because its counter is missing would be
 * worse than one that is briefly unthrottled.
 */
async function underRateLimit(env, caller) {
  if (env.RATE_LIMITER && typeof env.RATE_LIMITER.limit === "function") {
    try {
      const outcome = await env.RATE_LIMITER.limit({ key: caller });
      return outcome.success !== false;
    } catch {
      return true;
    }
  }
  if (!env.NEXUS_SYNC) return true;
  const minute = Math.floor(Date.now() / 1000 / RATE_WINDOW_SECONDS);
  const key = `rate:${minute}:${caller}`;
  try {
    const seen = Number((await env.NEXUS_SYNC.get(key)) || "0");
    if (seen >= RATE_LIMIT) return false;
    // the key expires with its window, so the namespace does not fill up
    await env.NEXUS_SYNC.put(key, String(seen + 1), {
      expirationTtl: RATE_WINDOW_SECONDS * 2,
    });
    return true;
  } catch {
    return true;
  }
}

/**
 * What a user id may look like.
 *
 * Deliberately narrow: lower-case letters, digits, dot, dash and underscore,
 * three to twenty-four characters. A name that is easy to type on a phone and
 * impossible to confuse with another one.
 */
const ID_PATTERN = /^[a-z0-9._-]{3,24}$/;

/** A user id, folded to the form the server stores. */
function normaliseId(raw) {
  return (typeof raw === "string" ? raw : "").trim().toLowerCase();
}

/** The key the encrypted blob lives under. Derived here, never sent by a client. */
async function slotFor(id) {
  const bytes = new TextEncoder().encode("nexuscalc-account-v1|" + id);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/** Where an account's password verifier is kept. */
function verifierKey(id) {
  return `acct:${id}`;
}

/**
 * Checks that this caller really is the account holder.
 *
 * The verifier is PBKDF2 over the password with a salt derived from the id, so
 * the server holds something it can compare but cannot turn back into the
 * password. The data itself is encrypted with a *different* key that never
 * leaves the device, so even a copy of this table would not open a single
 * calculation.
 *
 * Returns null when the caller is fine, or a Response when it is not.
 */
async function checkAccount(env, id, verifier) {
  if (!env.NEXUS_SYNC) {
    return json({ error: "Accounts are not configured on this deployment." }, 501);
  }
  if (!ID_PATTERN.test(id)) {
    return json({ error: "That is not a valid id." }, 400);
  }
  const stored = await env.NEXUS_SYNC.get(verifierKey(id));
  if (stored === null) {
    return json({ error: "No account with that id.", noAccount: true }, 404);
  }
  if (typeof verifier !== "string" || verifier.length < 32) {
    return json({ error: "A password is needed." }, 400);
  }
  if (stored !== verifier) {
    return json({ error: "That password does not match this id.", wrongPassword: true }, 401);
  }
  return null;
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });
}

/**
 * Belt and braces: the app also cleans the answer, but stripping Markdown and
 * LaTeX here means nothing raw can reach a device even from an older build.
 */
function sanitize(text) {
  let s = String(text == null ? "" : text);
  s = s.replace(/\$\$?/g, "");
  s = s.replace(/\\(?:text|mathrm|mathbf|mathit)\{([^}]*)\}/g, "$1");
  s = s.replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, "$1/$2");
  s = s.replace(/\\sqrt\{([^}]*)\}/g, "sqrt($1)");
  s = s.replace(/\\times/g, " x ").replace(/\\div/g, " / ").replace(/\\cdot/g, " x ");
  s = s.replace(/\\[a-zA-Z]+/g, "");
  s = s.replace(/[{}\\]/g, "");
  s = s.replace(/\*\*/g, "").replace(/__/g, "").replace(/`/g, "").replace(/~~/g, "");
  s = s.replace(/\*([A-Za-z][^*\n]*)\*/g, "$1");
  s = s.replace(/^#{1,6}\s*/gm, "");
  s = s.replace(/^\s*[-*]\s+/gm, "\u2022 ");
  s = s.replace(/[ \t]{2,}/g, " ");
  s = s.replace(/\n{3,}/g, "\n\n");
  return s.trim();
}

/** Pulls the text out of whichever shape the model returned. */
function extractText(out) {
  if (out == null) return "";
  if (typeof out === "string") return out;
  if (typeof out.response === "string") return out.response;
  if (typeof out.result?.response === "string") return out.result.response;
  const choice = out.choices?.[0];
  if (choice && typeof choice.message?.content === "string") return choice.message.content;
  if (choice && typeof choice.text === "string") return choice.text;
  return "";
}

/**
 * The image input shapes Workers AI models accept, tried in order. Meta's
 * vision model is gated behind a licence prompt, and the Llava model wants a
 * plain data URL, so both are handled here.
 */
function imageShapes(model, b64, systemPrompt) {
  const url = `data:image/jpeg;base64,${b64}`;
  const ask = "Read the arithmetic in this photo.";
  const sys = systemPrompt || OCR_SYSTEM;

  if (model.includes("llama-3.2-11b-vision")) {
    return [
      {
        agree: true,
        body: {
          messages: [
            { role: "system", content: sys },
            {
              role: "user",
              content: [
                { type: "text", text: ask },
                { type: "image_url", image_url: { url } },
              ],
            },
          ],
          max_tokens: 256,
        },
      },
      {
        agree: true,
        body: {
          messages: [
            { role: "system", content: sys },
            {
              role: "user",
              content: [
                { type: "text", text: ask },
                { type: "image", image: url },
              ],
            },
          ],
          max_tokens: 256,
        },
      },
    ];
  }

  // llava-style: a flat object with the image as a base64 string
  return [
    { agree: false, body: { image: b64, prompt: `${sys} ${ask}`, max_tokens: 512 } },
  ];
}

/** base64 (no data: prefix) to bytes, which is what Workers AI wants. */
function b64ToBytes(b64) {
  const clean = String(b64).replace(/^data:[^,]*,/, "");
  const bin = atob(clean);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

/** Keeps only what a calculator expression may contain. */
function tidyExpression(text) {
  const first = String(text || "").split("\n").find((l) => l.trim().length) || "";
  let s = first.toLowerCase();
  s = s.replace(/×/g, "*").replace(/÷/g, "/").replace(/−/g, "-").replace(/\s+/g, "");
  s = s.replace(/[^0-9a-z.+\-*/^()%!]/g, "");
  return s;
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response("ok", { headers: CORS });
    if (request.method !== "POST") return json({ error: "Use POST" }, 405);

    // A body ceiling, checked before anything is parsed. Without it a caller can
    // hand this Worker a hundred megabytes of JSON and make it read all of it
    // before deciding it was too big.
    const declared = Number(request.headers.get("content-length") || "0");
    if (declared > MAX_BODY_BYTES) {
      return json({ error: "That request is too large." }, 413);
    }

    // A public endpoint with no ceiling on how often it is called is a way to
    // spend somebody else's quota. The limit is per caller, per minute.
    const caller = request.headers.get("cf-connecting-ip") || "unknown";
    const allowed = await underRateLimit(env, caller);
    if (!allowed) {
      return json({ error: "Too many requests. Please wait a minute." }, 429);
    }

    try {
      const raw = await request.text();
      if (raw.length > MAX_BODY_BYTES) {
        return json({ error: "That request is too large." }, 413);
      }
      const body = JSON.parse(raw || "{}");

      // --- ping: answer at once, so the app can measure a true round trip ---
      if (body?.ping) return json({ pong: true, at: Date.now() });

      // --- account mode: create an id, or prove you own one ---
      const account = typeof body?.account === "string" ? body.account : "";
      if (account) {
        if (!env.NEXUS_SYNC) {
          return json({ error: "Accounts are not configured on this deployment." }, 501);
        }
        const id = normaliseId(body?.id);
        if (!ID_PATTERN.test(id)) {
          return json(
            {
              error:
                "An id is 3 to 24 characters: lower-case letters, numbers, dots, dashes or underscores.",
            },
            400,
          );
        }
        const verifier = typeof body?.verifier === "string" ? body.verifier : "";
        if (verifier.length < 32) {
          return json({ error: "A password is needed." }, 400);
        }

        if (account === "exists") {
          const taken = (await env.NEXUS_SYNC.get(verifierKey(id))) !== null;
          return json({ exists: taken, id });
        }

        if (account === "signup") {
          const taken = (await env.NEXUS_SYNC.get(verifierKey(id))) !== null;
          if (taken) {
            return json({ error: "That id is already taken.", taken: true }, 409);
          }
          await env.NEXUS_SYNC.put(verifierKey(id), verifier);
          return json({ created: true, id, at: Date.now() }, 201);
        }

        if (account === "login") {
          const refusal = await checkAccount(env, id, verifier);
          if (refusal) return refusal;
          const slot = await slotFor(id);
          const hasData = (await env.NEXUS_SYNC.get(`slot:${slot}`)) !== null;
          return json({ ok: true, id, hasData, at: Date.now() });
        }

        return json({ error: "Unknown account action." }, 400);
      }

      // --- sync mode: store or fetch an encrypted blob ---
      // The blob is already encrypted on the device, so the Worker only ever
      // holds unreadable bytes under an opaque slot id.
      const sync = typeof body?.sync === "string" ? body.sync : "";
      if (sync) {
        if (!env.NEXUS_SYNC) {
          return json(
            { error: "Sync storage is not configured on this deployment." },
            501,
          );
        }
        // The slot is derived from the account id here, not sent by the client,
        // so knowing a slot is the same thing as knowing an id — and an id alone
        // is not enough to read or destroy anything.
        const id = normaliseId(body?.id);
        const refusal = await checkAccount(env, id, body?.verifier);
        if (refusal) return refusal;
        const slot = await slotFor(id);

        // Every stored blob carries a version. A write must say which version it
        // is replacing, so a stale device cannot quietly undo a newer backup,
        // and a delete must say so explicitly. Knowing the slot alone is enough
        // to read, but not to destroy.
        if (sync === "put") {
          const blob = typeof body?.blob === "string" ? body.blob : "";
          if (!blob) return json({ error: "Nothing to store." }, 400);
          if (blob.length > 512_000) return json({ error: "That payload is too large." }, 413);

          const current = await env.NEXUS_SYNC.get(`slot:${slot}`);
          const currentVersion = current === null ? 0 : Number(await env.NEXUS_SYNC.get(`ver:${slot}`)) || 1;

          // what the caller believes it is replacing; null means "I expect nothing there"
          const expected = body?.version === undefined || body?.version === null
            ? null
            : Number(body.version);

          if (current !== null && expected === null) {
            return json(
              {
                error: "There is already a backup here. Read it first, then write with its version.",
                version: currentVersion,
              },
              409,
            );
          }
          if (expected !== null && expected !== currentVersion) {
            return json(
              {
                error: "This backup has changed since you read it. Download it again before writing.",
                version: currentVersion,
              },
              409,
            );
          }

          const nextVersion = currentVersion + 1;
          await env.NEXUS_SYNC.put(`slot:${slot}`, blob);
          await env.NEXUS_SYNC.put(`ver:${slot}`, String(nextVersion));
          return json({ stored: true, bytes: blob.length, version: nextVersion, at: Date.now() });
        }

        if (sync === "get") {
          const blob = await env.NEXUS_SYNC.get(`slot:${slot}`);
          if (blob === null) return json({ found: false, version: 0 });
          const version = Number(await env.NEXUS_SYNC.get(`ver:${slot}`)) || 1;
          return json({ found: true, blob, version });
        }

        if (sync === "delete") {
          // deleting is the one action that cannot be undone, so it has to be
          // asked for twice: once with the code, once with the confirmation
          if (body?.confirm !== true) {
            return json(
              { error: "Deleting a backup has to be confirmed.", needsConfirm: true },
              400,
            );
          }
          const current = await env.NEXUS_SYNC.get(`slot:${slot}`);
          const version = current === null ? 0 : Number(await env.NEXUS_SYNC.get(`ver:${slot}`)) || 1;
          await env.NEXUS_SYNC.delete(`slot:${slot}`);
          await env.NEXUS_SYNC.delete(`ver:${slot}`);
          return json({ deleted: true, previousVersion: version });
        }

        return json({ error: "Unknown sync action." }, 400);
      }

      // --- audio mode: transcribe an uploaded clip ---
      const audio = typeof body?.audio === "string" ? body.audio : "";
      if (audio) {
        if (!env.AI) return json({ error: "Workers AI binding is not configured." }, 500);
        if (audio.length > 40_000_000) return json({ error: "That audio file is too large." }, 413);

        const problems = [];
        for (const model of WHISPER_MODELS) {
          // the turbo model wants the clip as a base64 string
          for (const payload of [{ audio }, { audio: `data:audio/wav;base64,${audio}` }]) {
            try {
              const out = await env.AI.run(model, payload);
              const text = (out && (out.text || out.response || "")) + "";
              if (text.trim()) return json({ text: text.trim(), model });
              problems.push(`${model}: empty transcript`);
              break;
            } catch (e) {
              problems.push(`${model}: ${e}`);
            }
          }
        }
        return json(
          { error: "Could not transcribe that audio.", detail: problems.join(" | ") },
          502,
        );
      }

      // --- photo mode: read the maths out of an image ---
      const image = typeof body?.image === "string" ? body.image : "";
      if (image) {
        if (!env.AI) return json({ error: "Workers AI binding is not configured." }, 500);
        if (image.length > 8_000_000) return json({ error: "That photo is too large." }, 413);

        const asked = typeof body?.model === "string" ? body.model : "";
        const models = VISION_ALLOWED.includes(asked) ? [asked] : VISION_MODELS;
        // "text" transcribes a whole page; anything else reads one calculation.
        const transcribe = body?.mode === "text";
        const systemPrompt = transcribe ? TRANSCRIBE_SYSTEM : OCR_SYSTEM;

        // The vision model is not deterministic: the same photo can come back
        // as "1250+3750" or "1250+37505000". So it is asked up to three times
        // and the answer that appears most often wins.
        const problems = [];
        for (const model of models) {
          const votes = new Map();
          let agreed = false;

          for (const shape of imageShapes(model, image, systemPrompt)) {
            for (let attempt = 0; attempt < 3; attempt++) {
              try {
                if (shape.agree && !agreed) {
                  // Meta's vision model needs the licence accepted once.
                  try {
                    await env.AI.run(model, { prompt: "agree" });
                  } catch (e) {
                    problems.push(`${model} agree: ${e}`);
                  }
                  agreed = true;
                }
                const out = await env.AI.run(model, shape.body);
                const raw = extractText(out);
                if (transcribe) {
                  const page = raw.trim();
                  if (page && page.toUpperCase() !== "NONE") {
                    return json({ text: page, model });
                  }
                  break;
                }
                const expression = tidyExpression(raw);
                if (!expression || expression.toUpperCase() === "NONE") break;
                votes.set(expression, (votes.get(expression) || 0) + 1);
                // two matching reads are enough
                if (votes.get(expression) >= 2) {
                  return json({ expression, model, reads: attempt + 1 });
                }
              } catch (e) {
                problems.push(`${model}: ${e}`);
                break;
              }
            }
            if (votes.size) break;
          }

          if (votes.size) {
            // no two reads matched: give back the most common one, flagged
            let best = "";
            let bestCount = 0;
            for (const [expr, count] of votes) {
              if (count > bestCount) { best = expr; bestCount = count; }
            }
            return json({ expression: best, model, confidence: "low", reads: votes.size });
          }
        }
        return json(
          { error: "Could not read maths in that photo.", detail: problems.join(" | ") },
          502,
        );
      }

      const question = typeof body?.question === "string" ? body.question.trim() : "";
      const context = typeof body?.context === "string" ? body.context.trim() : "";
      if (!question) return json({ error: "Ask a question first." }, 400);
      if (question.length > 4000) return json({ error: "Question is too long." }, 400);
      if (context.length > MAX_CONTEXT_CHARS) {
        return json({ error: "The attached context is too long." }, 413);
      }

      if (!env.AI) return json({ error: "Workers AI binding is not configured." }, 500);

      const messages = [
        { role: "system", content: SYSTEM },
        {
          role: "user",
          content: context
            ? `App context: ${context}\n\nQuestion: ${question}`
            : question,
        },
      ];

      // A pinned model (if configured) goes first, then the fallbacks.
      const candidates = [];
      if (env.AI_MODEL) candidates.push(env.AI_MODEL);
      for (const m of FALLBACK_MODELS) if (!candidates.includes(m)) candidates.push(m);

      const problems = [];
      for (const model of candidates) {
        try {
          const out = await env.AI.run(model, { messages, max_tokens: 512 });
          const answer = sanitize(extractText(out));
          if (answer) return json({ answer, model });
          problems.push(`${model}: empty response`);
        } catch (e) {
          problems.push(`${model}: ${e}`);
        }
      }

      return json(
        { error: "No Workers AI model could answer.", detail: problems.join(" | ") },
        502,
      );
    } catch (e) {
      return json({ error: String(e) }, 500);
    }
  },
};
