import os
import sys
from PIL import Image
sys.stdout.reconfigure(encoding='utf-8')

out_dir = 'app/src/main/assets/posters/dining'
os.makedirs(out_dir, exist_ok=True)

for f in sorted(os.listdir('餐饮图库')):
    src_p = os.path.join('餐饮图库', f)
    clean_name = f.replace('\u200b', '').split('.')[0]
    slug = clean_name.lower().replace('&', 'and').replace('\'', '').replace(' ', '_').replace('é', 'e')
    slug = '_'.join([part for part in slug.split('_') if part])
    dst_name = slug + '.jpg'
    dst_p = os.path.join(out_dir, dst_name)
    im = Image.open(src_p).convert('RGB')
    im.resize((1280, int(1280 * im.size[1] / im.size[0])), Image.Resampling.LANCZOS).save(dst_p, quality=88, optimize=True)
    print(f"{clean_name} -> {dst_name}: {os.path.getsize(dst_p)} bytes, {Image.open(dst_p).size}")

files = sorted(os.listdir(out_dir))
print("Count in dining assets:", len(files))
assert len(files) == 22
