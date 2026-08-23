import re
import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/entertainment.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

# Look for embedded JSON state or structured data
json_blocks = re.findall(r'<script[^>]*type=["\'](?:application/json|application/ld\+json)["\'][^>]*>(.*?)</script>', html, re.DOTALL)
print(f"Found {len(json_blocks)} json script blocks")

for i, block in enumerate(json_blocks):
    try:
        data = json.loads(block)
        print(f"Block {i} is valid JSON keys: {list(data.keys()) if isinstance(data, dict) else type(data)}")
        with open(f'tmpver/json_block_{i}.json', 'w', encoding='utf-8') as out:
            json.dump(data, out, ensure_ascii=False, indent=2)
    except Exception as e:
        pass

# Extract headings and content using regex
h_matches = re.findall(r'<h[234][^>]*>(.*?)</h[234]>', html, re.DOTALL)
clean_headings = []
for h in h_matches:
    clean = re.sub(r'<[^>]+>', '', h).strip()
    if clean and clean not in clean_headings:
        clean_headings.append(clean)

print("\n--- Extracted Headings ---")
for h in clean_headings:
    print("-", h)
