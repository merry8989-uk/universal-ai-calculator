# Nexus Calc — AI backend

The app never holds the Gemini API key. It calls a tiny proxy that does.

Two ready-to-deploy versions are included — pick either:

| Folder | Host | Free tier |
| --- | --- | --- |
| `cloudflare/` | Cloudflare Worker | Yes, generous |
| `gcloud/` | Google Cloud Function (2nd gen) | Yes, within free limits |

Both read the key from an environment variable / secret named `GEMINI_API_KEY`.
**Never commit the key.** After deploying, put the printed URL into
`ai/AiClient.kt` (`ENDPOINT`) and rebuild.
