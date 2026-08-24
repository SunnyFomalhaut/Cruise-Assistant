# scripts/optimize_doll_assets.py
import os
import shutil
from PIL import Image

SRC_DIR = "人偶素材"
DEST_DIR = "app/src/main/assets/dolls"

FOLDER_MAP = {
    "达菲": "duffy",
    "雪莉玫": "shelliemay",
    "星黛露": "stellalou",
    "玲娜贝儿": "linabell",
    "杰拉多尼": "gelatoni",
    "可琦安": "cookieann",
    "奥乐米拉": "olumel",
    "双人组合": "duo",
}

def process_all():
    if os.path.exists(DEST_DIR):
        shutil.rmtree(DEST_DIR)
    os.makedirs(DEST_DIR, exist_ok=True)

    total_orig = 0
    total_opt = 0
    count = 0

    for root, _, files in os.walk(SRC_DIR):
        rel = os.path.relpath(root, SRC_DIR)
        parts = rel.split(os.sep)
        char_zh = parts[0]
        if char_zh not in FOLDER_MAP:
            continue
        char_en = FOLDER_MAP[char_zh]
        subfolder = parts[1] if len(parts) > 1 else ""

        target_folder = os.path.join(DEST_DIR, char_en, subfolder) if subfolder else os.path.join(DEST_DIR, char_en)
        os.makedirs(target_folder, exist_ok=True)

        for f in sorted(files):
            if f.lower().endswith(('.png', '.webp', '.jpg')):
                src_path = os.path.join(root, f)
                total_orig += os.path.getsize(src_path)

                base_name = os.path.splitext(f)[0]
                dest_path = os.path.join(target_folder, f"{base_name}.webp")

                with Image.open(src_path) as img:
                    img = img.convert("RGBA")
                    bbox = img.getbbox()
                    if bbox:
                        img = img.crop(bbox)
                    img.thumbnail((512, 512), Image.Resampling.LANCZOS)
                    img.save(dest_path, format="WEBP", quality=90, method=4)

                total_opt += os.path.getsize(dest_path)
                count += 1

    print(f"Processed {count} images.")
    print(f"Original size: {total_orig / (1024*1024):.2f} MB")
    print(f"Optimized size: {total_opt / (1024*1024):.2f} MB ({(total_opt/total_orig)*100:.2f}%)")

if __name__ == "__main__":
    process_all()
