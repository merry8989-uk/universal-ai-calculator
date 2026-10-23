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

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response("ok", { headers: CORS });
    if (request.method !== "POST") return json({ error: "Use POST" }, 405);

    try {
      const body = await request.json().catch(() => ({}));
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
          const answer = extractText(out);
          if (answer.trim()) return json({ answer: answer.trim(), model });
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
