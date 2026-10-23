# AI backend — Cloudflare Worker (Workers AI)

The assistant runs on **Cloudflare Workers AI**, so there is **no API key** to
manage: the model is billed to your Cloudflare account. Nothing sensitive lives
in the app or in this repository.

## Deploy

```bash
cd backend/cloudflare
npm install -g wrangler
wrangler login
wrangler deploy
```

`wrangler deploy` prints the URL, e.g.
`https://nexus-calc-ai.<your-subdomain>.workers.dev`. That URL is what the app
calls (see `ai/AiClient.kt`, the `ENDPOINT` constant).

The `[ai]` binding in `wrangler.toml` is what gives the Worker access to
Workers AI.

## Changing the model

Edit `AI_MODEL` in `wrangler.toml` and redeploy. Any Workers AI text model
works, for example:

- `@cf/meta/llama-3.1-8b-instruct` (default — fast, cheap)
- `@cf/meta/llama-3.3-70b-instruct-fp8-fast` (stronger, slower)
- `@cf/google/gemma-3-12b-it`
- `@cf/qwen/qwen1.5-14b-chat-awq`

## Why a Worker and not the app calling a model directly

An API key inside an APK can be extracted by anyone who downloads it, and this
repository is public. With Workers AI there is no key to leak at all, and the
Worker is free-tier friendly.
