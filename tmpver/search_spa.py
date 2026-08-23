import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/entertainment.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

keywords = ['Remember', 'Moana', 'Avengers', 'Baymax', 'Theatre', 'Stage', 'Deck', 'Show', 'Musical', 'Character', 'Captain', 'Club', 'Cinema']

for kw in keywords:
    matches = [m.start() for m in re.finditer(re.escape(kw), html, re.I)]
    print(f"Keyword '{kw}': {len(matches)} occurrences")
    if matches:
        first = matches[0]
        snippet = html[max(0, first - 100):min(len(html), first + 200)]
        print("  Snippet:", re.sub(r'\s+', ' ', snippet))
