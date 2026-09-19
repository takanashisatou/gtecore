"""Validate the actual T1 Java pattern; optionally render a schematic with --preview.

Run from any directory: python modules/gtecore/scripts/check-water-structure.py --preview
The preview uses symbolic colors, not Minecraft textures. Pillow is only needed for --preview.
"""

import argparse
from collections import Counter
from pathlib import Path
import re


MODULE = Path(__file__).resolve().parents[1]
SOURCE = MODULE / "src/main/java/org/satou/gtecore/common/data/machines/GTEWaterPurificationMachines.java"


def read_pattern():
    text = SOURCE.read_text(encoding="utf-8")
    start = text.index("public static final MultiblockMachineDefinition T1_CLARIFIER_PURIFICATION_UNIT")
    text = text[start:text.index(".build())", start)]
    slices = [re.findall(r'"([^"\n]*)"', call) for call in re.findall(r"\.aisle\((.*?)\)", text, re.S)]
    assert slices and slices[0] and slices[0][0], "T1 pattern missing"
    width, height, depth = len(slices[0][0]), len(slices[0]), len(slices)
    assert all(len(layer) == height for layer in slices), "Inconsistent aisle heights"
    assert all(len(row) == width for layer in slices for row in layer), "Inconsistent row widths"
    assert (width, height, depth) == (9, 8, 7), "Unexpected structure dimensions"
    cells = {(x, y, z): char for z, layer in enumerate(slices)
             for y, row in enumerate(layer) for x, char in enumerate(row)}
    counts = Counter(cells.values())
    mapped = re.findall(r'\.where\("(.)"', text)
    assert len(mapped) == len(set(mapped)), "Duplicate predicate mapping"
    assert set(counts) == set(mapped), "Unmapped or unused pattern symbol"
    for char in "#MTS":
        assert counts[char] == 1, f"Expected exactly one {char}"
    controls = {char: next(pos for pos, symbol in cells.items() if symbol == char) for char in "#MTS"}
    assert all(pos[2] == depth - 1 for pos in controls.values()), "Service parts must be on the front boundary"
    assert {pos[1] for pos in controls.values()} == {1}, "Service parts must share the accessible second layer"
    assert '.where("M", blocks(GTMachines.MAINTENANCE_HATCH.getBlock()).setExactLimit(1))' in text
    assert '.where("T", abilities(GTEWaterPurificationParts.THERMAL_CONTROL).setExactLimit(1))' in text
    assert '.where("S", abilities(GTEWaterPurificationParts.THERMAL_SIGNAL).setExactLimit(1))' in text
    assert "unitAbilities()" not in text, "T1 must not inherit an additional maintenance predicate"
    assert not re.search(r"autoAbilities\(\s*true\s*,", text), "Additional automatic maintenance predicate"
    assert not re.search(r"abilities\(PartAbility\.MAINTENANCE", text), "Allows nonstandard maintenance"
    assert re.search(r'\.where\("\."\s*,\s*Predicates.any\(\)\)', text), "External wiring space must be unrestricted"
    print(f"PASS: {width} x {height} x {depth}; symbols {dict(counts)}")
    print(f"Front service coordinates (x, y, z): {controls}")
    print("Ordinary maintenance hatch only; no second auto-maintenance requirement.")
    return cells, controls


def render_preview(cells, controls):
    from PIL import Image, ImageDraw, ImageFont

    out = MODULE / "build/water-purification/t1-structure.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (1600, 1100), "#101d2d")
    draw = ImageDraw.Draw(image)
    font_path = Path("C:/Windows/Fonts/segoeui.ttf")
    def font(size):
        return ImageFont.truetype(str(font_path), size) if font_path.exists() else ImageFont.load_default(size=size)
    title, heading, body, small = font(38), font(24), font(20), font(17)
    draw.text((55, 36), "T1 / THERMAL WATER PURIFICATION", font=title, fill="#e9f4ff")
    draw.text((58, 94), "Twin towers  /  heat recovery loop  /  accessible service deck", font=body, fill="#9cb7cc")
    draw.rounded_rectangle((1210, 39, 1538, 108), radius=14, fill="#243b51")
    draw.text((1240, 57), "9 W  x  8 H  x  7 D", font=heading, fill="#d6efff")
    palette = {"A": (75, 143, 164), "F": (163, 182, 193), "G": (132, 215, 230),
               "P": (197, 143, 84), "#": (99, 190, 255), "M": (230, 187, 82), "T": (246, 108, 91), "S": (97, 225, 151)}
    solid = {pos: char for pos, char in cells.items() if char != "."}
    def project(x, y, z):
        return (695 + (x - z) * 43, 540 + (x + z) * 22 - y * 51)
    def shade(rgb, factor):
        return tuple(int(channel * factor) for channel in rgb)
    # Render back to front; show only exposed positive-axis faces.
    for (x, y, z), char in sorted(solid.items(), key=lambda entry: sum(entry[0])):
        rgb = palette[char]
        faces = [((0, 1, 0), [(x,y+1,z),(x+1,y+1,z),(x+1,y+1,z+1),(x,y+1,z+1)], 1.0),
                 ((1, 0, 0), [(x+1,y,z),(x+1,y,z+1),(x+1,y+1,z+1),(x+1,y+1,z)], .70),
                 ((0, 0, 1), [(x,y,z+1),(x+1,y,z+1),(x+1,y+1,z+1),(x,y+1,z+1)], .85)]
        for (dx, dy, dz), corners, lighting in faces:
            if (x+dx,y+dy,z+dz) in solid:
                continue
            polygon = [project(*corner) for corner in corners]
            draw.polygon(polygon, fill=shade(rgb, lighting), outline="#1d3543", width=2)
            if char == "G":
                center = (sum(p[0] for p in polygon)/4, sum(p[1] for p in polygon)/4)
                inner = [(center[0]+(px-center[0])*.7, center[1]+(py-center[1])*.7) for px,py in polygon]
                draw.line(inner + inner[:1], fill="#c6f5fa", width=2)
            if char == "F":
                draw.line((polygon[0], polygon[2]), fill="#506b7d", width=3)
                draw.line((polygon[1], polygon[3]), fill="#506b7d", width=3)
            if char in "#MTS" and dz == 1:
                center = project(x+.5,y+.5,z+1)
                draw.text(center, "C" if char == "#" else char, font=body, fill="#071729", anchor="mm")

    labels = [("M", "M  MANUAL MAINTENANCE", (70, 825)),
              ("#", "C  CONTROLLER", (575, 917)),
              ("T", "T  THERMAL INPUT", (1120, 825)),
              ("S", "S  HEATING REQUEST", (1120, 620))]
    for char, label, (lx, ly) in labels:
        x,y,z = controls[char]
        px,py = project(x+.5,y+.5,z+1)
        elbow = (lx+30, ly-20)
        draw.line([(px,py), elbow, (lx+300,ly-20)], fill=palette[char], width=3)
        draw.ellipse((px-4,py-4,px+4,py+4), fill=palette[char])
        draw.text((lx,ly), label, font=body, fill=palette[char])
    draw.text((70, 860), "Standard hatch required", font=small, fill="#a9bdcd")
    draw.text((1120, 860), "Redstone ON heats / OFF cools", font=small, fill="#a9bdcd")
    draw.line((55,985,1545,985), fill="#344a5d", width=2)
    legend = [("A","Watertight casing"),("F","Steel frame"),("G","Observation glass"),("P","Exchange piping")]
    for index,(char,label) in enumerate(legend):
        x=60+index*355
        draw.rounded_rectangle((x,1005,x+23,1028), radius=4, fill=palette[char])
        draw.text((x+33,1003),label,font=body,fill="#cedde8")
    draw.text((60,1050), "SCHEMATIC COLORS  |  Geometry parsed from the Java pattern  |  Existing in-game blocks; no new textures", font=small, fill="#91a8bb")
    image.save(out)
    print(f"Preview: {out}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", action="store_true", help="also render a labeled isometric PNG (requires Pillow)")
    args = parser.parse_args()
    pattern, service_parts = read_pattern()
    if args.preview:
        render_preview(pattern, service_parts)
