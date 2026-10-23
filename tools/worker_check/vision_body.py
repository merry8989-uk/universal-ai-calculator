"""Prints a Workers AI vision request body for the given shape."""
import json, os, sys

shape = sys.argv[1] if len(sys.argv) > 1 else "url"
b64 = open(os.path.join(os.path.dirname(__file__), "photo.b64")).read().strip()
prompt = (
    "Read the arithmetic in this image. Reply with ONLY the expression using digits "
    "and + - * / ^ ( ) %. If there is an equals sign, give only the part before it. "
    "No words, no markdown."
)
url = "data:image/png;base64," + b64

if shape == "url":
    body = {"messages": [{"role": "user", "content": [
        {"type": "text", "text": prompt},
        {"type": "image_url", "image_url": {"url": url}},
    ]}]}
elif shape == "type-image":
    body = {"messages": [{"role": "user", "content": [
        {"type": "text", "text": prompt},
        {"type": "image", "image": url},
    ]}]}
else:
    body = {"image": url, "prompt": prompt, "max_tokens": 128}

print(json.dumps(body))
