import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

venues = doc.get('venues', [])
chore_venues = [v for v in venues if v.get('category') == 'chore']

print(f"Total ship service venues: {len(chore_venues)}")
zone_dict = {'bow': '船头', 'mid': '船中', 'stern': '船尾'}

for idx, v in enumerate(chore_venues, 1):
    name = v.get('name', '')
    deck = v.get('deck', '')
    zone = zone_dict.get(v.get('zone', ''), v.get('zone', ''))
    slots = v.get('slots', [])
    hours = " / ".join(f"{sl.get('start')}-{sl.get('end')}" for sl in slots) if slots else "未配置"
    print(f"{idx}. **{name}**")
    print(f"   - **位置**：Deck {deck} {zone}")
    print(f"   - **开放时间**：{hours}")
