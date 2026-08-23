import subprocess
import os

urls = [
    "https://disneycruise.disney.go.com/dcl-cruise-101-webapi/cruise-101/i18n/adventure/entertainment.json",
    "https://disneycruise.disney.go.com/dcl-cruise-101-webapi/cruise-101/i18n/ships/adventure/entertainment.json",
    "https://disneycruise.disney.go.com/dcl-cruise-101-spa/assets/i18n/en_US.json",
    "https://disneycruise.disney.go.com/dcl-cruise-101-spa/assets/i18n/en-us.json",
    "https://disneycruise.disney.go.com/dcl-cruise-101-webapi/cruise-101/i18n/en_US.json",
    "https://disneycruise.disney.go.com/dcl-cruise-101-webapi/cruise-101/i18n/en-us.json"
]

for idx, url in enumerate(urls):
    cmd = f'curl.exe -s -L -A "Mozilla/5.0" "{url}" -o tmpver/json_{idx}.json'
    subprocess.run(cmd, shell=True)
    size = os.path.getsize(f"tmpver/json_{idx}.json") if os.path.exists(f"tmpver/json_{idx}.json") else 0
    print(f"URL: {url} -> Size: {size}")
