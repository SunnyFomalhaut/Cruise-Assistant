import subprocess
import re
import json

queries = [
    "Disney+Adventure+Avengers+Assemble",
    "Disney+Adventure+Seas+the+Adventure",
    "Disney+Adventure+Let%27s+Set+Sail",
    "Disney+Adventure+Captain+Jack+Sparrow",
    "Disney+Adventure+Garden+Stage"
]

all_found = {}

for q in queries:
    url = f"https://disneyparksblog.com/?s={q}"
    cmd = f'curl.exe -s -L -A "Mozilla/5.0" "{url}" -o tmpver/search_q.html'
    subprocess.run(cmd, shell=True)
    with open('tmpver/search_q.html', 'r', encoding='utf-8', errors='ignore') as f:
        html = f.read()
    
    imgs = re.findall(r'src=["\'](https://disneyparksblog\.com/app/uploads/[^"\']+\.(?:jpg|jpeg|png))["\']', html)
    print(f"Query {q} found {len(imgs)} uploads")
    for img in imgs:
        all_found[img] = q

print(f"\nUnique upload images found: {len(all_found)}")
for img in sorted(all_found.keys()):
    print("-", img)
