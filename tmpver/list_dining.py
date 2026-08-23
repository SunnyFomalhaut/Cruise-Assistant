import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

venues = doc.get('venues', [])

# Let's inspect all venues under category == 'meal' or relevant categories
dining_venues = [v for v in venues if v.get('category') == 'meal']

print(f"Total dining/bar/snack venues: {len(dining_venues)}")

# Group them logically (Complimentary Main Dining, Specialty Dining, Quick Service & Snacks, Bars & Lounges, etc.)
zone_dict = {'bow': '船头', 'mid': '船中', 'stern': '船尾'}

for idx, v in enumerate(dining_venues, 1):
    name = v.get('name', '')
    deck = v.get('deck', '')
    zone = zone_dict.get(v.get('zone', ''), v.get('zone', ''))
    multi_deck = v.get('multiDeck', '')
    deck_str = f"Deck {multi_deck}" if multi_deck else f"Deck {deck}"
    slots = v.get('slots', [])
    hours = " / ".join(f"{sl.get('start')}-{sl.get('end')}" for sl in slots) if slots else "未配置"
    meal_periods = v.get('mealPeriods', [])
    period_str = f"[{', '.join(meal_periods)}]" if meal_periods else ""
    late_night = " (夜宵)" if v.get('lateNight') else ""
    
    print(f"{idx}. {name} | {deck_str} {zone} | {hours}{late_night} {period_str}")
