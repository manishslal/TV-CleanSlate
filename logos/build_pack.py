#!/usr/bin/env python3
"""Build your private logo pack in logos/dark/ from the art you are allowed to use.

Each brand in manifest.json may name a source:
  "simple": "<slug>"   an SVG from Simple Icons (https://simpleicons.org), rendered white
  "trace": "<file>"    a PNG or JPG traced to clean SVG with VTracer, then rendered
  "commons": "<file>"  an SVG from Wikimedia Commons, flattened to white
Brands without a source keep the file already in dark/, and apps with no entry use their own icon.

Needs resvg (brew install resvg). Tracing also needs vtracer (pip install vtracer).
    npm pack simple-icons && tar xzf simple-icons-*.tgz
    python3 logos/build_pack.py --simple-icons package

Logos are trademarks. The art in dark/ is ignored by git. Simple Icons' license does not cover them.
"""
import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).parent
DARK = HERE / 'dark'
SIDE = 1024


def trim(path):
    """Cut a logo to its visible pixels, so tiles size the logo and not its margins."""
    image = Image.open(path).convert('RGBA')
    box = image.getchannel('A').point(lambda a: 255 if a > 24 else 0).getbbox()
    image.crop(box).save(path)


def flatten(path):
    """Keep a logo's shape and turn it white, so a colored SVG joins the pack."""
    image = Image.open(path).convert('RGBA')
    Image.merge('RGBA', (*[Image.new('L', image.size, 255)] * 3, image.getchannel('A'))).save(path)
    trim(path)


def render(svg, out):
    with tempfile.NamedTemporaryFile('w', suffix='.svg') as white:
        white.write(re.sub(r'<svg ', '<svg fill="#fff" ', svg.read_text(), count=1))
        white.flush()
        subprocess.run(['resvg', '--width', str(SIDE), white.name, str(out)], check=True)
    trim(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--simple-icons', type=Path, help='unpacked simple-icons package')
    args = parser.parse_args()
    manifest_path = HERE / 'manifest.json'
    manifest = json.loads(manifest_path.read_text())
    DARK.mkdir(exist_ok=True)
    data = {}
    if args.simple_icons:
        icons = json.loads((args.simple_icons / 'data/simple-icons.json').read_text())
        data = {i.get('slug', ''): i for i in icons}
    for brand, entry in manifest.items():
        out = DARK / f'{brand}.png'
        if entry.get('simple') and args.simple_icons:
            svg = args.simple_icons / 'icons' / f"{entry['simple']}.svg"
            render(svg, out)
            entry['file'] = f'dark/{brand}.png'
            if 'plate' not in entry and entry['simple'] in data:
                entry['plate'] = '#' + data[entry['simple']]['hex']
        plate = entry.get('plate')
        # Only generated one-color marks. Art in its own colors must keep them.
        if plate and 'ink' not in entry and (entry.get('simple') or entry.get('commons')):
            r, g, b = (int(plate[i:i + 2], 16) for i in (1, 3, 5))
            entry['ink'] = '#000000' if 0.2126 * r + 0.7152 * g + 0.0722 * b > 165 else '#FFFFFF'
        elif entry.get('commons'):
            svg = out.with_suffix('.svg')
            subprocess.run(['curl', '-fsSL', '-A', 'TV-CleanSlate-logo-pack/1.0 (personal use)', '-o', str(svg),
                            'https://commons.wikimedia.org/wiki/Special:FilePath/' + entry['commons']], check=True)
            subprocess.run(['resvg', '--width', str(SIDE), str(svg), str(out)], check=True)
            svg.unlink()
            flatten(out)
            entry['file'] = f'dark/{brand}.png'
        elif entry.get('trace'):
            import vtracer
            svg = out.with_suffix('.svg')
            vtracer.convert_image_to_svg_py(str(HERE / entry['trace']), str(svg), colormode='binary')
            render(svg, out)
            svg.unlink()
            entry['file'] = f'dark/{brand}.png'
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')


if __name__ == '__main__':
    main()
