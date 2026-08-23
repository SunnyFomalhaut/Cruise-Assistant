import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Find occurrences of contentLocale or ContentService
matches = [m.start() for m in re.finditer(r'contentLocale', js)]
print(f"Found {len(matches)} matches for contentLocale")
for idx in matches:
    start = max(0, idx - 150)
    end = min(len(js), idx + 400)
    print("--- Snippet ---")
    print(js[start:end])
