import os
import sys
from PIL import Image
sys.stdout.reconfigure(encoding='utf-8')

out_dir = 'app/src/main/assets/posters/youth'
os.makedirs(out_dir, exist_ok=True)

mapping = {
    'it’s a small world” nursery.avif': 'its_a_small_world_nursery.jpg',
    'Mickey & Minnie Captain’s Deck.jfif': 'mickey_minnie_captains_deck.jpg',
    "Disney's Oceaneer Club.webp": 'disneys_oceaneer_club.jpg',
    'Edge.jfif': 'edge.jpg',
    'Vibe.webp': 'vibe.jpg'
}

for src_name, dst_name in mapping.items():
    src_p = os.path.join('地点库', src_name)
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    im.resize((1000, int(1000 * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f"{src_name} -> {dst_name}: {os.path.getsize(dst_p)} bytes, {Image.open(dst_p).size}")

files = sorted(os.listdir(out_dir))
print("Count in youth assets:", len(files))
assert len(files) == 5
