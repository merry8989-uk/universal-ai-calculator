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

// Vision models, tried in order for a photo. Workers AI retires models, so
// several are listed.
const VISION_MODELS = [
  "@cf/meta/llama-3.2-11b-vision-instruct",
  "@cf/llava-hf/llava-1.5-7b-hf",
  "@cf/unum/uform-gen2-qwen-500m",
];

// The photo prompt: we want a bare calculator expression back, nothing else.
const OCR_SYSTEM = [
  "You read arithmetic from a photo for a calculator app.",
  "Reply with ONLY a calculator expression made of digits and the characters",
  "+ - * / ^ ( ) % . and the names sqrt, cbrt, sin, cos, tan, ln, log, pi, e.",
  "Use * for multiplication and ^ for powers.",
  "If the photo shows an equation, return the left-hand side only.",
  "If you cannot read any arithmetic, reply with exactly: NONE",
  "Do not explain. Do not use Markdown. Output one line only.",
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

    try {
      const body = await request.json().catch(() => ({}));

      // --- photo mode: read the maths out of an image ---
      const image = typeof body?.image === "string" ? body.image : "";
      if (image) {
        if (!env.AI) return json({ error: "Workers AI binding is not configured." }, 500);
        if (image.length > 6_000_000) return json({ error: "That photo is too large." }, 413);
        let bytes;
        try {
          bytes = b64ToBytes(image);
        } catch (e) {
          return json({ error: "The photo could not be read." }, 400);
        }
        const problems = [];
        for (const model of VISION_MODELS) {
          try {
            const out = await env.AI.run(model, {
              messages: [
                { role: "system", content: OCR_SYSTEM },
                {
                  role: "user",
                  content: [
                    { type: "text", text: "Read the arithmetic in this photo." },
                    { type: "image", image: bytes },
                  ],
                },
              ],
              max_tokens: 256,
            });
            const raw = extractText(out);
            const expression = tidyExpression(raw);
            if (expression && expression.toUpperCase() !== "NONE") {
              return json({ expression, model });
            }
            problems.push(`${model}: nothing readable`);
          } catch (e) {
            problems.push(`${model}: ${e}`);
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
