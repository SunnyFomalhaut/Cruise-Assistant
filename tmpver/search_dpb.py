import subprocess
import json
import re

url = "https://disneyparksblog.com/?s=Disney+Adventure+entertainment"
cmd = f'curl.exe -s -L -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" "{url}" -o tmpver/dpb_search.html'
subprocess.run(cmd, shell=True)

with open('tmpver/dpb_search.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

print("DPB search html length:", len(html))
# Find article links
links = set(re.findall(r'https://disneyparksblog\.com/[^\s"\'`<>]+disney-adventure[^\s"\'`<>]*', html))
print(f"Found {len(links)} article links:")
for l in sorted(links):
    print("-", l)
