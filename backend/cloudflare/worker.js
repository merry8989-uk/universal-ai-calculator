/**
 * Nexus Calc — AI backend (Cloudflare Worker, Workers AI).
 *
 * The model runs ON Cloudflare, billed to your Cloudflare account, so there is
 * no third-party API key anywhere — not in the app, not in this repo, and no
 * secret to rotate.
 *
 * Deploy:
 *   cd backend/cloudflare
 *   npm install -g wrangler
 *   wrangler login
 *   wrangler deploy
 */

const DEFAULT_MODEL = "@cf/meta/llama-3.1-8b-instruct";

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
      const model = env.AI_MODEL || DEFAULT_MODEL;

      const messages = [
        { role: "system", content: SYSTEM },
        {
          role: "user",
          content: context
            ? `App context: ${context}\n\nQuestion: ${question}`
            : question,
        },
      ];

      const out = await env.AI.run(model, { messages, max_tokens: 512 });
      const answer =
        typeof out === "string" ? out : (out?.response ?? out?.result?.response ?? "");
      if (!answer) return json({ error: "The model returned no answer." }, 502);

      return json({ answer, model });
    } catch (e) {
      return json({ error: String(e) }, 500);
    }
  },
};
