import urllib.request
import json
import re

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
}

# Search Disney Parks Blog / DCL media CDN for Adventure entertainment image assets
search_terms = [
    "Disney-Adventure-Remember-WALL-E",
    "Disney-Adventure-Avengers-Assemble",
    "Disney-Adventure-Moana-Call-of-the-Sea",
    "Disney-Adventure-Seas-the-Adventure",
    "Disney-Adventure-Garden-Stage",
    "Disney-Adventure-Wayfinder-Bay"
]

print("Searching for known DCL media asset URLs...")
