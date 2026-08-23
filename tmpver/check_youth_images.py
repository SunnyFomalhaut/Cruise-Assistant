import os
import sys
from PIL import Image
sys.stdout.reconfigure(encoding='utf-8')

for f in sorted(os.listdir('地点库')):
    fp = os.path.join('地点库', f)
    if os.path.isfile(fp):
        im = Image.open(fp)
        print(f"{f}: format={im.format}, size={im.size}, mode={im.mode}")
