import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Look for chunks
chunks = re.findall(r'[0-9a-zA-Z_-]+\.[0-9a-f]+\.js', js)
print("Chunks referenced in main.js:", len(chunks))
for c in set(chunks):
    print(" ", c)
