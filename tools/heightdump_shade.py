#!/usr/bin/env python3
"""Hill-shaded pictures of an "rtf heightdump" (RTFHeightDump): <name>-raw.png and <name>-final.png next to the .npy,
and <name>-diff.png (final - raw: red where the erosion lowered the ground, blue where it raised it).

    python tools/heightdump_shade.py run/heightdump-neoforge/<name>.npy [...]

Light from the north west (azimuth 315, 45 degrees up), x to the right (east), z down (south) as on a map; water (height
0 or below, blocks above the water level) in blue by depth. Needs numpy and Pillow.
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image


def shade(height, spacing):
    # z grows downwards (south): north is up, the light from the upper left
    dz, dx = np.gradient(height.astype(np.float64), spacing)
    # light from the north west at 45 degrees: direction (-1, -1) in (x, z) on the map, then up
    lx, lz, ly = -np.sqrt(0.5) * np.cos(np.radians(45)), -np.sqrt(0.5) * np.cos(np.radians(45)), np.sin(np.radians(45))
    # the surface normal of y = h(x, z) is (-dh/dx, 1, -dh/dz)
    norm = np.sqrt(dx * dx + dz * dz + 1.0)
    light = (-dx * lx - dz * lz + ly) / norm
    return np.clip(light, 0.0, 1.0)


def picture(height, spacing):
    light = shade(height, spacing)
    land = height > 0.0
    # land: a light brown to white tint by height, times the light
    t = np.clip(height / max(1.0, float(height[land].max()) if land.any() else 1.0), 0.0, 1.0)
    base = np.stack([150 + 105 * t, 140 + 115 * t, 110 + 145 * t], axis=-1)
    rgb = base * (0.25 + 0.75 * light)[..., None]
    # water: blue by depth, a little shading of the floor
    depth = np.clip(-height / 60.0, 0.0, 1.0)
    water = np.stack([20 + 30 * (1 - depth), 60 + 80 * (1 - depth), 140 + 100 * (1 - depth)], axis=-1) * (0.8 + 0.2 * light)[..., None]
    rgb = np.where(land[..., None], rgb, water)
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")


def diff_picture(raw, final, scale=4.0):
    d = np.clip((final - raw) / scale, -1.0, 1.0)
    r = np.where(d < 0, 255, 255 * (1 - d))
    b = np.where(d > 0, 255, 255 * (1 + d))
    g = 255 * (1 - np.abs(d))
    return Image.fromarray(np.stack([r, g, b], axis=-1).astype(np.uint8), "RGB")


def main(paths):
    for p in map(Path, paths):
        data = np.load(p)
        meta = json.loads(p.with_suffix(".json").read_text(encoding="utf-8"))
        ch = {name: i for i, name in enumerate(meta["channels"])}
        step = meta["step"]
        for name in ("raw", "final"):
            out = p.with_name(p.stem + "-" + name + ".png")
            picture(data[ch[name]], step).save(out)
            print(out)
        out = p.with_name(p.stem + "-diff.png")
        diff_picture(data[ch["raw"]], data[ch["final"]]).save(out)
        print(out)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
