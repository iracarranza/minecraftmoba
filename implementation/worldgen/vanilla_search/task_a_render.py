"""Task A SVG presentation; no Minecraft/world-writing functions."""
import base64
from html import escape


def svg(fit, candidate, background):
    w=candidate['feature_grid']['width'];h=candidate['feature_grid']['height']
    scale=5;ox=65;oy=90;right=ox+w*scale+35;height=max(850,h*scale+180)
    parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{right+310}" height="{height}" viewBox="0 0 {right+310} {height}">',
           '<rect width="100%" height="100%" fill="#14211d"/>',
           '<g font-family="sans-serif" fill="#eef2e9">',
           f'<text x="30" y="30" font-size="22">Default Task A · {fit["seed"]}</text>',
           '<text x="30" y="54" font-size="13">Analytical spatial skeleton · no constructed content · effective depth measured from homeland edge</text>',
           f'<image x="{ox}" y="{oy}" width="{w*scale}" height="{h*scale}" href="data:image/png;base64,{base64.b64encode(background).decode()}"/>',
           f'<rect x="{ox}" y="{oy}" width="{w*scale}" height="{h*scale}" stroke="white" stroke-width="2" fill="none"/>']
    def point(sample): return ox+(sample[0]+.5)*scale,oy+(sample[1]+.5)*scale
    def label(sample,text,color='#fff',dy=-8):
        x,y=point(sample);parts.append(f'<text x="{x}" y="{y+dy}" fill="{color}" font-size="11" stroke="#14211d" stroke-width="3" paint-order="stroke">{escape(text)}</text>')
    def line(path,color,width,dash=''):
        points=' '.join(f'{x},{y}' for x,y in map(point,path))
        parts.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linejoin="round" stroke-linecap="round" stroke-dasharray="{dash}"/>')
    for name,p in (('N',[w/2,-3]),('S',[w/2,h+4]),('W',[-7,h/2]),('E',[w+3,h/2])):label(p,name)
    for connection in fit.get('cross_connections',[]): line(connection['sample_path'],'#b991ca',1,'4 5')
    for route in fit['routes']:
        color='#55bcff' if route['team']=='north' else '#ff856c'
        if 'sample_path' not in route: continue
        line(route['sample_path'],color,2.5,'6 4')
        early=route['starter'];line(early['supported_sample_path'],color,6)
        sx,sy=point(early['terminus']['sample'])
        parts.append(f'<circle cx="{sx}" cy="{sy}" r="5" fill="#fff" stroke="{color}" stroke-width="2"/>')
        label(early['terminus']['sample'],route['id'].replace('north','N').replace('south','S'),color)
    for team,home in fit['homelands'].items():
        color='#55bcff' if team=='north' else '#ff856c';x0,x1,z0,z1=home['logical_footprint_samples']
        parts.append(f'<rect x="{ox+x0*scale}" y="{oy+z0*scale}" width="{(x1-x0+1)*scale}" height="{(z1-z0+1)*scale}" fill="{color}" fill-opacity=".16" stroke="{color}" stroke-width="3"/>')
        label([(x0+x1)/2,z0],team.upper()+' HOME',color)
        if home['fountain']:
            x,y=point(home['fountain']['sample'])
            parts.append(f'<path d="M{x},{y-6}L{x+6},{y}L{x},{y+6}L{x-6},{y}Z" fill="white" stroke="{color}" stroke-width="2"><title>Aether Fountain anchor</title></path>')
    center=fit.get('center')
    if center and center['reference']:
        x,y=point(center['reference']['sample']);radius=center['reference_radius_blocks']/candidate['feature_grid']['sample_spacing_blocks']*scale
        parts.append(f'<circle cx="{x}" cy="{y}" r="{radius}" stroke="#ffec94" stroke-dasharray="6 5" fill="none"/>')
        label(center['reference']['sample'],'INTERIOR','#ffec94')
    for item in fit.get('intersections',[]):
        x,y=point(item['logical_sample']);parts.append(f'<circle cx="{x}" cy="{y}" r="3" fill="#fff1a8"><title>Sampled Route intersection / overlap</title></circle>')
    for index,item in enumerate(fit.get('key_locations',[]),1):
        x,y=point(item['sample']);parts.append(f'<rect x="{x-2}" y="{y-2}" width="4" height="4" fill="#ffe178"><title>K{index}: {escape(item["kind"])}</title></rect>')
        if index<=6: label(item['sample'],f'K{index}','#ffe178',12)
    major=sorted(fit.get('landmarks',[]),key=lambda r:(-r['area_blocks2'],r['id']))[:5]
    for index,item in enumerate(major,1): label(item['sample'],f'L{index}','#e3eacb',14)
    def side(y,text,size=12):parts.append(f'<text x="{right}" y="{y}" font-size="{size}">{escape(text)}</text>')
    side(110,'PROPOSED MAP FIT',16)
    for y,text in zip(range(140,261,20),['Blue / coral: North / South Routes','Thick: supported Starter portion','Dashed: persistent natural corridor','White circles: Wilderness handoffs','Diamonds: Aether Fountains','Violet: cross-connections','Yellow: intersections / Key Locations']):side(y,text)
    side(300,'EFFECTIVE DEPTH · LAND SAMPLES',13)
    for offset,team in enumerate(('north','south')):
        y=325+offset*90;side(y,team.upper(),13)
        access=fit.get('regional_depth',{}).get(team,{}).get('core_access',{})
        for row,(name,band) in enumerate((('secondary','70–110'),('tertiary','150–210'),('deep','250–350+'))):
            d=access.get(name,{});side(y+20+row*18,f'{band}: {d.get("land_samples",0)} samples · {"present" if d.get("meaningful_area_available") else "weak / absent"}')
    side(525,'LANDMARK REFERENCES',13)
    for index,item in enumerate(major,1):side(530+index*20,f'L{index} · {item["kind"]}')
    side(675,f'Failure diagnostics: {len(fit["failures"])}')
    side(698,'Detailed evidence and limits: companion JSON')
    side(722,'No winning seed or gameplay assignments')
    parts.extend([f'<text x="30" y="{height-45}" font-size="12">Bounds: {fit["playable_bounds"]} · rotation {fit["logical_orientation"]["rotation_degrees_clockwise"]}° · reflected {fit["logical_orientation"]["east_west_reflected"]}</text>',
                  f'<text x="30" y="{height-23}" font-size="12">Corridor spines and surface joins are sampled analytical references; block walkability and physical construction remain unverified.</text>', '</g></svg>'])
    return '\n'.join(parts)+'\n'
