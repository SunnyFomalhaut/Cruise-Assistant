import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

venues = doc.get('venues', [])
shops = [v for v in venues if v.get('category') == 'shop']

print(f"Total shops: {len(shops)}")
for idx, s in enumerate(shops, 1):
    name = s.get('name', '')
    deck = s.get('deck', '')
    zone = s.get('zone', '')
    slots = s.get('slots', [])
    hours = " / ".join(f"{sl.get('start')}-{sl.get('end')}" for sl in slots) if slots else "未配置"
    zone_dict = {'bow': '船头', 'mid': '船中', 'stern': '船尾'}
    zone_str = zone_dict.get(zone, zone)
    print(f"{idx}. **{name}**")
    print(f"   - **位置**：Deck {deck} {zone_str}")
    print(f"   - **营业时间**：{hours}")
