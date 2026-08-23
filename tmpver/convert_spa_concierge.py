import os
import sys
from PIL import Image
sys.stdout.reconfigure(encoding='utf-8')

out_dir = 'app/src/main/assets/posters/facilities'
os.makedirs(out_dir, exist_ok=True)

mapping = {
    'Infinite Bliss Spa - Elemis at Sea.jfif': 'infinite_bliss_spa.jpg',
    'Opulence Spa - Elemis at Sea.jfif': 'opulence_spa.jpg',
    '礼宾休息室.jfif': 'concierge_lounge.jpg',
    'Concierge Sundeck.jfif': 'concierge_sundeck.jpg'
}

for src_name, dst_name in mapping.items():
    src_p = os.path.join('地点库', src_name)
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    target_w = 1280 if im.size[0] >= 1280 else im.size[0]
    im.resize((target_w, int(target_w * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=90, optimize=True)
    print(f"{src_name} -> {dst_name}: {os.path.getsize(dst_p)} bytes, {Image.open(dst_p).size}")

print('Done generating spa and concierge posters!')
