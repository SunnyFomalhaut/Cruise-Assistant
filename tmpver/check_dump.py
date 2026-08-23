import xml.etree.ElementTree as ET
import sys
sys.stdout.reconfigure(encoding='utf-8')

tree = ET.parse('tmpver/dump_main_v1110.xml')
for node in tree.iter('node'):
    t = node.attrib.get('text', '')
    d = node.attrib.get('content-desc', '')
    b = node.attrib.get('bounds', '')
    if t or d:
        print(f"text: {t} | desc: {d} | bounds: {b}")
