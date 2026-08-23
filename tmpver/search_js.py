import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

print("Length of main.js:", len(js))

# Look for API urls or endpoints
apis = set(re.findall(r'https?://[^\s"\'`<>]+api[^\s"\'`<>]+', js))
print(f"Found {len(apis)} API URLs:")
for a in sorted(apis)[:20]:
    print(" ", a)

# Look for 'adventure' and entertainment objects
matches = [m.start() for m in re.finditer(r'adventure[^\s"\'`<>]*entertainment', js, re.I)]
print(f"Found {len(matches)} matches for adventure.*entertainment")
for idx in matches[:5]:
    print("Snippet:", js[max(0, idx - 100):min(len(js), idx + 200)])
