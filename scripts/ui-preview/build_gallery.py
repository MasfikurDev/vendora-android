#!/usr/bin/env python3
"""Create a lightweight gallery page for the published screenshot branch."""
import argparse
import html
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--screenshots", required=True)
parser.add_argument("--output", required=True)
parser.add_argument("--run-url", required=True)
parser.add_argument("--commit", required=True)
args = parser.parse_args()
screens = Path(args.screenshots)
cards = [
    ("home.png", "Home / Overview"),
    ("products.png", "Products / Inventory"),
    ("selling.png", "Sell / Checkout"),
    ("insights.png", "Insights"),
]
for filename, _ in cards:
    if not (screens / filename).is_file():
        raise SystemExit(f"Required screenshot missing: {filename}")
body = "\n".join(
    f'<article><h2>{html.escape(title)}</h2><a href="screenshots/{filename}"><img src="screenshots/{filename}" alt="{html.escape(title)} screenshot"></a></article>'
    for filename, title in cards
)
page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vendora UI Preview</title>
<style>
:root{{color-scheme:light dark;font-family:system-ui,sans-serif}}body{{max-width:1100px;margin:auto;padding:24px}}header{{margin-bottom:24px}}.grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px}}article{{border:1px solid #8885;border-radius:16px;padding:12px}}img{{display:block;width:100%;height:auto;border-radius:10px}}a{{color:inherit}}small{{overflow-wrap:anywhere}}
</style>
</head>
<body>
<header><h1>Vendora Android UI Preview</h1><p>Automatically captured from the successful main-branch APK build using disposable demo data. These images are illustrative, not live business information.</p><p><a href="{html.escape(args.run_url, quote=True)}">Source build run</a> · <small>Commit {html.escape(args.commit)}</small></p></header>
<main class="grid">{body}</main>
</body></html>
"""
Path(args.output).parent.mkdir(parents=True, exist_ok=True)
Path(args.output).write_text(page, encoding="utf-8")
