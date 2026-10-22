# AI backend — Cloudflare Worker

Keeps the Gemini key on the server so it is never inside the app.

## Deploy (about two minutes)

```bash
cd backend/cloudflare
npm install -g wrangler
wrangler login                       # opens the browser once
wrangler secret put GEMINI_API_KEY   # paste the key when prompted
wrangler deploy
```

`wrangler deploy` prints the URL, e.g.

```
https://nexus-calc-ai.<your-subdomain>.workers.dev
```

Put that URL into the app as the AI endpoint
(`app/src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt`,
the `ENDPOINT` constant) and rebuild. The Worker needs no publishable key of
its own, so the app sends no Authorization header.

## Optional

- Change the model with a variable: `wrangler secret put GEMINI_MODEL` (default
  `gemini-3.5-flash`).
- Lock it down to your app by checking an `Origin` or a shared header in
  `worker.js`.

## Why a Worker and not the app calling Gemini directly

A key inside an APK can be extracted by anyone who downloads it, and this
repository is public. A Worker keeps the key server-side and is free on the
Cloudflare free tier.
