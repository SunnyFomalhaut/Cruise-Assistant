import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Search for json asset URLs
json_urls = set(re.findall(r'["\']([^"\']+\.json[^"\']*)["\']', js))
print("JSON assets in JS:", len(json_urls))
for u in sorted(json_urls):
    print("-", u)
