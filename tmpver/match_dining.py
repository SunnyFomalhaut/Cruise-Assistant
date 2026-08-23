import os
import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    doc = json.load(f)

venues = [v for v in doc.get('venues', []) if v.get('category') == 'meal']

files_in_gallery = sorted(os.listdir('餐饮图库'))
print(f"Total files in 餐饮图库: {len(files_in_gallery)}")
for f in files_in_gallery:
    print("  -", f)

print(f"\nTotal dining venues in locations.json: {len(venues)}")
for v in venues:
    name = v.get('name')
    # Find matching file
    matched = [f for f in files_in_gallery if f.split('.')[0].lower() in name.lower() or name.lower().split()[0] in f.lower()]
    print(f"Venue: {name} => Matched: {matched}")
