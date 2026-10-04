"""Rasterize generated plan views into a compact scientific comparison figure."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

reports = Path(sys.argv[1])
packages = json.loads((reports / 'packages.json').read_text())
font_path = '/System/Library/Fonts/Supplemental/Arial.ttf'
def font(size):
    return ImageFont.truetype(font_path, size)

canvas = Image.new('RGB', (470 * len(packages) + 32, 680), '#142029')
draw = ImageDraw.Draw(canvas)
draw.text((24, 18), 'Minecraft MOBA · Test-map terrain', font=font(28), fill='white')
draw.text((24, 56), 'Prototype/test · eight-block surface samples · logical map orientation',
          font=font(15), fill='#bac7cd')
for index, package in enumerate(packages):
    left = 24 + index * 470
    draw.text((left, 102), f"Seed {package['seed']}", font=font(23), fill='white')
    draw.text((left, 134), ' / '.join(package['types']).replace('_', ' '),
              font=font(15), fill='#bac7cd')
    root = ET.parse(package['overview']).getroot()
    _, _, w, h = map(float, root.attrib['viewBox'].split())
    scale = min(430 / w, 420 / h)
    ox, oy = left, 174
    draw.rectangle((ox, oy, ox + w*scale, oy + h*scale), fill='#263844')
    for element in root:
        tag = element.tag.split('}')[-1]
        a = element.attrib
        if tag == 'rect':
            x, y = float(a['x']), float(a['y'])
            draw.rectangle((ox+x*scale, oy+y*scale,
                            ox+(x+1)*scale, oy+(y+1)*scale), fill=a['fill'])
    labels = {'#faf3a5': 'F', '#ed916d': 'O', '#d879ef': 'L'}
    for element in root:
        if element.tag.split('}')[-1] != 'circle':
            continue
        a = element.attrib
        x, y = ox+float(a['cx'])*scale, oy+float(a['cy'])*scale
        draw.ellipse((x-7, y-7, x+7, y+7), fill=a['fill'], outline='#111111', width=2)
        draw.text((x, y), labels[a['fill']], font=font(9), fill='#111111', anchor='mm')
    draw.text((left, 610), f"Lair access disparity: {package['lair_access_asymmetry']:.4f}",
              font=font(15), fill='white')
    draw.text((left, 633), f"Derived renewable sources: {package['renewable_sources']}",
              font=font(15), fill='#bac7cd')
draw.text((24, 658), 'F  Fountain     O  Defensive objective     L  Lair     Blue  Water     Dark green  Forest',
          font=font(14), fill='#bac7cd')
canvas.save(reports / 'test-map-comparison.png')
