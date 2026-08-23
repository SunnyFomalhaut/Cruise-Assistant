import subprocess
import re
import json
import urllib.request

urls = [
    "https://disneyparksblog.com/dcl/disney-cruise-line-adventure-updates/",
    "https://disneyparksblog.com/dcl/must-do-experiences-disney-adventure/"
]

all_images = []

for i, u in enumerate(urls):
    cmd = f'curl.exe -s -L -A "Mozilla/5.0" "{u}" -o tmpver/article_{i}.html'
    subprocess.run(cmd, shell=True)
    with open(f'tmpver/article_{i}.html', 'r', encoding='utf-8', errors='ignore') as f:
        html = f.read()
    
    # Extract all img tags with src, data-src, alt, etc.
    imgs = re.findall(r'<img[^>]+src=["\']([^"\']+)["\'][^>]*>', html)
    print(f"Article {i} has {len(imgs)} images")
    for img in imgs:
        if 'parksmedia' in img or 'wp-content' in img or 'wdprapps' in img:
            all_images.append((u, img))

print(f"\nTotal candidate images: {len(all_images)}")
with open('tmpver/article_imgs.json', 'w', encoding='utf-8') as out:
    json.dump(all_images, out, indent=2)
