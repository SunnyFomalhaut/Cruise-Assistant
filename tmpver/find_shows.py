import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/main.js', 'r', encoding='utf-8', errors='ignore') as f:
    js = f.read()

# Search for Disney Adventure specific entertainment venues/shows
shows = ["Remember", "Moana", "Avengers", "Big Hero 6", "Baymax", "Walt Disney Theatre", "Garden Stage", "Wayfinder", "Captain Marvel", "Spider-Man", "Pirate", "Fireworks", "Tangled", "Aladdin", "Frozen", "Duffy"]

for s in shows:
    matches = list(re.finditer(re.escape(s), js, re.I))
    print(f"Show '{s}': {len(matches)} matches")
    for m in matches[:3]:
        idx = m.start()
        print("  Snippet:", re.sub(r'\s+', ' ', js[max(0, idx - 80):min(len(js), idx + 180)]))
