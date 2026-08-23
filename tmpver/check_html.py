import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('tmpver/entertainment.html', 'r', encoding='utf-8', errors='ignore') as f:
    html = f.read()

print("Length:", len(html))
print("First 1000 chars:\n", html[:1000])
