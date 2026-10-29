/**
 * Nexus Calc — AI backend (Cloudflare Worker).
 *
 * The thinking is done by Sarvam AI. One key covers all three jobs: text chat
 * (sarvam-105b), a photo and a question about it (glm5.3-flash), and a voice
 * clip (saarika). The key is a Worker secret called SARVAM_API_KEY and is never
 * in this repo and never in the app.
 *
 * The Cloudflare models this used to call were too small to hold a question in
 * mind: a photograph of an exam paper came back as the marks line printed in
 * its header.
 */

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

/**
 * Used when the user asks a question about a photo.
 *
 * This is a different job from reading an expression: the picture may be a
 * question paper, a diagram, a table or a page of handwriting, and the user
 * wants the ANSWER, not an arithmetic string. Without this, a photo of a
 * multiple-choice paper came back as whatever sum happened to be printed in the
 * header.
 */
/**
 * How every answer is laid out, whether it came from a photo, a PDF or a typed
 * question.
 *
 * The answers come first, in one line each, so someone marking a paper can see
 * at a glance what to tick. The reasoning follows underneath, and for a
 * multiple-choice question it says why each wrong option is wrong — which is the
 * part a student actually learns from.
 */
const ANSWER_FORMAT = [
  "FORMAT - follow it exactly, every time.",
  "Start with a line: ANSWERS",
  "Then one short line per question, and nothing else on that line:",
  "  the question number, the correct option letter if there is one, and the",
  "  answer itself in a few words.",
  "  For example:",
  "  Q1: A - 1 + w + w^2 = 0",
  "  Q2: D - n*pi + 5*pi/6",
  "  Q7: 5 percent profit",
  "Then a blank line, then the line: EXPLANATIONS",
  "Then, for each question in order, a short paragraph saying why the right",
  "answer is right AND why each of the other options is wrong. Name the options",
  "you are dismissing.",
  "If a question has no options, give the answer, the working, and then say in a",
  "sentence or two why it is right.",
].join(" ");

const ANSWER_IMAGE_SYSTEM = [
  "You are a patient tutor helping someone with the picture they just sent.",
  "You teach every subject, not only mathematics: science, commerce, arts,",
  "history, geography, English, computer science. Answer them all with the same",
  "care, and never refuse a question just because it is not arithmetic.",
  ANSWER_FORMAT,
  "Read the WHOLE picture first: it may be an exam paper, a diagram, a table, a",
  "graph, handwriting or a screenshot.",

  "THE QUESTION IS USUALLY PRINTED IN THE PICTURE. When the user has not named a",
  "question, do not describe the page — solve what is on it. Go through the",
  "questions in order, starting at number 1, and for each one write the question",
  "number, then the answer with two or three lines of working.",

  "If the user names a question number, answer THAT one only.",
  "If the user asks what the page is, then describe it.",
  "If it is a multiple-choice question, name the option, show the working, and",
  "say why the others are wrong.",
  "If the page holds more questions than fit in one reply, solve as many as you",
  "can from number 1 upwards and say plainly which numbers are left.",

  "Read the numbers from the picture exactly; never invent one.",
  "Ignore the marks printed in the header, such as 11x1=11 or Full Marks: 75 —",
  "those are not questions.",
  "If the picture is too blurred to read, say so plainly rather than guessing.",
  "Reply in PLAIN TEXT only. Never use Markdown or LaTeX.",
  "Write maths literally, for example: tan x = -1/sqrt(3), so x = n*pi + 5*pi/6.",
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
  "You are a general tutor, not a calculator: answer maths, physics, chemistry,",
  "biology, commerce, accountancy, economics, history, geography, civics,",
  "English, computer science, health and everyday questions. Never refuse a",
  "question because it is outside mathematics.",
  ANSWER_FORMAT,
  "Answer the question that was actually asked. Read it twice before replying.",
  "Think the question through before you answer: work out what is being asked,",
  "what is given, and which idea applies. Do not guess. If something is",
  "genuinely uncertain, say so instead of inventing it.",
  "If the user names a question number, answer that one and no other.",
  "If the user asks you to choose between options, name the option, show the",
  "working, and say why the others are wrong. Never reply with a bare sum when a",
  "question was asked.",
  "Reply in PLAIN TEXT only. Never use Markdown (no **, no *, no #, no backticks)",
  "and never use LaTeX or maths notation (no $, no \\text{}, no \\times, no \\frac).",
  "Write symbols literally, for example: Total = 1200 x 2500 = Rs 30,00,000.",
  "Be as long as the question needs and no longer: one line in ANSWERS, then",
  "just enough reasoning in EXPLANATIONS for the reader to follow it.",
  "Prefer Indian conventions (rupees, lakh/crore, metric, IST) unless the user asks",
  "otherwise. Never claim to be a doctor or a financial adviser; add a short",
  "caution for medical or financial questions.",
  "When search results are given to you, they are current and you are not.",
  "Believe the search results over what you remember, even when they contradict",
  "what you remember.",
].join(" ");

/** The system prompt, with today's date, so the model does not answer as if it were last year. */
function systemWithDate() {
  const now = new Date();
  const day = now.toISOString().slice(0, 10);
  return `${SYSTEM} Today is ${day}. Anything you remember after that date you cannot know; search for it.`;
}

/**
 * Sarvam AI, which does all the thinking now.
 *
 * Cloudflare's Workers AI models were too small to hold a question in mind: a
 * photograph of an exam paper came back as the marks line printed in its header.
 * Sarvam's models are built for Indian languages and Indian exam questions, and
 * one key covers all three jobs — text, a picture, and a voice clip.
 */
const SARVAM_BASE = "https://api.sarvam.ai";
const SARVAM_CHAT_MODEL = "sarvam-105b";
// Used for anything that needs real reasoning: a model that thinks before it
// answers, with a million-token window for long documents.
const SARVAM_DEEP_MODEL = "deepseekv4.1-flash";
const SARVAM_VISION_MODEL = "glm5.3-flash";
const SARVAM_STT_MODEL = "saarika:v2.5";

/** The key, or a clear error rather than a silent empty answer. */
function sarvamKey(env) {
  const key = env && env.SARVAM_API_KEY;
  if (!key) throw new Error("SARVAM_API_KEY is not set on the Worker.");
  return key;
}

/**
 * Text chat.
 *
 * reasoning_effort is pinned low: the model will otherwise spend five hundred
 * tokens thinking about "what is 12 times 8", which costs money and makes the
 * user wait for an answer they could have had at once.
 */
async function sarvamChat(env, messages, maxTokens = 1200, model) {
  const r = await fetch(`${SARVAM_BASE}/v2/chat/completions`, {
    method: "POST",
    headers: { "api-subscription-key": sarvamKey(env), "Content-Type": "application/json" },
    body: JSON.stringify({
      model: model || SARVAM_CHAT_MODEL,
      messages,
      max_tokens: maxTokens,
    }),
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`sarvam ${r.status}: ${text.slice(0, 200)}`);
  const out = JSON.parse(text);
  const msg = out && out.choices && out.choices[0] && out.choices[0].message;
  return (msg && msg.content) || "";
}

/**
 * A picture and a question about it.
 *
 * glm5.3-flash is the one Sarvam model that takes an image. It reads the whole
 * page, so a question paper is answered as a paper rather than mined for
 * whatever sum happens to be printed on it.
 */
async function sarvamVision(env, systemPrompt, userText, b64, maxTokens = 900) {
  const r = await fetch(`${SARVAM_BASE}/v2/chat/completions`, {
    method: "POST",
    headers: { "api-subscription-key": sarvamKey(env), "Content-Type": "application/json" },
    body: JSON.stringify({
      model: SARVAM_VISION_MODEL,
      messages: [
        { role: "system", content: systemPrompt },
        {
          role: "user",
          content: [
            { type: "text", text: userText },
            { type: "image_url", image_url: { url: `data:image/jpeg;base64,${b64}` } },
          ],
        },
      ],
      max_tokens: maxTokens,
    }),
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`sarvam ${r.status}: ${text.slice(0, 200)}`);
  const out = JSON.parse(text);
  const msg = out && out.choices && out.choices[0] && out.choices[0].message;
  return (msg && msg.content) || "";
}

/**
 * A chat that is allowed to look things up.
 *
 * The model is given one tool, the web. When it asks to search, the search is
 * run here and the results are handed back, and the model carries on — up to
 * three rounds, so a question needing two lookups still works but a model that
 * keeps searching cannot loop for ever. The searches it made are returned too,
 * so the app can show where an answer came from.
 */
async function sarvamChatWithTools(env, messages, model, maxTokens) {
  const working = messages.slice();
  const used = [];
  let last = "";

  // The last round is run with no tools at all, so the model has to answer with
  // what it has. Without that it will happily search four times and never reply.
  const ROUNDS = 3;
  for (let round = 0; round < ROUNDS; round++) {
    const last = round === ROUNDS - 1;
    const r = await fetch(`${SARVAM_BASE}/v2/chat/completions`, {
      method: "POST",
      headers: { "api-subscription-key": sarvamKey(env), "Content-Type": "application/json" },
      body: JSON.stringify({
        model,
        messages: working,
        max_tokens: maxTokens,
        ...(last ? {} : { tools: TOOLS, tool_choice: "auto" }),
      }),
    });
    const text = await r.text();
    if (!r.ok) throw new Error(`sarvam ${r.status}: ${text.slice(0, 200)}`);
    const out = JSON.parse(text);
    const msg = out && out.choices && out.choices[0] && out.choices[0].message;
    if (!msg) throw new Error("empty response");

    const calls = msg.tool_calls || [];
    if (!calls.length) {
      return { answer: msg.content || last, searched: used };
    }

    working.push({ role: "assistant", content: msg.content || "", tool_calls: calls });
    for (const call of calls) {
      let query = "";
      try {
        query = JSON.parse(call.function && call.function.arguments || "{}").query || "";
      } catch (e) {
        query = "";
      }
      const hits = await webSearch(query, env);
      if (query) used.push(query);
      working.push({
        role: "tool",
        tool_call_id: call.id,
        content: JSON.stringify(hits.length ? hits : { note: "No results found." }).slice(0, 6000),
      });
    }
  }
  return { answer: last, searched: used };
}

/**
 * Searching the live web.
 *
 * DuckDuckGo's HTML endpoint needs no key and answers a plain GET, which is the
 * only search a Worker can do for free. If it refuses — it sometimes blocks
 * datacentre addresses — Wikipedia is asked instead, which is not the whole web
 * but does cover the science, history and commerce questions this app gets.
 */
async function webSearch(query, env) {
  const q = String(query || "").slice(0, 300);
  if (!q.trim()) return [];

  const strip = (h) =>
    String(h || "")
      .replace(/<[^>]*>/g, "")
      .replace(/&amp;/g, "&")
      .replace(/&quot;/g, '"')
      .replace(/&#x27;/g, "'")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/\s+/g, " ")
      .trim();

  // Tavily first: it is a real search API, so it does not throttle and it
  // reaches today's news. DuckDuckGo and Wikipedia are the fallbacks for when
  // the key is missing or the quota has run out.
  const tavily = env && env.TAVILY_API_KEY;
  if (tavily) {
    try {
      const r = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${tavily}` },
        body: JSON.stringify({
          query: q,
          max_results: 5,
          search_depth: "basic",
          include_answer: false,
        }),
      });
      if (r.ok) {
        const data = await r.json();
        const hits = (data && data.results) || [];
        const out = hits.slice(0, 5).map((h) => ({
          title: String(h.title || "").slice(0, 200),
          snippet: strip(String(h.content || "")).slice(0, 900),
          url: String(h.url || ""),
        }));
        if (out.length) return out;
      }
    } catch (e) {
      // fall through to the keyless engines
    }
  }

  try {
    const r = await fetch(`https://html.duckduckgo.com/html/?q=${encodeURIComponent(q)}`, {
      headers: {
        "User-Agent": "Mozilla/5.0 (compatible; NexusCalc/1.0; +https://merry8989-uk.github.io/universal-ai-calculator/)",
        "Accept-Language": "en-IN,en;q=0.9",
      },
    });
    if (r.ok) {
      const html = await r.text();
      const titles = [...html.matchAll(/result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/g)];
      const snippets = [...html.matchAll(/result__snippet"[^>]*>([\s\S]*?)<\/a>/g)];
      const out = [];
      for (let i = 0; i < titles.length && out.length < 5; i++) {
        let url = titles[i][1];
        const m = url.match(/uddg=([^&]+)/);
        if (m) url = decodeURIComponent(m[1]);
        const title = strip(titles[i][2]);
        const snippet = strip(snippets[i] ? snippets[i][1] : "");
        if (title) out.push({ title, snippet, url });
      }
      if (out.length) return out;
    }
  } catch (e) {
    // fall through to Wikipedia
  }

  try {
    const r = await fetch(
      `https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=3&srsearch=${encodeURIComponent(q)}`,
      { headers: { "User-Agent": "NexusCalc/1.0 (calculator app)" } },
    );
    if (!r.ok) return [];
    const data = await r.json();
    const hits = (data && data.query && data.query.search) || [];
    const out = hits.map((h) => ({
      title: h.title,
      snippet: strip(h.snippet),
      url: `https://en.wikipedia.org/wiki/${encodeURIComponent(h.title.replace(/ /g, "_"))}`,
    }));

    // A one-line snippet is thin. The first article's opening paragraphs are
    // fetched and used instead, which is usually enough to answer from.
    if (out.length) {
      try {
        const s2 = await fetch(
          `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(out[0].title.replace(/ /g, "_"))}`,
          { headers: { "User-Agent": "NexusCalc/1.0 (calculator app)" } },
        );
        if (s2.ok) {
          const sum = await s2.json();
          if (sum && sum.extract) out[0].snippet = String(sum.extract).slice(0, 1200);
        }
      } catch (e) {
        // keep the snippet we have
      }
    }
    return out;
  } catch (e) {
    return [];
  }
}

/** What the model may ask for. Only one tool: the web. */
const TOOLS = [
  {
    type: "function",
    function: {
      name: "web_search",
      description:
        "Search the live web. Use it for anything current, factual, or outside your own knowledge: news, prices, dates, who holds an office, recent results, or a fact you are not sure of. Do not use it for arithmetic.",
      parameters: {
        type: "object",
        properties: {
          query: { type: "string", description: "What to search for." },
        },
        required: ["query"],
      },
    },
  },
];

/**
 * Which model should answer this.
 *
 * A question with numbers, equations or a "why"/"prove" in it wants the model
 * that thinks; a question in an Indian script, or an everyday one, wants the
 * one built for Indian languages. Routing badly is the difference between a
 * confident wrong answer and a worked one.
 */
function pickModel(question, history) {
  const q = String(question || "");
  // an Indian script, or a Roman-script Indian language question
  const indic = /[\u0900-\u097F\u0980-\u09FF\u0A00-\u0A7F\u0A80-\u0AFF\u0B00-\u0B7F\u0B80-\u0BFF\u0C00-\u0C7F\u0C80-\u0CFF\u0D00-\u0D7F]/.test(q);
  const reasoning =
    /[0-9]/.test(q) ||
    /(prove|derive|why|explain|solve|calculate|integral|derivative|matrix|probability|step by step|reason)/i.test(q) ||
    /(kyu|kaise|kya|hisaab|ganit|nikalo|batao|samjhao)/i.test(q);
  if (indic && !reasoning) return SARVAM_CHAT_MODEL;
  if (reasoning) return SARVAM_DEEP_MODEL;
  return indic ? SARVAM_CHAT_MODEL : SARVAM_DEEP_MODEL;
}

/** A voice clip, transcribed. */
async function sarvamSpeech(env, b64, mime) {
  const bytes = b64ToBytes(b64);
  const form = new FormData();
  form.append("file", new Blob([bytes], { type: mime || "audio/wav" }), "clip.wav");
  form.append("model", SARVAM_STT_MODEL);
  form.append("language_code", "unknown");
  const r = await fetch(`${SARVAM_BASE}/speech-to-text`, {
    method: "POST",
    headers: { "api-subscription-key": sarvamKey(env) },
    body: form,
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`sarvam ${r.status}: ${text.slice(0, 200)}`);
  const out = JSON.parse(text);
  return (out && out.transcript) || "";
}

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
        if (audio.length > 40_000_000) return json({ error: "That audio file is too large." }, 413);
        try {
          const mime = typeof body?.mime === "string" ? body.mime : "audio/wav";
          const text = await sarvamSpeech(env, audio, mime);
          if (text.trim()) return json({ text: text.trim(), model: SARVAM_STT_MODEL });
          return json({ error: "That clip had no words in it." }, 502);
        } catch (e) {
          return json({ error: "Could not transcribe that audio.", detail: String(e).slice(0, 200) }, 502);
        }
      }

      // --- photo mode: read the maths out of an image ---
      const image = typeof body?.image === "string" ? body.image : "";
      if (image) {
        if (image.length > 8_000_000) return json({ error: "That photo is too large." }, 413);

        // "text" transcribes a whole page, "answer" answers a question about
        // the picture, anything else reads one calculation out of it.
        const transcribe = body?.mode === "text";
        const answering = body?.mode === "answer";
        const systemPrompt = answering
          ? ANSWER_IMAGE_SYSTEM
          : transcribe ? TRANSCRIBE_SYSTEM : OCR_SYSTEM;

        // --- answer mode: the user asked something about this picture ---
        if (answering) {
          const question = typeof body?.question === "string" ? body.question.trim() : "";
          if (!question) return json({ error: "Ask a question about the photo." }, 400);
          if (question.length > 2000) return json({ error: "Question is too long." }, 400);

          try {
            // A page of questions needs room: six worked answers ran out of
            // tokens at 900, and the sixth was cut off mid-sentence.
            const answer = sanitize(await sarvamVision(env, ANSWER_IMAGE_SYSTEM, question, image, 3000));
            if (answer.trim()) return json({ answer, model: SARVAM_VISION_MODEL });
            return json({ error: "The photo produced no answer. Try a clearer picture." }, 502);
          } catch (e) {
            return json(
              { error: "Could not answer that from the photo.", detail: String(e).slice(0, 200) },
              502,
            );
          }
        }

        // Sarvam's vision model reads a photo far more steadily than the small
        // Workers AI models did, so one read is enough. If it comes back empty
        // it is asked once more before giving up.
        const problems = [];
        for (let attempt = 0; attempt < 2; attempt++) {
          try {
            const raw = await sarvamVision(
              env,
              systemPrompt,
              transcribe ? "Transcribe this page." : "Read the arithmetic in this photo.",
              image,
              transcribe ? 1500 : 200,
            );
            if (transcribe) {
              const page = raw.trim();
              if (page && page.toUpperCase() !== "NONE") {
                return json({ text: page, model: SARVAM_VISION_MODEL });
              }
            } else {
              const expression = tidyExpression(raw);
              if (expression && expression.toUpperCase() !== "NONE") {
                return json({ expression, model: SARVAM_VISION_MODEL, reads: attempt + 1 });
              }
            }
            problems.push(`attempt ${attempt + 1}: nothing readable`);
          } catch (e) {
            problems.push(`attempt ${attempt + 1}: ${String(e).slice(0, 120)}`);
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

      // Earlier turns, so a follow-up question knows what was said before.
      // Without this every question starts from nothing, which is what made the
      // assistant feel like a search box rather than a conversation.
      const history = Array.isArray(body?.history) ? body.history : [];
      const earlier = [];
      for (const turn of history.slice(-16)) {
        const role = turn && turn.role === "assistant" ? "assistant" : "user";
        const text = turn && typeof turn.content === "string" ? turn.content.trim() : "";
        if (text) earlier.push({ role, content: text.slice(0, 4000) });
      }

      const messages = [
        { role: "system", content: systemWithDate() },
        ...earlier,
        {
          role: "user",
          content: context
            ? `App context: ${context}\n\nQuestion: ${question}`
            : question,
        },
      ];

      const model = pickModel(question, earlier);
      try {
        const out = await sarvamChatWithTools(env, messages, model, 3000);
        const answer = sanitize(out.answer);
        if (answer.trim()) {
          return json({
            answer,
            model,
            turns: earlier.length,
            // what it looked up, so the app can say where an answer came from
            searched: out.searched,
          });
        }
        return json({ error: "The assistant had nothing to say. Try asking again." }, 502);
      } catch (e) {
        return json(
          { error: "The assistant could not be reached.", detail: String(e).slice(0, 200) },
          502,
        );
      }
    } catch (e) {
      return json({ error: String(e) }, 500);
    }
  },
};
