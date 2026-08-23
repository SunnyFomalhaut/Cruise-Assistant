import re
import sys
sys.stdout.reconfigure(encoding='utf-8')

for i in [0, 1]:
    with open(f'tmpver/article_{i}.html', 'r', encoding='utf-8', errors='ignore') as f:
        html = f.read()
    
    # Extract images and captions / alts
    figures = re.findall(r'<figure[^>]*>(.*?)</figure>', html, re.DOTALL)
    print(f"Article {i} has {len(figures)} figure elements")
    for fig in figures:
        img_match = re.search(r'src=["\']([^"\']+)["\']', fig)
        alt_match = re.search(r'alt=["\']([^"\']+)["\']', fig)
        cap_match = re.search(r'<figcaption[^>]*>(.*?)</figcaption>', fig, re.DOTALL)
        
        src = img_match.group(1) if img_match else ""
        alt = alt_match.group(1) if alt_match else ""
        cap = re.sub(r'<[^>]+>', '', cap_match.group(1)).strip() if cap_match else ""
        
        if src and not src.endswith('.svg') and not 'avatar' in src:
            print(f"  [IMG] {src}")
            print(f"    Alt: {alt}")
            print(f"    Caption: {cap}")
