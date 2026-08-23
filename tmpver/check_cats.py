import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

venues = doc.get('venues', [])
cats = {}
for v in venues:
    c = v.get('category')
    cats.setdefault(c, []).append(v)

for c, items in cats.items():
    print(f"Category '{c}': {len(items)} items")
    for it in items[:3]:
        print(f"   - {it.get('name')}")
