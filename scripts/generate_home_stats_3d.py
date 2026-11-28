#!/usr/bin/env python3
"""Generate the three independent premium home-stat GLB illustrations."""
from pathlib import Path
import numpy as np
import trimesh

OUT = Path("app/src/main/res/raw")
OUT.mkdir(parents=True, exist_ok=True)

def material(color, metallic=0.0, roughness=0.32, emissive=None):
    return trimesh.visual.material.PBRMaterial(
        baseColorFactor=color, metallicFactor=metallic,
        roughnessFactor=roughness,
        emissiveFactor=emissive or [0, 0, 0])

def add(scene, name, mesh, position=(0, 0, 0), rotation=None):
    mesh.visual.material = mesh.visual.material or material([220, 220, 220, 255])
    transform = np.eye(4)
    if rotation is not None:
        transform[:3, :3] = trimesh.transformations.euler_matrix(*rotation)[:3, :3]
    transform[:3, 3] = position
    scene.add_geometry(mesh, node_name=name, geom_name=name, transform=transform)

def box(extents, color, radius_detail=2, metallic=0.0, roughness=0.38):
    mesh = trimesh.creation.box(extents=extents)
    mesh.visual.material = material(color, metallic, roughness)
    return mesh

def curved_flame(height, width, bend, color, seed):
    rng = np.random.default_rng(seed)
    rings, sides = 18, 32
    vertices, faces = [], []
    for j in range(rings):
        t = j / (rings - 1)
        taper = (1 - t) ** 0.72
        cx = bend * t * t + 0.025 * np.sin(t * 8 + seed)
        cy = 0.025 * np.sin(t * 5 + seed)
        rx = width * taper * (0.82 + 0.18 * np.sin(t * 13 + seed))
        ry = width * 0.60 * taper
        for i in range(sides):
            a = 2 * np.pi * i / sides
            vertices.append([cx + rx * np.cos(a), cy + ry * np.sin(a), height * t])
    for j in range(rings - 1):
        for i in range(sides):
            a = j * sides + i
            b = j * sides + (i + 1) % sides
            c = (j + 1) * sides + (i + 1) % sides
            d = (j + 1) * sides + i
            faces.extend([[a, b, c], [a, c, d]])
    mesh = trimesh.Trimesh(vertices=np.array(vertices), faces=np.array(faces), process=True)
    mesh.visual.material = material(color, 0.0, 0.24, [0.45, 0.10, 0.015])
    return mesh

def make_flame():
    s = trimesh.Scene()
    add(s, "Outer flame", curved_flame(1.18, 0.34, -0.10, [255, 91, 24, 255], 1), (0, 0, -0.58))
    add(s, "Amber flame", curved_flame(0.94, 0.25, 0.12, [255, 164, 37, 255], 3), (-0.015, -0.015, -0.56))
    add(s, "Inner flame", curved_flame(0.67, 0.15, -0.04, [255, 224, 125, 255], 5), (0.015, -0.02, -0.55))
    for i, (x, y, z, r) in enumerate([(-.31, .02, -.2, .035), (.28, -.02, .04, .026), (.12, .04, .38, .02), (-.18, -.02, .48, .018)]):
        spark = trimesh.creation.icosphere(subdivisions=2, radius=r)
        spark.visual.material = material([255, 213, 119, 255], 0.0, 0.18, [0.8, 0.3, 0.04])
        add(s, "Flame spark %d" % i, spark, (x, y, z))
    s.export(OUT / "home_stats_flame.glb")

def make_books():
    s = trimesh.Scene()
    # Individual hardcover volumes with page blocks, spine ridges and gold cover details.
    specs = [
        ((0.0, -0.01, -0.39), (0.90, 0.18, 0.43), [55, 78, 112, 255], -0.07, "Book cover 0"),
        ((0.0, 0.015, 0.00), (0.82, 0.18, 0.42), [151, 65, 81, 255], 0.055, "Book cover 1"),
        ((0.0, -0.005, 0.39), (0.88, 0.18, 0.42), [68, 111, 91, 255], -0.045, "Book cover 2"),
    ]
    for idx, (pos, size, col, angle, name) in enumerate(specs):
        page = box((size[0] * .91, size[1] * .91, size[2] * .91), [245, 232, 207, 255], metallic=0.0, roughness=.72)
        add(s, "Book pages %d" % idx, page, (pos[0], pos[1] + .035, pos[2]), (0, 0, angle))
        cover = box(size, col, metallic=.12, roughness=.3)
        add(s, name, cover, pos, (0, 0, angle))
        spine = box((size[0] * .12, size[1] * 1.04, size[2] * .94), [min(255, col[0]+22), min(255, col[1]+22), min(255, col[2]+22), 255], metallic=.16)
        add(s, "Book spine %d" % idx, spine, (pos[0] - size[0]*.43, pos[1] - .01, pos[2]), (0, 0, angle))
        for zoff in (-.12, .12):
            line = box((size[0]*.56, .012, .012), [218, 181, 103, 255], metallic=.72, roughness=.22)
            add(s, "Book gold inlay %d %.2f" % (idx, zoff), line, (pos[0]+.08, pos[1]-.098, pos[2]+zoff), (0, 0, angle))
    s.export(OUT / "home_stats_books.glb")

def lathe(profile, sections=64):
    vertices, faces = [], []
    for radius, z in profile:
        for i in range(sections):
            a = 2*np.pi*i/sections
            vertices.append([radius*np.cos(a), radius*np.sin(a), z])
    for j in range(len(profile)-1):
        for i in range(sections):
            a=j*sections+i; b=j*sections+(i+1)%sections
            c=(j+1)*sections+(i+1)%sections; d=(j+1)*sections+i
            faces.extend([[a,b,c],[a,c,d]])
    return trimesh.Trimesh(vertices=np.array(vertices), faces=np.array(faces), process=True)

def make_trophy():
    s = trimesh.Scene()
    gold = [238, 181, 55, 255]
    bowl = lathe([(0.08, .08),(.25,.08),(.30,.16),(.39,.34),(.43,.57),(.40,.65),(.34,.57),(.30,.37),(.20,.22),(.08,.20)])
    bowl.visual.material = material(gold, .88, .19)
    add(s, "Cup", bowl, (0,0,-.05))
    # Distinct sculpted handles, stem and stepped pedestal.
    for side in (-1, 1):
        handle = trimesh.creation.torus(major_radius=.19, minor_radius=.045, major_sections=48, minor_sections=12)
        handle.visual.material = material([222, 158, 42, 255], .86, .2)
        add(s, "Trophy handle %d" % side, handle, (side*.39, 0, .37), (np.pi/2, 0, side*.18))
    stem = trimesh.creation.cylinder(radius=.085, height=.30, sections=48)
    stem.visual.material = material([248, 202, 91, 255], .9, .17)
    add(s, "Trophy stem", stem, (0,0,-.38))
    for name, radius, height, z in [("Trophy base upper",.22,.10,-.56),("Trophy base middle",.31,.12,-.67),("Trophy base foot",.36,.10,-.78)]:
        part=trimesh.creation.cylinder(radius=radius,height=height,sections=64)
        part.visual.material=material(gold,.88,.2)
        add(s,name,part,(0,0,z))
    s.export(OUT / "home_stats_trophy.glb")

if __name__ == "__main__":
    make_flame()
    make_books()
    make_trophy()
    for path in sorted(OUT.glob("home_stats_*.glb")):
        print(f"Generated {path}: {path.stat().st_size} bytes")
