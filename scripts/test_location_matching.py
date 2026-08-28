import json
import os
import re

with open('app/src/main/assets/locations.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

svg_texts = {}
for deck_num in range(1, 21):
    svg_path = f'app/src/main/assets/decks/deck_{deck_num}.svg'
    if os.path.exists(svg_path):
        with open(svg_path, 'r', encoding='utf-8') as sf:
            svg_texts[deck_num] = sf.read()

def normalize_text(s):
    if not s:
        return ""
    s = s.lower()
    for q in ["'", "’", "‘", "“", "”", "`"]:
        s = s.replace(q, "")
    s = re.sub(r"[\s·,/'\"()\-_]+", " ", s)
    return s.strip()

ZH_EXPANSIONS = {
    "航海家": ["navigator"],
    "皮克斯": ["pixar", "market"],
    "怪兽": ["flavors", "asia", "mike", "sulley"],
    "亚洲风味": ["flavors", "asia"],
    "帕洛": ["palo", "trattoria"],
    "动画师": ["animator", "palate", "table"],
    "盛宴": ["enchanted", "summer"],
    "聚光灯": ["hollywood", "spotlight"],
    "花园舞台": ["garden", "stage"],
    "迪士尼剧院": ["walt", "disney", "theatre", "theater"],
    "剧院": ["theatre", "theater", "cinemas", "cinema"],
    "寻路者": ["wayfinder"],
    "大白": ["baymax"],
    "泳池": ["pool"],
    "露天泳池": ["pool", "sunnyside"],
    "阳光泳池": ["sunnyside", "pool"],
}

unmatched = []
for v in data['venues']:
    name = v['name']
    deck = v.get('deck')
    if not deck or deck not in svg_texts:
        continue
    
    clean_svg = normalize_text(svg_texts[deck])
    clean_name = normalize_text(name)
    words = [w for w in clean_name.split(' ') if len(w) > 1 and w not in ['deck', 'the', 'and', 'restaurant', 'club']]
    for zh_key, en_list in ZH_EXPANSIONS.items():
        if zh_key in name:
            words.extend(en_list)
            
    matched = any(w in clean_svg for w in words)
    if not matched:
        unmatched.append((deck, name, words))

print(f"Total venues checked: {len(data['venues'])}, Unmatched: {len(unmatched)}")
for d, n, w in unmatched:
    print(f"  Deck {d}: {n} (words={w})")
