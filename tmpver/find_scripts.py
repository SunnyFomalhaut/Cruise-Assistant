import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/entertainment.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

scripts = re.findall(r'<script[^>]*src=["\'](.*?)["\']', html)
print("Scripts found:")
for s in scripts:
    print("-", s)
