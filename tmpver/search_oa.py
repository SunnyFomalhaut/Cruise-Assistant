import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

for m in re.finditer(r'onboard-activities', js):
    start = max(0, m.start() - 100)
    end = min(len(js), m.end() + 300)
    print("=== onboad-activities context ===")
    print(js[start:end])
