import subprocess
import os

os.makedirs('app/src/main/assets/posters/events', exist_ok=True)

downloads = [
    ("https://disneyparksblog.com/app/uploads/2024/10/Disney-Adventure_22Remember22-Stage-Show.jpg", "app/src/main/assets/posters/events/remember.jpg"),
    ("https://disneyparksblog.com/app/uploads/2026/01/2026-DCL-Adventure-Ships-Log-Fireworks-1.jpg", "app/src/main/assets/posters/events/lion_king_fireworks.jpg"),
]

for url, target in downloads:
    cmd = f'curl.exe -s -L -A "Mozilla/5.0" "{url}" -o "{target}"'
    subprocess.run(cmd, shell=True)
    size = os.path.getsize(target) if os.path.exists(target) else 0
    print(f"Downloaded: {target} (Size: {size} bytes)")
