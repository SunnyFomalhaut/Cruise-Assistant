import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

for v in doc.get('venues', []):
    name = v.get('name', '')
    if any(k in name.lower() for k in ['theatre', 'theater', '剧院', 'pool', '泳池', 'infinity', '无边']):
        print(f"{v.get('category')}: {name} | Deck {v.get('deck')} {v.get('zone')}")
