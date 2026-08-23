import xml.etree.ElementTree as ET

tree = ET.parse('tmpver/dump_sett.xml')
def print_parents(node, path):
    if node.attrib.get('content-desc') == '设置':
        print("Path:")
        for p in path:
            print(p.attrib.get('class'), p.attrib.get('bounds'), "clickable:", p.attrib.get('clickable'))
        print(node.attrib)
    for child in node:
        print_parents(child, path + [node])

print_parents(tree.getroot(), [])
