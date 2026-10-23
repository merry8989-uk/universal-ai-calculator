/**
 * Nexus Calc — AI backend (Cloudflare Worker).
 *
 * Holds the Gemini API key on the server so the app never ships it.
 * The key is a Worker secret named GEMINI_API_KEY — it is NOT in this file
 * and never gets committed.
 *
 * Deploy:
 *   cd backend/cloudflare
 *   npm install -g wrangler
 *   wrangler login
 *   wrangler secret put GEMINI_API_KEY     # paste the key when prompted
 *   wrangler deploy
 */

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

      const key = env.GEMINI_API_KEY;
      if (!key) return json({ error: "Server is not configured." }, 500);
      const model = env.GEMINI_MODEL || "gemini-3.5-flash";

      const prompt = context
        ? `${SYSTEM}\n\nApp context: ${context}\n\nUser question: ${question}`
        : `${SYSTEM}\n\nUser question: ${question}`;

      const resp = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
        },
      );

      const data = await resp.json().catch(() => null);
      if (!resp.ok) {
        return json({ error: data?.error?.message ?? `AI request failed (${resp.status})` }, 502);
      }
      const answer =
        data?.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";
      if (!answer) return json({ error: "The model returned no answer." }, 502);

      return json({ answer, model });
    } catch (e) {
      return json({ error: String(e) }, 500);
    }
  },
};
