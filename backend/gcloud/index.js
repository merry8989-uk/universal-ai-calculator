/**
 * Nexus Calc — AI backend (Google Cloud Function, 2nd gen).
 *
 * Holds the Gemini API key in the function's environment, never in the app.
 *
 * Deploy:
 *   cd backend/gcloud
 *   gcloud functions deploy ask-ai --gen2 --runtime=nodejs20 \
 *     --region=asia-south1 --trigger-http --allow-unauthenticated \
 *     --set-env-vars GEMINI_API_KEY=YOUR_KEY_HERE
 *
 * Or keep the key out of the shell history with Secret Manager:
 *   gcloud secrets create GEMINI_API_KEY --data-file=-
 *   gcloud functions deploy ask-ai --gen2 --runtime=nodejs20 --region=asia-south1 \
 *     --trigger-http --allow-unauthenticated \
 *     --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest
 */

const functions = require("@google-cloud/functions-framework");

const SYSTEM = [
  "You are the built-in assistant of the Nexus Calc Android app.",
  "Answer maths, physics, chemistry, finance, economics, health and everyday",
  "questions clearly and briefly. Show the key steps when a calculation is",
  "involved and finish with the final numeric answer. Prefer Indian conventions",
  "(rupees, lakh/crore, metric, IST) unless the user asks otherwise.",
].join(" ");

functions.http("askAi", async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "content-type");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  if (req.method === "OPTIONS") return res.status(204).send("");

  try {
    const question = typeof req.body?.question === "string" ? req.body.question.trim() : "";
    const context = typeof req.body?.context === "string" ? req.body.context.trim() : "";
    if (!question) return res.status(400).json({ error: "Ask a question first." });

    const key = process.env.GEMINI_API_KEY;
    if (!key) return res.status(500).json({ error: "Server is not configured." });
    const model = process.env.GEMINI_MODEL || "gemini-3.5-flash";

    const prompt = context
      ? `${SYSTEM}\n\nApp context: ${context}\n\nUser question: ${question}`
      : `${SYSTEM}\n\nUser question: ${question}`;

    const r = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
      },
    );
    const data = await r.json().catch(() => null);
    if (!r.ok) {
      return res.status(502).json({ error: data?.error?.message ?? `AI request failed (${r.status})` });
    }
    const answer =
      data?.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";
    if (!answer) return res.status(502).json({ error: "The model returned no answer." });
    return res.json({ answer, model });
  } catch (e) {
    return res.status(500).json({ error: String(e) });
  }
});
