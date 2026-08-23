import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/dpb_search.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

posts = re.findall(r'<a[^>]+href=["\'](https://disneyparksblog\.com/[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Total post links: {len(posts)}")
for href, title in posts:
    clean_title = re.sub(r'<[^>]+>', '', title).strip()
    if clean_title and len(clean_title) > 5 and 'adventure' in href.lower() or 'cruise' in href.lower() or 'disney' in clean_title.lower():
        print(f"Link: {href}\n  Title: {clean_title}")
