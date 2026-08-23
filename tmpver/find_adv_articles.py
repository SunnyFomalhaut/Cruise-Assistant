import subprocess
import re
import json

url = "https://disneyparksblog.com/?s=Disney+Adventure+shows+Remember+Avengers"
cmd = f'curl.exe -s -L -A "Mozilla/5.0" "{url}" -o tmpver/dpb_adventure.html'
subprocess.run(cmd, shell=True)

with open('tmpver/dpb_adventure.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

posts = re.findall(r'<a[^>]+href=["\'](https://disneyparksblog\.com/[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
for href, title in posts:
    clean_title = re.sub(r'<[^>]+>', '', title).strip()
    if 'adventure' in href.lower() or 'cruise' in href.lower():
        print(f"{clean_title} -> {href}")
