import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Find where 'adventure/entertainment' is defined or used
for m in re.finditer(r'adventure/entertainment', js):
    start = max(0, m.start() - 200)
    end = min(len(js), m.end() + 1000)
    print("=== Context ===")
    print(js[start:end])
