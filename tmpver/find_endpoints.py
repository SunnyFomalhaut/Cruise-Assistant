import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Search for endpoint patterns like /api/, /graphql, /content/, dcl, prismic, contentful, cms
urls = set(re.findall(r'["\'](/[^"\']*adventure[^"\']*)["\']', js))
print("Adventure paths in JS:")
for u in sorted(urls):
    print(" ", u)

# Search for network calls / endpoints
endpoints = set(re.findall(r'["\'](https?://[^"\']+)["\']', js))
print(f"Total full URLs in JS: {len(endpoints)}")
for e in sorted(endpoints):
    if 'disney' in e or 'dcl' in e or 'wdpr' in e:
        print(" ", e)
