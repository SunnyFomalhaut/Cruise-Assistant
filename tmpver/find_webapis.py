import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

matches = re.findall(r'["\'](/dcl-cruise-101-webapi/[^"\']*)["\']', js)
print("WebAPI paths found:")
for m in sorted(set(matches)):
    print("-", m)
