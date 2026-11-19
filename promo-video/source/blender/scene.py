# Find & Charge promo - procedural EV charging bay scene (Blender 5.3 / bpy, Cycles CPU)
import bpy, bmesh, math, sys, os, time, random, argparse
import numpy as np
from mathutils import Vector, Matrix, Euler, Quaternion
from mathutils.bvhtree import BVHTree

BASE = os.path.dirname(os.path.abspath(__file__))

ap = argparse.ArgumentParser()
ap.add_argument('--shot', default='shot3')
ap.add_argument('--res', default='1280x720')
ap.add_argument('--spp', type=int, default=32)
ap.add_argument('--frames', default='')       # "a-b" or "a,b,c" ; default = full shot
ap.add_argument('--out', default='')
ap.add_argument('--still', action='store_true')
ap.add_argument('--blend', default='')
ap.add_argument('--camcheck', default='')
ap.add_argument('--nomb', action='store_true')
ap.add_argument('--threads', type=int, default=4)
ap.add_argument('--norain', action='store_true')
args = ap.parse_args(sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else [])

bpy.ops.wm.read_factory_settings(use_empty=True)
try:
    bpy.context.preferences.edit.keyframe_new_interpolation_type = 'LINEAR'
except Exception:
    pass
scene = bpy.context.scene
random.seed(7)

# ---------------------------------------------------------------- helpers
def srgb2lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

def hexcol(h):
    h = h.lstrip('#')
    return tuple(srgb2lin(int(h[i:i + 2], 16)) for i in (0, 2, 4))

GREEN = hexcol('#4CAF50'); LGREEN = hexcol('#8BC34A'); BLUE = hexcol('#1E88E5')
NAVY = hexcol('#0D1B3D'); ORANGE = hexcol('#F59E0B')

def link(obj, parent=None):
    scene.collection.objects.link(obj)
    if parent is not None:
        obj.parent = parent
    return obj

def new_mat(name, color=(0.5, 0.5, 0.5), rough=0.5, metal=0.0, coat=0.0, coat_rough=0.03,
            emis=None, estr=0.0, spec=0.5, transmission=0.0, ior=1.45, sheen=0.0):
    m = bpy.data.materials.new(name)
    b = m.node_tree.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = (*color, 1)
    b.inputs['Roughness'].default_value = rough
    b.inputs['Metallic'].default_value = metal
    b.inputs['Coat Weight'].default_value = coat
    b.inputs['Coat Roughness'].default_value = coat_rough
    b.inputs['Specular IOR Level'].default_value = spec
    b.inputs['Transmission Weight'].default_value = transmission
    b.inputs['IOR'].default_value = ior
    b.inputs['Sheen Weight'].default_value = sheen
    if emis is not None:
        b.inputs['Emission Color'].default_value = (*emis, 1)
        b.inputs['Emission Strength'].default_value = estr
    return m

def emis_mat(name, color, strength, sample=True):
    m = bpy.data.materials.new(name)
    if not sample: m.cycles.emission_sampling = 'NONE'
    nt = m.node_tree
    for n in list(nt.nodes): nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    e = nt.nodes.new('ShaderNodeEmission')
    e.inputs['Color'].default_value = (*color, 1)
    e.inputs['Strength'].default_value = strength
    nt.links.new(e.outputs[0], out.inputs['Surface'])
    return m, e

def mesh_obj(name, verts, faces, mat=None, smooth=True, parent=None, subsurf=0, render_sub=None):
    me = bpy.data.meshes.new(name)
    me.from_pydata([tuple(v) for v in verts], [], faces)
    me.validate()
    bm = bmesh.new(); bm.from_mesh(me)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me); bm.free()
    if smooth:
        me.polygons.foreach_set('use_smooth', [True] * len(me.polygons))
    if mat is not None:
        me.materials.append(mat)
    ob = bpy.data.objects.new(name, me)
    link(ob, parent)
    if subsurf:
        m = ob.modifiers.new('sub', 'SUBSURF'); m.levels = subsurf
        m.render_levels = render_sub if render_sub else subsurf
    return ob

def grid_faces(nu, nv, closed_v=True):
    f = []
    for i in range(nu - 1):
        for j in range(nv if closed_v else nv - 1):
            j2 = (j + 1) % nv
            f.append((i * nv + j, i * nv + j2, (i + 1) * nv + j2, (i + 1) * nv + j))
    return f

def loft(name, rings, mat=None, cap=True, **kw):
    """rings: list of lists of Vector (same count each). closed rings, end caps fan."""
    nu = len(rings); nv = len(rings[0])
    verts = [v for r in rings for v in r]
    faces = grid_faces(nu, nv)
    if cap:
        c0 = sum(rings[0], Vector()) / nv; c1 = sum(rings[-1], Vector()) / nv
        i0 = len(verts); verts.append(c0); i1 = len(verts); verts.append(c1)
        for j in range(nv):
            faces.append((i0, (j + 1) % nv, j))
            faces.append((i1, (nu - 1) * nv + j, (nu - 1) * nv + (j + 1) % nv))
    return mesh_obj(name, verts, faces, mat, **kw)

def ellipse_ring(z, rx, ry, n=24, cx=0.0, cy=0.0, p=2.0):
    out = []
    for k in range(n):
        a = 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        x = math.copysign(abs(c) ** (2 / p), c); y = math.copysign(abs(s) ** (2 / p), s)
        out.append(Vector((cx + rx * x, cy + ry * y, z)))
    return out

def limb(name, length, r0, r1, mat, n=20, parent=None, rx_scale=1.0, sub=1):
    """capsule-ish limb from origin down -Z by length, radii r0 at top, r1 at bottom"""
    rings = []
    # top dome
    for k in range(4, 0, -1):
        a = k / 4 * math.pi / 2
        rings.append(ellipse_ring(r0 * math.sin(a) * 0.8, r0 * math.cos(a) * rx_scale, r0 * math.cos(a), n))
    steps = 8
    for i in range(steps + 1):
        t = i / steps
        r = r0 + (r1 - r0) * t
        r *= 1 + 0.06 * math.sin(math.pi * t)
        rings.append(ellipse_ring(-length * t, r * rx_scale, r, n))
    for k in range(1, 5):
        a = k / 4 * math.pi / 2
        rings.append(ellipse_ring(-length - r1 * math.sin(a) * 0.8, r1 * math.cos(a) * rx_scale + 1e-4, r1 * math.cos(a) + 1e-4, n))
    return loft(name, rings, mat, cap=True, parent=parent, subsurf=sub)

def pchip(xs, ys):
    xs = list(xs); ys = list(ys); n = len(xs)
    h = [xs[i + 1] - xs[i] for i in range(n - 1)]
    d = [(ys[i + 1] - ys[i]) / h[i] for i in range(n - 1)]
    m = [0.0] * n
    m[0] = d[0]; m[-1] = d[-1]
    for i in range(1, n - 1):
        if d[i - 1] * d[i] <= 0: m[i] = 0
        else:
            w1 = 2 * h[i] + h[i - 1]; w2 = h[i] + 2 * h[i - 1]
            m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i])
    def f(x):
        if x <= xs[0]: return ys[0]
        if x >= xs[-1]: return ys[-1]
        i = max(0, min(n - 2, next(k for k in range(n - 1) if x <= xs[k + 1])))
        t = (x - xs[i]) / h[i]
        h00 = 2 * t**3 - 3 * t**2 + 1; h10 = t**3 - 2 * t**2 + t
        h01 = -2 * t**3 + 3 * t**2; h11 = t**3 - t**2
        return h00 * ys[i] + h10 * h[i] * m[i] + h01 * ys[i + 1] + h11 * h[i] * m[i + 1]
    return f

def catmull_closed(pts, per):
    n = len(pts); out = []
    for i in range(n):
        p0, p1, p2, p3 = pts[(i - 1) % n], pts[i], pts[(i + 1) % n], pts[(i + 2) % n]
        for k in range(per):
            t = k / per; t2 = t * t; t3 = t2 * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    return out

def smoothstep(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a))) if b != a else (1.0 if x >= b else 0.0)
    return t * t * (3 - 2 * t)

def lerp(a, b, t): return a + (b - a) * t

def curve_obj(name, pts, radius, mat, parent=None, closed=False, order=3, res=6):
    cu = bpy.data.curves.new(name, 'CURVE'); cu.dimensions = '3D'
    cu.bevel_depth = radius; cu.bevel_resolution = 3; cu.resolution_u = res
    cu.use_fill_caps = True
    sp = cu.splines.new('NURBS'); sp.points.add(len(pts) - 1)
    for p, v in zip(sp.points, pts): p.co = (v[0], v[1], v[2], 1)
    sp.use_endpoint_u = True; sp.order_u = min(order, len(pts)); sp.use_cyclic_u = closed
    if mat: cu.materials.append(mat)
    ob = bpy.data.objects.new(name, cu); link(ob, parent)
    return ob

def box(name, size, loc, mat, parent=None, bevel=0.0, seg=3):
    sx, sy, sz = [s / 2 for s in size]
    v = [(-sx, -sy, -sz), (sx, -sy, -sz), (sx, sy, -sz), (-sx, sy, -sz), (-sx, -sy, sz), (sx, -sy, sz), (sx, sy, sz), (-sx, sy, sz)]
    f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7)]
    ob = mesh_obj(name, v, f, mat, smooth=bevel > 0, parent=parent)
    ob.location = loc
    if bevel > 0:
        m = ob.modifiers.new('bev', 'BEVEL'); m.width = bevel; m.segments = seg; m.limit_method = 'NONE'
        try: m.harden_normals = False
        except Exception: pass
    return ob

def cylinder(name, r, depth, loc, mat, parent=None, n=32, axis='Z', bevel=0.0, smooth=True, caps=True):
    v = []; f = []
    for k in range(n):
        a = 2 * math.pi * k / n
        v.append((r * math.cos(a), r * math.sin(a), -depth / 2))
        v.append((r * math.cos(a), r * math.sin(a), depth / 2))
    for k in range(n):
        a, b = 2 * k, 2 * ((k + 1) % n)
        f.append((a, b, b + 1, a + 1))
    if caps: f.append(tuple(2 * k for k in range(n))[::-1]); f.append(tuple(2 * k + 1 for k in range(n)))
    ob = mesh_obj(name, v, f, mat, smooth=False, parent=parent)
    if smooth:
        for p in ob.data.polygons:
            p.use_smooth = len(p.vertices) == 4
    if axis == 'X': ob.rotation_euler = (0, math.pi / 2, 0)
    elif axis == 'Y': ob.rotation_euler = (math.pi / 2, 0, 0)
    ob.location = loc
    if bevel > 0:
        m = ob.modifiers.new('bev', 'BEVEL'); m.width = bevel; m.segments = 3; m.limit_method = 'ANGLE'
    return ob

def look_quat(loc, target, up='Y'):
    return (Vector(target) - Vector(loc)).to_track_quat('-Z', up)

def np_image(name, arr):
    h, w, _ = arr.shape
    img = bpy.data.images.new(name, w, h, alpha=True)
    img.pixels.foreach_set(np.ascontiguousarray(arr[::-1]).astype(np.float32).ravel())
    img.pack()
    return img

def in_poly(X, Y, poly):
    inside = np.zeros_like(X, dtype=bool)
    n = len(poly)
    for i in range(n):
        x1, y1 = poly[i]; x2, y2 = poly[(i + 1) % n]
        cond = ((y1 > Y) != (y2 > Y)) & (X < (x2 - x1) * (Y - y1) / (y2 - y1 + 1e-12) + x1)
        inside ^= cond
    return inside

BOLT = [(0.56, 0.86), (0.38, 0.56), (0.49, 0.56), (0.42, 0.34), (0.64, 0.66), (0.52, 0.66), (0.61, 0.86)]

def pin_mask(X, Y, cx=0.5, cy=0.60, r=0.30, tip=0.06):
    circ = (X - cx) ** 2 + (Y - cy) ** 2 < r * r
    t = np.clip((Y - tip) / (cy - tip), 0, 1)
    tri = (Y < cy) & (Y > tip) & (np.abs(X - cx) < r * np.sin(np.arccos(np.clip(1 - t, -1, 1))) * t ** 0.6)
    return circ | tri

def logo_image(name, size=256, white=False):
    ys, xs = np.mgrid[0:size, 0:size] / size
    Y = 1 - ys; X = xs
    a = np.zeros((size, size, 4))
    pin = pin_mask(X, Y)
    # bolt scaled into circle
    bolt = in_poly((X - 0.5) / 0.62 + 0.5, (Y - 0.60) / 0.62 + 0.60, BOLT)
    if white:
        inner = pin_mask((X - 0.5) / 0.80 + 0.5, (Y - 0.53) / 0.80 + 0.6)
        ring = pin & ~inner
        m = ring | bolt
        a[m] = (1, 1, 1, 1)
    else:
        a[pin] = (0.30, 0.69, 0.31, 1)
        a[pin & bolt] = (0.96, 0.62, 0.04, 1)
    return np_image(name, a)

def screen_image(name, col, mode):
    W, H = 320, 200
    ys, xs = np.mgrid[0:H, 0:W]
    X = (xs - W / 2) / H; Y = (H / 2 - ys) / H
    img = np.zeros((H, W, 4)); img[..., :3] = (0.02, 0.05, 0.12); img[..., 3] = 1
    r = np.sqrt(X ** 2 + Y ** 2); ang = (np.arctan2(X, Y) + 2 * np.pi) % (2 * np.pi)
    frac = {'red': 1.0, 'green': 1.0, 'cyan': 0.62}[mode]
    ring = (np.abs(r - 0.34) < 0.035) & (ang <= frac * 2 * np.pi)
    ringbg = (np.abs(r - 0.34) < 0.035)
    img[ringbg, :3] = (0.08, 0.1, 0.16)
    img[ring, :3] = col
    if mode == 'red':
        m = (np.abs(np.abs(X) - np.abs(Y)) < 0.03) & (np.abs(X) < 0.15)
    elif mode == 'green':
        m = (((np.abs(Y + 0.06 - (X + 0.12) * -1.0)) < 0.035) & (X > -0.17) & (X < -0.03)) | \
            ((np.abs(Y + 0.06 - (X + 0.12) * 1.6 + 0.0)) < 0.04) & (X > -0.06) & (X < 0.17)
        m = (np.abs((Y + 0.03) + (X + 0.08)) < 0.035) & (X > -0.18) & (X < -0.06) | \
            (np.abs((Y + 0.15) - 1.45 * (X + 0.06) - 0.03) < 0.04) & (X > -0.08) & (X < 0.16)
    else:
        m = in_poly(X * 1.0 + 0.5, Y * 1.0 + 0.55, BOLT)
    img[m, :3] = col
    return np_image(name, img)

def img_emis_mat(name, img, strength):
    m = bpy.data.materials.new(name)
    nt = m.node_tree
    for n in list(nt.nodes): nt.nodes.remove(n)
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    tex = nt.nodes.new('ShaderNodeTexImage'); tex.image = img
    e = nt.nodes.new('ShaderNodeEmission'); e.inputs['Strength'].default_value = strength
    g = nt.nodes.new('ShaderNodeBsdfPrincipled'); g.inputs['Roughness'].default_value = 0.05
    g.inputs['Base Color'].default_value = (0, 0, 0, 1)
    add = nt.nodes.new('ShaderNodeAddShader')
    nt.links.new(tex.outputs['Color'], e.inputs['Color'])
    nt.links.new(e.outputs[0], add.inputs[0]); nt.links.new(g.outputs[0], add.inputs[1])
    nt.links.new(add.outputs[0], out.inputs['Surface'])
    return m, e

def decal_mat(name, img, base=None, rough=0.35, emis=0.0):
    m = bpy.data.materials.new(name)
    nt = m.node_tree
    b = nt.nodes['Principled BSDF']; out = nt.nodes['Material Output']
    tex = nt.nodes.new('ShaderNodeTexImage'); tex.image = img; tex.extension = 'CLIP'
    nt.links.new(tex.outputs['Color'], b.inputs['Base Color'])
    b.inputs['Roughness'].default_value = rough
    tr = nt.nodes.new('ShaderNodeBsdfTransparent')
    mix = nt.nodes.new('ShaderNodeMixShader')
    nt.links.new(tex.outputs['Alpha'], mix.inputs[0])
    nt.links.new(tr.outputs[0], mix.inputs[1]); nt.links.new(b.outputs[0], mix.inputs[2])
    nt.links.new(mix.outputs[0], out.inputs['Surface'])
    if emis > 0:
        nt.links.new(tex.outputs['Color'], b.inputs['Emission Color'])
        b.inputs['Emission Strength'].default_value = emis
    return m

def plane(name, w, h, loc, mat, rot=(0, 0, 0), parent=None, uv=True):
    v = [(-w / 2, -h / 2, 0), (w / 2, -h / 2, 0), (w / 2, h / 2, 0), (-w / 2, h / 2, 0)]
    ob = mesh_obj(name, v, [(0, 1, 2, 3)], mat, smooth=False, parent=parent)
    if uv:
        uvl = ob.data.uv_layers.new(name='UVMap')
        for i, co in enumerate([(0, 0), (1, 0), (1, 1), (0, 1)]):
            uvl.data[i].uv = co
    ob.location = loc; ob.rotation_euler = rot
    return ob

def noshadow(ob):
    try:
        ob.visible_shadow = False
    except Exception:
        pass

# ---------------------------------------------------------------- shot config
SHOT = args.shot
CFG = {
    'shot1': dict(frames=90, led='red', car='grey', person=False, plugged=True, warm=0.7),
    'shot2': dict(frames=75, led='green', car=None, person=False, plugged=False, warm=1.15),
    'shot3': dict(frames=180, led='green', car='blue', person=True, plugged=False, warm=1.0),
    'shot4': dict(frames=60, led='cyan', car='blue', person=True, plugged=False, warm=1.0),
}[SHOT]
NF = CFG['frames']

# ---------------------------------------------------------------- world (blue hour gradient)
world = bpy.data.worlds.new('World'); scene.world = world
wn = world.node_tree; [wn.nodes.remove(n) for n in list(wn.nodes)]
wout = wn.nodes.new('ShaderNodeOutputWorld')
tc = wn.nodes.new('ShaderNodeTexCoord')
sep = wn.nodes.new('ShaderNodeSeparateXYZ'); wn.links.new(tc.outputs['Generated'], sep.inputs[0])
ramp = wn.nodes.new('ShaderNodeValToRGB')
wn.links.new(sep.outputs['Z'], ramp.inputs[0])
cr = ramp.color_ramp
cr.elements[0].position = 0.0; cr.elements[0].color = (0.004, 0.005, 0.008, 1)
cr.elements[1].position = 0.55; cr.elements[1].color = (0.006, 0.014, 0.045, 1)
e = cr.elements.new(0.06); e.color = (0.06, 0.075, 0.13, 1)
e = cr.elements.new(0.02); e.color = (0.05, 0.05, 0.075, 1)
e = cr.elements.new(0.18); e.color = (0.025, 0.045, 0.11, 1)
# warm glow on one azimuth (sunset afterglow, -Y side far)
dot = wn.nodes.new('ShaderNodeVectorMath'); dot.operation = 'DOT_PRODUCT'
dot.inputs[1].default_value = (-0.55, -0.83, 0.0)
wn.links.new(tc.outputs['Generated'], dot.inputs[0])
glowr = wn.nodes.new('ShaderNodeMapRange'); glowr.inputs['From Min'].default_value = 0.4; glowr.inputs['From Max'].default_value = 1.0
wn.links.new(dot.outputs['Value'], glowr.inputs['Value'])
hmask = wn.nodes.new('ShaderNodeMapRange'); hmask.inputs['From Min'].default_value = 0.25; hmask.inputs['From Max'].default_value = 0.0
wn.links.new(sep.outputs['Z'], hmask.inputs['Value'])
mulm = wn.nodes.new('ShaderNodeMath'); mulm.operation = 'MULTIPLY'
wn.links.new(glowr.outputs[0], mulm.inputs[0]); wn.links.new(hmask.outputs[0], mulm.inputs[1])
pos = wn.nodes.new('ShaderNodeMath'); pos.operation = 'GREATER_THAN'; pos.inputs[1].default_value = -0.01
wn.links.new(sep.outputs['Z'], pos.inputs[0])
mulm2 = wn.nodes.new('ShaderNodeMath'); mulm2.operation = 'MULTIPLY'
wn.links.new(mulm.outputs[0], mulm2.inputs[0]); wn.links.new(pos.outputs[0], mulm2.inputs[1])
mixc = wn.nodes.new('ShaderNodeMix'); mixc.data_type = 'RGBA'; mixc.blend_type = 'ADD'
wn.links.new(mulm2.outputs[0], mixc.inputs['Factor'])
wn.links.new(ramp.outputs['Color'], mixc.inputs['A'])
mixc.inputs['B'].default_value = (0.20 * CFG['warm'], 0.07 * CFG['warm'], 0.025, 1)
bg = wn.nodes.new('ShaderNodeBackground'); bg.inputs['Strength'].default_value = 2.0
wn.links.new(mixc.outputs['Result'], bg.inputs['Color'])
wn.links.new(bg.outputs[0], wout.inputs['Surface'])

# ---------------------------------------------------------------- ground & lot
def asphalt_mat():
    m = bpy.data.materials.new('Asphalt')
    nt = m.node_tree; b = nt.nodes['Principled BSDF']
    tc = nt.nodes.new('ShaderNodeTexCoord')
    n1 = nt.nodes.new('ShaderNodeTexNoise'); n1.inputs['Scale'].default_value = 0.35; n1.inputs['Detail'].default_value = 6
    n1.inputs['Roughness'].default_value = 0.6
    nt.links.new(tc.outputs['Object'], n1.inputs['Vector'])
    pr = nt.nodes.new('ShaderNodeMapRange'); pr.inputs['From Min'].default_value = 0.53; pr.inputs['From Max'].default_value = 0.585
    nt.links.new(n1.outputs['Fac'], pr.inputs['Value'])          # puddle mask 0..1
    n2 = nt.nodes.new('ShaderNodeTexNoise'); n2.inputs['Scale'].default_value = 260; n2.inputs['Detail'].default_value = 2
    nt.links.new(tc.outputs['Object'], n2.inputs['Vector'])
    n3 = nt.nodes.new('ShaderNodeTexNoise'); n3.inputs['Scale'].default_value = 4; n3.inputs['Detail'].default_value = 4
    nt.links.new(tc.outputs['Object'], n3.inputs['Vector'])
    cr = nt.nodes.new('ShaderNodeMapRange'); cr.inputs['To Min'].default_value = 0.018; cr.inputs['To Max'].default_value = 0.05
    nt.links.new(n3.outputs['Fac'], cr.inputs['Value'])
    mcol = nt.nodes.new('ShaderNodeMath'); mcol.operation = 'MULTIPLY_ADD'
    nt.links.new(n2.outputs['Fac'], mcol.inputs[0]); mcol.inputs[1].default_value = 0.03
    nt.links.new(cr.outputs[0], mcol.inputs[2])
    # puddles darker
    dk = nt.nodes.new('ShaderNodeMapRange'); dk.inputs['To Min'].default_value = 1.0; dk.inputs['To Max'].default_value = 0.55
    nt.links.new(pr.outputs[0], dk.inputs['Value'])
    mc2 = nt.nodes.new('ShaderNodeMath'); mc2.operation = 'MULTIPLY'
    nt.links.new(mcol.outputs[0], mc2.inputs[0]); nt.links.new(dk.outputs[0], mc2.inputs[1])
    nt.links.new(mc2.outputs[0], b.inputs['Base Color'])
    rr = nt.nodes.new('ShaderNodeMapRange'); rr.inputs['To Min'].default_value = 0.30; rr.inputs['To Max'].default_value = 0.025
    nt.links.new(pr.outputs[0], rr.inputs['Value'])
    rn = nt.nodes.new('ShaderNodeMath'); rn.operation = 'MULTIPLY_ADD'
    nt.links.new(n3.outputs['Fac'], rn.inputs[0]); rn.inputs[1].default_value = 0.12; nt.links.new(rr.outputs[0], rn.inputs[2])
    nt.links.new(rn.outputs[0], b.inputs['Roughness'])
    inv = nt.nodes.new('ShaderNodeMapRange'); inv.inputs['To Min'].default_value = 0.35; inv.inputs['To Max'].default_value = 0.0
    nt.links.new(pr.outputs[0], inv.inputs['Value'])
    bump = nt.nodes.new('ShaderNodeBump'); bump.inputs['Distance'].default_value = 0.002
    nt.links.new(inv.outputs[0], bump.inputs['Strength']); nt.links.new(n2.outputs['Fac'], bump.inputs['Height'])
    nt.links.new(bump.outputs['Normal'], b.inputs['Normal'])
    b.inputs['Specular IOR Level'].default_value = 0.5
    return m

ground = plane('Ground', 140, 140, (0, 0, 0), asphalt_mat(), uv=False)

def paint_mat(name, col, rough=0.28):
    m = new_mat(name, col, rough=rough)
    nt = m.node_tree; b = nt.nodes['Principled BSDF']
    tc = nt.nodes.new('ShaderNodeTexCoord')
    n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = 30; n.inputs['Detail'].default_value = 8
    nt.links.new(tc.outputs['Object'], n.inputs['Vector'])
    mr = nt.nodes.new('ShaderNodeMapRange'); mr.inputs['From Min'].default_value = 0.35; mr.inputs['From Max'].default_value = 0.65
    nt.links.new(n.outputs['Fac'], mr.inputs['Value'])
    mix = nt.nodes.new('ShaderNodeMix'); mix.data_type = 'RGBA'
    mix.inputs['A'].default_value = (0.03, 0.03, 0.03, 1); mix.inputs['B'].default_value = (*col, 1)
    nt.links.new(mr.outputs[0], mix.inputs['Factor']); nt.links.new(mix.outputs['Result'], b.inputs['Base Color'])
    return m

white_paint = paint_mat('LinePaint', (0.62, 0.62, 0.6))
green_paint = paint_mat('BayGreen', tuple(c * 0.75 for c in GREEN), rough=0.22)
_mr = [n for n in green_paint.node_tree.nodes if n.type == 'MAP_RANGE'][0]; _mr.inputs['From Min'].default_value = 0.22; _mr.inputs['From Max'].default_value = 0.40

# bays: width 2.6 along X, length 5.0 along Y (y 0.3..5.3); green EV bay centred x=0
BAYW = 2.6
for i in range(-3, 4):
    x = (i + 0.5) * BAYW
    plane('Line%d' % i, 0.12, 5.0, (x, 2.8, 0.003), white_paint, uv=False)
plane('BayGreen', BAYW - 0.12, 4.9, (0, 2.85, 0.002), green_paint, uv=False)
evsym = plane('EVSymbol', 1.5, 1.5, (0, 1.6, 0.004), decal_mat('EVSym', logo_image('logo_white', 256, white=True), rough=0.25),
              rot=(0, 0, math.pi), uv=True)
# far row bays (opposite side)
for i in range(-4, 5):
    plane('LineF%d' % i, 0.12, 5.0, ((i + 0.5) * BAYW, -6.5, 0.003), white_paint, uv=False)

concrete = new_mat('Concrete', (0.06, 0.06, 0.06), rough=0.28)
nt = concrete.node_tree; bsdf = nt.nodes['Principled BSDF']
nz = nt.nodes.new('ShaderNodeTexNoise'); nz.inputs['Scale'].default_value = 40
bp = nt.nodes.new('ShaderNodeBump'); bp.inputs['Strength'].default_value = 0.15
nt.links.new(nz.outputs['Fac'], bp.inputs['Height']); nt.links.new(bp.outputs['Normal'], bsdf.inputs['Normal'])
box('Curb', (40, 0.25, 0.14), (0, 5.95, 0.07), concrete, bevel=0.02)
box('Sidewalk', (40, 4.0, 0.13), (0, 8.1, 0.065), concrete)
hedge = new_mat('Hedge', (0.012, 0.02, 0.012), rough=0.85)
_n = hedge.node_tree.nodes; _b = _n['Principled BSDF']
_v = _n.new('ShaderNodeTexVoronoi'); _v.inputs['Scale'].default_value = 9
_bp = _n.new('ShaderNodeBump'); _bp.inputs['Strength'].default_value = 0.8
hedge.node_tree.links.new(_v.outputs['Distance'], _bp.inputs['Height']); hedge.node_tree.links.new(_bp.outputs['Normal'], _b.inputs['Normal'])
box('Hedge', (40, 1.2, 1.1), (0, 10.6, 0.55), hedge, bevel=0.3, seg=3)
box('HedgeS', (40, 1.2, 1.1), (0, -10.0, 0.55), hedge, bevel=0.3, seg=3)

# far building with windows
bldg = new_mat('Building', (0.03, 0.03, 0.035), rough=0.7)
bldg.cycles.emission_sampling = 'NONE'
nt = bldg.node_tree; bsdf = nt.nodes['Principled BSDF']
tcn = nt.nodes.new('ShaderNodeTexCoord')
brick = nt.nodes.new('ShaderNodeTexBrick'); brick.inputs['Scale'].default_value = 0.5
brick.inputs['Brick Width'].default_value = 1.1; brick.inputs['Row Height'].default_value = 1.4
brick.inputs['Mortar Size'].default_value = 0.3; brick.offset = 0.0
brick.inputs['Color1'].default_value = (0, 0, 0, 1); brick.inputs['Color2'].default_value = (1, 1, 1, 1)
brick.inputs['Mortar'].default_value = (0, 0, 0, 1)
mapn = nt.nodes.new('ShaderNodeMapping'); nt.links.new(tcn.outputs['Object'], mapn.inputs['Vector'])
sxz = nt.nodes.new('ShaderNodeSeparateXYZ'); nt.links.new(tcn.outputs['Object'], sxz.inputs[0])
cxz = nt.nodes.new('ShaderNodeCombineXYZ')
nt.links.new(sxz.outputs['X'], cxz.inputs['X']); nt.links.new(sxz.outputs['Z'], cxz.inputs['Y'])
nt.links.new(cxz.outputs[0], brick.inputs['Vector'])
thr = nt.nodes.new('ShaderNodeMath'); thr.operation = 'GREATER_THAN'; thr.inputs[1].default_value = 0.62
sepc = nt.nodes.new('ShaderNodeSeparateColor'); nt.links.new(brick.outputs['Color'], sepc.inputs[0])
_sn = nt.nodes.new('ShaderNodeVectorMath'); _sn.operation = 'SNAP'; _sn.inputs[1].default_value = (2.2, 2.8, 1.0)
_mp = nt.nodes.new('ShaderNodeVectorMath'); _mp.operation = 'MULTIPLY'; _mp.inputs[1].default_value = (1.0, 1.0, 1.0)
nt.links.new(cxz.outputs[0], _mp.inputs[0]); nt.links.new(_mp.outputs[0], _sn.inputs[0])
_wn = nt.nodes.new('ShaderNodeTexWhiteNoise'); _wn.noise_dimensions = '3D'
nt.links.new(_sn.outputs[0], _wn.inputs['Vector'])
nt.links.new(_wn.outputs['Value'], thr.inputs[0])
mfac = nt.nodes.new('ShaderNodeMath'); mfac.operation = 'MULTIPLY'
nt.links.new(thr.outputs[0], mfac.inputs[0]); nt.links.new(brick.outputs['Fac'], mfac.inputs[1])
inv1 = nt.nodes.new('ShaderNodeMath'); inv1.operation = 'SUBTRACT'; inv1.inputs[0].default_value = 1.0
nt.links.new(brick.outputs['Fac'], inv1.inputs[1])
mfac2 = nt.nodes.new('ShaderNodeMath'); mfac2.operation = 'MULTIPLY'
nt.links.new(thr.outputs[0], mfac2.inputs[0]); nt.links.new(inv1.outputs[0], mfac2.inputs[1])
bsdf.inputs['Emission Color'].default_value = (1.0, 0.62, 0.32, 1)
mulE = nt.nodes.new('ShaderNodeMath'); mulE.operation = 'MULTIPLY'; mulE.inputs[1].default_value = 2.2
nt.links.new(mfac2.outputs[0], mulE.inputs[0]); nt.links.new(mulE.outputs[0], bsdf.inputs['Emission Strength'])
for i, (x, y, w, h) in enumerate([(-8, 48, 34, 16), (30, 44, 20, 24), (-40, 38, 18, 12), (5, -50, 44, 14), (-36, -42, 22, 20)]):
    bo = box('Bldg%d' % i, (w, 8, h), (x, y, h / 2), bldg)
    if y < 0: bo.rotation_euler = (0, 0, math.pi)

# trees: dark blob silhouettes
leaf = new_mat('Leaf', (0.01, 0.015, 0.01), rough=0.8)
for (x, y, s) in []:
    o = loft('Tree', [ellipse_ring(z, rr, rr, 16) for z, rr in [(2.0, 0.2), (2.4, s * 0.8), (3.6, s), (4.8, s * 0.8), (5.6, 0.3)]], leaf, subsurf=2)
    o.location = (x, y, 0)
    cylinder('Trunk', 0.15, 2.4, (x, y, 1.2), leaf)

# street lights
pole_mat = new_mat('Pole', (0.05, 0.05, 0.055), rough=0.4, metal=0.8)
lamp_e, _ = emis_mat('LampE', (1.0, 0.62, 0.30), 60, False)
def streetlight(x, y, h=6.0, face=0.0, power=900):
    cylinder('Pole', 0.07, h, (x, y, h / 2), pole_mat)
    off = Vector((0, -1.0, 0)); off.rotate(Euler((0, 0, face)))
    arm = box('Arm', (0.12, 1.1, 0.08), (x + off.x * 0.5, y + off.y * 0.5, h), pole_mat)
    arm.rotation_euler = (0, 0, face)
    hx, hy = x + off.x, y + off.y
    head = box('Head', (0.35, 0.6, 0.1), (hx, hy, h - 0.02), pole_mat, bevel=0.03); head.rotation_euler = (0, 0, face)
    pl = plane('LampPanel', 0.28, 0.5, (hx, hy, h - 0.075), lamp_e, rot=(math.pi, 0, face), uv=False); noshadow(pl)
    if power > 0:
        L = bpy.data.lights.new('Street', 'SPOT'); L.energy = power; L.spot_size = math.radians(120); L.spot_blend = 0.6
        L.color = (1.0, 0.68, 0.38); L.shadow_soft_size = 0.25
        lo = bpy.data.objects.new('StreetL', L); link(lo); lo.location = (hx, hy, h - 0.12)
    return (hx, hy, h)

warm = CFG['warm']
SL = []
SL.append(streetlight(3.4, 8.6, 6.2, face=math.radians(160), power=850 * warm))
SL.append(streetlight(-7.5, 8.8, 6.2, face=math.radians(180), power=900 * warm))
SL.append(streetlight(14, 9, 6.2, face=math.radians(180), power=600))
SL.append(streetlight(-4, -10.5, 6.2, face=0.0, power=700))
SL.append(streetlight(9, -10.5, 6.2, face=0.0, power=500))
SL.append(streetlight(-20, 9, 6.2, face=math.radians(180), power=0))
SL.append(streetlight(26, 9, 6.2, face=math.radians(180), power=0))
SL.append(streetlight(-18, -11, 6.2, face=0.0, power=0))
# distant city bokeh points
bok_w, _ = emis_mat('BokehWarm', (1.0, 0.55, 0.25), 40, False)
bok_c, _ = emis_mat('BokehCool', (0.7, 0.8, 1.0), 25, False)
bok_r, _ = emis_mat('BokehRed', (1.0, 0.08, 0.04), 30, False)
for i in range(70):
    ang = random.uniform(0, 2 * math.pi); d = random.uniform(22, 60)
    x, y = d * math.cos(ang), d * math.sin(ang)
    z = random.choice([random.uniform(0.6, 1.2), random.uniform(2, 9)])
    mat = random.choice([bok_w, bok_w, bok_w, bok_c, bok_r])
    o = cylinder('Bok', random.uniform(0.08, 0.18), 0.05, (x, y, z), mat, n=10, smooth=False)
    o.rotation_euler = (math.pi / 2, 0, ang + math.pi / 2); noshadow(o)

# canopy over the charging bays (gives strip highlights on paint)
canopy_m = new_mat('Canopy', (0.06, 0.065, 0.07), rough=0.45, metal=0.6)
can = box('Canopy', (9.0, 3.2, 0.18), (0, 6.6, 3.9), canopy_m, bevel=0.03)
for x in (-4.2, 4.2):
    box('CanPost', (0.18, 0.18, 3.9), (x, 7.4, 1.95), canopy_m, bevel=0.02)
can_e, can_emis = emis_mat('CanopyLED', (0.85, 0.92, 1.0), 6.0 * (0.6 if SHOT == 'shot1' else 1.0))
for x in (-2.6, 0.0, 2.6):
    s = plane('CanStrip', 0.06, 2.8, (x, 6.4, 3.80), can_e, rot=(math.pi, 0, 0), uv=False)
# canopy edge LED (brand green line)
can_g, _ = emis_mat('CanopyGreen', (0.15, 1.0, 0.2), 3.0)
plane('CanEdge', 9.0, 0.03, (0, 4.99, 3.9), can_g, rot=(math.pi / 2, 0, 0), uv=False)

# ---------------------------------------------------------------- charging pillar
PIL = Vector((0.95, 5.55, 0.0))
pil = bpy.data.objects.new('Pillar', None); link(pil); pil.location = PIL
pil_body = new_mat('PillarBody', (0.8, 0.8, 0.8), rough=0.25, coat=0.6, coat_rough=0.05)
nt = pil_body.node_tree; b = nt.nodes['Principled BSDF']
tcp = nt.nodes.new('ShaderNodeTexCoord'); sp = nt.nodes.new('ShaderNodeSeparateXYZ')
nt.links.new(tcp.outputs['Object'], sp.inputs[0])
st = nt.nodes.new('ShaderNodeMath'); st.operation = 'GREATER_THAN'; st.inputs[1].default_value = -0.12
nt.links.new(sp.outputs['Z'], st.inputs[0])
mx = nt.nodes.new('ShaderNodeMix'); mx.data_type = 'RGBA'
mx.inputs['A'].default_value = (0.035, 0.038, 0.045, 1); mx.inputs['B'].default_value = (0.75, 0.76, 0.78, 1)
nt.links.new(st.outputs[0], mx.inputs['Factor']); nt.links.new(mx.outputs['Result'], b.inputs['Base Color'])
plinth = box('Plinth', (0.55, 0.45, 0.08), (0, 0, 0.04), concrete, parent=pil, bevel=0.01)
pbody = box('PillarBody', (0.32, 0.24, 1.55), (0, 0, 0.08 + 0.775), pil_body, parent=pil, bevel=0.07, seg=8)
blackgl = new_mat('BlackGloss', (0.01, 0.01, 0.012), rough=0.08, coat=1.0)
fpanel = box('FrontPanel', (0.24, 0.02, 0.62), (0, -0.115, 1.06), blackgl, parent=pil, bevel=0.03, seg=4)
led_col = {'red': (1.0, 0.04, 0.02), 'green': (0.12, 1.0, 0.18), 'cyan': (0.12, 1.0, 0.18)}[CFG['led']]
led_m, led_e = emis_mat('PillarLED', led_col, 25.0)
# vertical LED strips on both front corners + top halo
for sx in (-1, 1):
    s = box('LEDStrip', (0.012, 0.012, 1.2), (sx * 0.152, -0.112, 0.80), led_m, parent=pil)
halo = box('LEDHalo', (0.30, 0.22, 0.012), (0, 0, 1.55), led_m, parent=pil)
# screen
scr_imgs = {'red': screen_image('scr_red', (1.0, 0.1, 0.06), 'red'),
            'green': screen_image('scr_green', (0.2, 0.95, 0.25), 'green'),
            'cyan': screen_image('scr_cyan', (0.15, 0.7, 1.0), 'cyan')}
scr_m, scr_e = img_emis_mat('Screen', scr_imgs['red' if CFG['led'] == 'red' else 'green'], 3.0)
scr_tex = [n for n in scr_m.node_tree.nodes if n.type == 'TEX_IMAGE'][0]
screen = plane('Screen', 0.17, 0.106, (0, -0.126, 1.22), scr_m, rot=(math.pi / 2, 0, 0), parent=pil)
logo = plane('Logo', 0.12, 0.12, (0, -0.126, 0.98), decal_mat('LogoM', logo_image('logo', 256), rough=0.2, emis=0.6),
             rot=(math.pi / 2, 0, 0), parent=pil)
# holster on +X side
holster = box('Holster', (0.05, 0.12, 0.14), (0.175, 0.0, 1.00), blackgl, parent=pil, bevel=0.015)
cable_exit_local = Vector((0.165, 0.03, 0.62))
box('CableGland', (0.04, 0.06, 0.06), (0.165, 0.03, 0.62), blackgl, parent=pil, bevel=0.01)
# pillar LED light helper (point light for clean illumination)
ledL = bpy.data.lights.new('LEDLight', 'POINT'); ledL.energy = 12; ledL.color = led_col; ledL.shadow_soft_size = 0.3
ledLo = bpy.data.objects.new('LEDLight', ledL); link(ledLo); ledLo.location = PIL + Vector((0, -0.35, 0.8))

# ---------------------------------------------------------------- car
L2 = 2.05
top_f = pchip([-2.05, -1.97, -1.85, -1.55, -0.6, 0.05, 0.30, 0.70, 1.00, 1.35, 1.75, 2.05],
              [1.10, 1.36, 1.45, 1.51, 1.53, 1.50, 1.42, 1.18, 1.00, 0.94, 0.86, 0.76])
belt_f = pchip([-2.05, -1.0, 0.8, 2.05], [1.03, 1.00, 0.96, 0.90])
bot_f = pchip([-2.05, -1.80, -1.0, 1.0, 1.85, 2.05], [0.32, 0.22, 0.18, 0.18, 0.22, 0.32])
wid_f = pchip([-2.05, -1.6, -1.2, 1.1, 1.6, 2.05], [0.86, 0.895, 0.90, 0.90, 0.88, 0.80])
AXLE = 1.30; WR = 0.335; ARCH_R = 0.405; ARCH_C = 0.33

def car_bottom(y):
    b = bot_f(y)
    for ya in (-AXLE, AXLE):
        dy = abs(y - ya)
        if dy < ARCH_R:
            b = max(b, ARCH_C + math.sqrt(ARCH_R ** 2 - dy ** 2))
    return b

def end_shrink(y):
    if y >= 0:
        d = min(1.0, y / L2); p = 4.5
    else:
        d = min(1.0, -y / L2); p = 7.0
    return max(0.0, 1 - d ** p) ** (1 / p)

def car_ring(y, per=5):
    t = top_f(y); b = car_bottom(y); W = wid_f(y)
    zb = min(belt_f(y), t - 0.05)
    gh = smoothstep(0.08, 0.35, t - zb)        # greenhouse amount
    Wr = lerp(W * 0.93, 0.66, gh)
    s = end_shrink(y)
    half = [(0.0, b), (W * 0.80, b), (W * 0.975, b + 0.05), (W, b + 0.40 * (zb - b)),
            (W * 0.99, zb - 0.06), (W * 0.965, zb + 0.005),
            (lerp(W * 0.96, Wr * 1.03, 0.85 if gh > 0 else 1.0), t - 0.07 * gh - 0.03 * (1 - gh)),
            (Wr * 0.75, t - 0.006), ]
    ctrl = [Vector((x, z)) for x, z in half] + [Vector((0.0, t))] + [Vector((-x, z)) for x, z in reversed(half[1:])]
    pts = catmull_closed(ctrl, per)
    zc = (b + t) / 2
    out = []
    for p in pts:
        out.append(Vector((p.x * s, y, zc + (p.y - zc) * s)))
    return out, zb, t, gh

def build_car(name, paint_color, loc, rot_z, port_open=0.0, lights=True):
    root = bpy.data.objects.new(name, None); link(root)
    paint = new_mat(name + '_Paint', paint_color, rough=0.32, metal=0.75, coat=1.0, coat_rough=0.02)
    # metallic flake
    nt = paint.node_tree; b = nt.nodes['Principled BSDF']
    vn = nt.nodes.new('ShaderNodeTexVoronoi'); vn.inputs['Scale'].default_value = 1500
    tcx = nt.nodes.new('ShaderNodeTexCoord'); nt.links.new(tcx.outputs['Object'], vn.inputs['Vector'])
    bmp = nt.nodes.new('ShaderNodeBump'); bmp.inputs['Strength'].default_value = 0.08
    nt.links.new(vn.outputs['Distance'], bmp.inputs['Height']); nt.links.new(bmp.outputs['Normal'], b.inputs['Normal'])
    glass = new_mat(name + '_Glass', (0.004, 0.005, 0.007), rough=0.02, spec=0.8, coat=1.0, coat_rough=0.0)
    roofm = new_mat(name + '_Roof', (0.005, 0.005, 0.006), rough=0.05, coat=1.0, coat_rough=0.0)
    plast = new_mat(name + '_Plastic', (0.012, 0.012, 0.013), rough=0.45)
    chrome = new_mat(name + '_Trim', (0.25, 0.26, 0.28), rough=0.15, metal=1.0)

    # ---- body surface
    N = 150
    ys = [-L2 * math.cos(math.pi * (0.012 + 0.976 * i / (N - 1))) for i in range(N)]
    rings = []; info = []
    for y in ys:
        r, zb, t, gh = car_ring(y)
        rings.append(r); info.append((zb, t, gh))
    body = loft(name + '_Body', rings, paint, cap=True, parent=root, subsurf=1, render_sub=2)
    me = body.data
    me.materials.append(glass); me.materials.append(roofm)
    nv = len(rings[0])
    mi = []
    for p in me.polygons:
        c = p.center; n = p.normal
        y = c.y
        zb = min(belt_f(y), top_f(y) - 0.05); t = top_f(y)
        idx = 0
        if (t - zb) > 0.18 and c.z > zb + 0.012:
            idx = 1
            if n.z > 0.80 and -1.75 < y < 0.15:
                idx = 2
            if y < -1.42 and n.y > -0.55 and n.z < 0.80:
                idx = 0   # C-pillar body colour
            if -0.62 < y < -0.50 and abs(n.x) > 0.6:
                idx = 2   # B pillar black
        elif c.z < 0.42 and (abs(y) > 1.75):
            idx = 3 if False else 0
        mi.append(idx)
    me.polygons.foreach_set('material_index', mi)
    me.update()
    depsgraph = bpy.context.evaluated_depsgraph_get()
    bvh = BVHTree.FromObject(body, depsgraph)

    def hit(origin, direction, off=0.003):
        loc_, nor, _, _ = bvh.ray_cast(Vector(origin), Vector(direction).normalized())
        if loc_ is None:
            return None, None
        return loc_ + nor * off, nor

    def strip(nm, origins, direction, radius, mat, off=0.004):
        pts = []
        for o in origins:
            h, _ = hit(o, direction, off)
            if h is not None: pts.append(h)
        if len(pts) > 2:
            return curve_obj(nm, pts, radius, mat, parent=root)

    # lights
    head_m, head_e = emis_mat(name + '_DRL', (0.85, 0.92, 1.0), 18.0 if lights else 0.0)
    tail_m, tail_e = emis_mat(name + '_Tail', (1.0, 0.02, 0.01), 14.0 if lights else 0.6)
    strip(name + '_DRL', [(x, 5, 0.74) for x in np.linspace(-0.74, 0.74, 25)], (0, -1, 0), 0.010, head_m)
    for sx in (-1, 1):  # headlight clusters (dark glass pods with bright cores)
        strip(name + '_HL', [(sx * x, 5, 0.69) for x in np.linspace(0.45, 0.70, 8)], (0, -1, 0), 0.022, blackgl)
    strip(name + '_Tail', [(x, -5, 1.05) for x in np.linspace(-0.80, 0.80, 27)], (0, 1, 0), 0.012, tail_m)
    strip(name + '_TailHouse', [(x, -5, 1.05) for x in np.linspace(-0.82, 0.82, 27)], (0, 1, 0), 0.026, roofm, off=0.0)
    strip(name + '_FrontIntake', [(x, 5, 0.40) for x in np.linspace(-0.6, 0.6, 15)], (0, -1, 0), 0.045, plast, off=-0.02)
    strip(name + '_RearDiff', [(x, -5, 0.36) for x in np.linspace(-0.7, 0.7, 15)], (0, 1, 0), 0.05, plast, off=-0.02)
    # side sills (black cladding) and belt trim
    for sx in (-1, 1):
        strip(name + '_Sill', [(sx * 3, y, 0.245) for y in np.linspace(-0.88, 0.88, 20)], (-sx, 0, 0), 0.035, plast, off=-0.012)
        strip(name + '_Belt', [(sx * 3, y, belt_f(y) + 0.012) for y in np.linspace(-1.36, 0.62, 30)], (-sx, 0, 0), 0.006, chrome)
        for yy, z0, z1 in [(0.62, 0.35, 0.99), (-0.56, 0.30, 1.0), (-1.30 + 0.43, 0.75, 0.99)]:
            strip(name + '_Seam', [(sx * 3, yy + 0.04 * (z - z0), z) for z in np.linspace(z0, z1, 10)], (-sx, 0, 0), 0.0025, plast, off=-0.001)
        for yy in (0.18, -0.95):
            strip(name + '_Handle', [(sx * 3, yy + d, 0.93) for d in np.linspace(-0.08, 0.08, 6)], (-sx, 0, 0), 0.008, chrome, off=0.002)
        # mirrors
        mo, mn = hit((sx * 3, 0.52, 1.03), (-sx, 0, 0), 0.0)
        if mo is not None:
            mir = loft(name + '_Mirror', [ellipse_ring(z, rx, ry, 16) for z, rx, ry in
                                          [(-0.05, 0.02, 0.02), (-0.045, 0.09, 0.05), (0.0, 0.11, 0.06), (0.045, 0.09, 0.05), (0.05, 0.02, 0.02)]],
                       roofm, parent=root, subsurf=1)
            mir.location = mo + Vector((sx * 0.08, -0.02, 0.02)); mir.rotation_euler = (0, 0, 0)
            box(name + '_MirStalk', (0.08, 0.03, 0.02), mo + Vector((sx * 0.035, 0, 0.0)), plast, parent=root)

    # under body filler (hidden dark mass between wheels)
    box(name + '_Under', (1.5, 3.6, 0.30), (0, 0, 0.32), plast, parent=root)
    # arch liners
    for sx in (-1, 1):
        for ya in (-AXLE, AXLE):
            lin = cylinder(name + '_Liner', ARCH_R - 0.005, 0.30, (sx * 0.70, ya, ARCH_C), plast, parent=root, n=32, axis='X')

    # ---- wheels
    tire_m = new_mat(name + '_Tire', (0.018, 0.018, 0.018), rough=0.55, sheen=0.3)
    rim_m = new_mat(name + '_Rim', (0.35, 0.36, 0.38), rough=0.22, metal=1.0)
    rimd_m = new_mat(name + '_RimDark', (0.03, 0.03, 0.035), rough=0.3, metal=0.6)
    disc_m = new_mat(name + '_Disc', (0.2, 0.2, 0.2), rough=0.35, metal=1.0)
    def wheel(px, py, side):
        w = bpy.data.objects.new(name + '_Wheel', None); link(w, root)
        w.location = (px, py, WR); w.rotation_euler = (0, 0, 0 if side > 0 else math.pi)
        prof = [(0.228, -0.09), (0.265, -0.105), (0.31, -0.104), (0.330, -0.088), (0.336, -0.03), (0.336, 0.03),
                (0.330, 0.088), (0.31, 0.104), (0.265, 0.105), (0.228, 0.09), (0.222, 0.0)]
        n = 64
        rings = []
        for k in range(n):
            a = 2 * math.pi * k / n
            rings.append([Vector((xo, r * math.cos(a), r * math.sin(a))) for r, xo in prof])
        verts = [v for r in rings for v in r]
        faces = []
        m = len(prof)
        for k in range(n):
            for j in range(m):
                a0 = k * m + j; a1 = k * m + (j + 1) % m; b0 = ((k + 1) % n) * m + j; b1 = ((k + 1) % n) * m + (j + 1) % m
                faces.append((a0, a1, b1, b0))
        t = mesh_obj(name + '_Tire', verts, faces, tire_m, parent=w, subsurf=1)
        # rim: barrel + face disc + spokes
        cylinder(name + '_Barrel', 0.226, 0.18, (0, 0, 0), rimd_m, parent=w, n=48, axis='X', caps=False)
        cylinder(name + '_BackPlate', 0.22, 0.01, (-0.06, 0, 0), plast, parent=w, n=32, axis='X')
        lip = bpy.data.objects.new(name + '_Lip', None)
        cyl = cylinder(name + '_Lip', 0.228, 0.02, (0.085, 0, 0), rim_m, parent=w, n=48, axis='X')
        cylinder(name + '_Disc', 0.165, 0.025, (0.0, 0, 0), disc_m, parent=w, n=40, axis='X')
        cylinder(name + '_Hub', 0.06, 0.05, (0.075, 0, 0), rim_m, parent=w, n=24, axis='X', bevel=0.01)
        for k in range(5):
            a = 2 * math.pi * k / 5
            sp = box(name + '_Spoke', (0.035, 0.16, 0.05), (0.075, 0, 0), rim_m, parent=w, bevel=0.012)
            sp.rotation_euler = (a, 0, 0)
            sp.location = (0.078, 0.135 * math.sin(-a) * -1 if False else 0, 0)
            d = Vector((0, 0.135, 0)); d.rotate(Euler((a, 0, 0)))
            sp.location = (0.075, d.y, d.z)
        return w
    for sx in (-1, 1):
        for ya in (-AXLE, AXLE):
            wheel(sx * 0.785, ya, sx)

    # ---- charge port (left rear quarter: local -X side)
    P, Nn = hit((-3, -1.62, 0.88), (1, 0, 0), 0.0)
    Nn = Nn.normalized()
    port_q = Nn.to_track_quat('Z', 'Y')   # local Z = outward normal
    port_m = port_q.to_matrix().to_4x4(); port_m.translation = P
    cutter = cylinder(name + '_PortCut', 0.062, 0.24, (0, 0, 0), plast, n=40, parent=root)
    cutter.matrix_world = port_m; cutter.hide_render = True; cutter.hide_viewport = True
    bm_ = body.modifiers.new('PortHole', 'BOOLEAN'); bm_.object = cutter; bm_.operation = 'DIFFERENCE'; bm_.solver = 'EXACT'
    port = bpy.data.objects.new(name + '_Port', None); link(port, root); port.matrix_world = port_m
    # recess cup
    cupv = []; cupf = []
    n = 40
    for k in range(n):
        a = 2 * math.pi * k / n
        cupv += [(0.063 * math.cos(a), 0.063 * math.sin(a), 0.004), (0.063 * math.cos(a), 0.063 * math.sin(a), -0.055)]
    for k in range(n):
        a, b2 = 2 * k, 2 * ((k + 1) % n)
        cupf.append((a, a + 1, b2 + 1, b2))
    cup = mesh_obj(name + '_Cup', cupv, cupf, plast, parent=port)
    sock_m = new_mat(name + '_Socket', (0.015, 0.015, 0.017), rough=0.35)
    face = cylinder(name + '_SockFace', 0.062, 0.01, (0, 0, -0.05), sock_m, parent=port, n=40)
    pin_m = new_mat(name + '_PinHole', (0.0, 0.0, 0.0), rough=0.9)
    for (hx, hy, hr) in [(0, 0.028, 0.008), (-0.024, 0.012, 0.008), (0.024, 0.012, 0.008), (-0.016, -0.014, 0.008),
                         (0.016, -0.014, 0.008), (-0.010, 0.0, 0.004), (0.010, 0.0, 0.004)]:
        cylinder(name + '_Pin', hr, 0.004, (hx, hy, -0.0445), pin_m, parent=port, n=16)
    ring_m, ring_e = emis_mat(name + '_PortLED', (0.1, 0.9, 1.0), 0.0)
    rv = []; rf = []
    for k in range(n):
        a = 2 * math.pi * k / n
        for (rr, zz) in [(0.050, -0.043), (0.058, -0.043), (0.058, -0.039), (0.050, -0.039)]:
            rv.append((rr * math.cos(a), rr * math.sin(a), zz))
    for k in range(n):
        for j in range(4):
            a0 = k * 4 + j; a1 = k * 4 + (j + 1) % 4; b0 = ((k + 1) % n) * 4 + j; b1 = ((k + 1) % n) * 4 + (j + 1) % 4
            rf.append((a0, a1, b1, b0))
    ring = mesh_obj(name + '_PortRing', rv, rf, ring_m, parent=port)
    # flap: hinge on the forward edge (local car +Y)
    hinge = bpy.data.objects.new(name + '_FlapHinge', None); link(hinge, port)
    fwd_local = port_m.to_3x3().inverted() @ Vector((0, -1, 0))
    hinge_axis_local = Vector((0, 0, 1)).cross(fwd_local).normalized()
    hinge.location = fwd_local.normalized() * 0.072 + Vector((0, 0, 0.004))
    flap_rings = []
    fl = mesh_obj(name + '_Flap', *flap_geom(), paint, parent=hinge, subsurf=1)
    fl.location = -fwd_local.normalized() * 0.072
    hinge.rotation_mode = 'AXIS_ANGLE'
    hinge.rotation_axis_angle = (0.0, *hinge_axis_local)
    return dict(root=root, body=body, port=port, port_local=port_m, hinge=hinge, ring_e=ring_e,
                head_e=head_e, tail_e=tail_e, paint=paint)

def flap_geom():
    # rounded disc slightly domed, ~0.13 diameter
    n = 40; v = []; f = []
    for ri, (r, z) in enumerate([(0.0, 0.006), (0.035, 0.0058), (0.062, 0.004), (0.068, 0.0), (0.068, -0.004), (0.062, -0.005)]):
        for k in range(n):
            a = 2 * math.pi * k / n
            v.append((r * math.cos(a), r * math.sin(a), z))
    rings = 6
    for i in range(rings - 1):
        for k in range(n):
            f.append((i * n + k, i * n + (k + 1) % n, (i + 1) * n + (k + 1) % n, (i + 1) * n + k))
    f.append(tuple(range((rings - 1) * n, rings * n))[::-1])
    return v, f

def set_flap(car, amount, frame=None):
    h = car['hinge']; ax = h.rotation_axis_angle
    h.rotation_axis_angle = (amount * math.radians(100), ax[1], ax[2], ax[3])
    if frame is not None: h.keyframe_insert('rotation_axis_angle', frame=frame)

CAR_LOC = Vector((0.0, 2.75, 0.0)); CAR_ROT = math.pi   # nose toward -Y, rear toward pillar
cars = {}
paint_cols = {'blue': (0.02, 0.075, 0.32), 'grey': (0.16, 0.17, 0.18), 'white': (0.75, 0.76, 0.78), 'black': (0.01, 0.01, 0.012)}
main = None
if CFG['car']:
    main = build_car('Car', paint_cols[CFG['car']], CAR_LOC, CAR_ROT, lights=True)
    main['root'].location = CAR_LOC; main['root'].rotation_euler = (0, 0, CAR_ROT)
# neighbour cars (background realism)
if SHOT in ('shot1', 'shot3', 'shot2'):
    nb = build_car('CarN', paint_cols['white'], None, 0, lights=False)
    nb['root'].location = (-BAYW, 2.9, 0); nb['root'].rotation_euler = (0, 0, math.pi)
if SHOT in ('shot3', 'shot1'):
    nb2 = build_car('CarN2', paint_cols['black'], None, 0, lights=False)
    nb2['root'].location = (BAYW * 3, 2.7, 0); nb2['root'].rotation_euler = (0, 0, math.pi + 0.03)
bpy.context.view_layer.update()

def port_world(car):
    M = car['root'].matrix_world @ car['port_local']
    return M

# ---------------------------------------------------------------- connector + cable
conn_dark = new_mat('ConnDark', (0.02, 0.02, 0.022), rough=0.38)
conn_grey = new_mat('ConnGrey', (0.12, 0.12, 0.13), rough=0.3)
conn_green = new_mat('ConnGreen', tuple(c * 0.9 for c in GREEN), rough=0.3)
rubber = new_mat('CableRubber', (0.012, 0.012, 0.013), rough=0.5, sheen=0.2)
conn = bpy.data.objects.new('Connector', None); link(conn); conn.rotation_mode = 'QUATERNION'
# nozzle along local +Y
noz = cylinder('Nozzle', 0.045, 0.075, (0, 0.035, 0), conn_grey, parent=conn, n=40, axis='Y', bevel=0.006)
nface = cylinder('NozFace', 0.040, 0.004, (0, 0.0735, 0), conn_dark, parent=conn, n=32, axis='Y')
grn = cylinder('NozRing', 0.049, 0.012, (0, -0.004, 0), conn_green, parent=conn, n=40, axis='Y')
head = box('ConnHead', (0.105, 0.075, 0.11), (0, -0.045, 0.005), conn_dark, parent=conn, bevel=0.03, seg=4)
handle = limb('ConnHandle', 0.15, 0.03, 0.026, conn_dark, n=16, parent=conn)
handle.location = (0, -0.07, 0.02); handle.rotation_euler = (math.radians(-60), 0, 0)
boot = limb('ConnBoot', 0.06, 0.022, 0.018, rubber, n=16, parent=conn)
boot.location = Vector((0, -0.07, 0.02)) + Vector((0, -0.15 * math.sin(math.radians(60)), -0.15 * math.cos(math.radians(60))))
boot.rotation_euler = (math.radians(-60), 0, 0)
CONN_TAIL = Vector((0, -0.07, 0.02)) + Vector((0, -0.21 * math.sin(math.radians(60)), -0.21 * math.cos(math.radians(60))))
CONN_TAIL_DIR = Vector((0, -math.sin(math.radians(60)), -math.cos(math.radians(60))))
GRIP = Vector((0, -0.12, -0.02))

NCP = 26
cable = curve_obj('Cable', [Vector((0, 0, k * 0.1)) for k in range(NCP)], 0.016, rubber, order=4, res=4)
cable_len = 3.4

def holster_matrix():
    # nozzle pointing -X into holster on pillar +X side
    q = Quaternion((0, 0, 1), math.pi / 2)  # local +Y -> world -X
    M = q.to_matrix().to_4x4()
    M.translation = PIL + Vector((0.20 + 0.07, 0.0, 1.0))
    return M

def cable_points(p0, d0, p1, d1, L):
    """hanging cable from p0 (leaving along d0) to p1 (leaving along d1, pointing away from p1 into cable)."""
    p0 = Vector(p0); p1 = Vector(p1)
    a = p0 + d0 * 0.12; b = p1 + d1 * 0.12
    chord = (b - a).length
    rem = max(0.05, L - 0.24)
    horiz = Vector((b.x - a.x, b.y - a.y, 0))
    side = Vector((0, 0, 1)).cross(horiz.normalized()) if horiz.length > 1e-3 else Vector((1, 0, 0))
    def build(sag):
        pts = []
        for k in range(NCP - 4):
            t = k / (NCP - 5)
            p = a.lerp(b, t)
            p.z -= sag * 4 * t * (1 - t)
            pts.append(p)
        return pts
    lo, hi = 0.0, 6.0
    for _ in range(30):
        mid = (lo + hi) / 2
        pts = build(mid)
        ln = sum((pts[i + 1] - pts[i]).length for i in range(len(pts) - 1))
        if ln < rem: lo = mid
        else: hi = mid
    pts = build(lo)
    out = [p0, p0 + d0 * 0.06]
    for k, p in enumerate(pts):
        t = k / (len(pts) - 1)
        z0 = p.z
        p.z = max(0.018, p.z)
        if z0 < 0.018:   # lying on ground: spread sideways a bit
            p += side * min(0.5, (0.018 - z0) * 0.35) * math.sin(math.pi * t)
        out.append(p)
    out += [p1 + d1 * 0.06, p1]
    return out

def set_cable(pts, frame=None):
    sp = cable.data.splines[0]
    for p, v in zip(sp.points, pts):
        p.co = (v.x, v.y, v.z, 1)
        if frame is not None: p.keyframe_insert('co', frame=frame)

# ---------------------------------------------------------------- person
jacket = new_mat('Jacket', (0.018, 0.02, 0.026), rough=0.55, sheen=0.4, coat=0.3, coat_rough=0.25)
jeans = new_mat('Jeans', (0.025, 0.04, 0.075), rough=0.75, sheen=0.5)
skin = new_mat('Skin', (0.42, 0.26, 0.19), rough=0.5)
skin.node_tree.nodes['Principled BSDF'].inputs['Subsurface Weight'].default_value = 0.2
hair = new_mat('Hair', (0.012, 0.009, 0.008), rough=0.45, sheen=0.5)
glove = new_mat('Glove', (0.015, 0.015, 0.017), rough=0.5, sheen=0.4)
shoe = new_mat('Shoe', (0.03, 0.03, 0.032), rough=0.4)
sole = new_mat('Sole', (0.5, 0.5, 0.5), rough=0.6)

PARTS = {}
def make_person():
    P = PARTS
    # pelvis/jeans hips
    P['pelvis'] = loft('P_pelvis', [ellipse_ring(z, rx, ry, 24, p=2.4) for z, rx, ry in
                                     [(0.10, 0.10, 0.07), (0.06, 0.165, 0.105), (0.0, 0.175, 0.11), (-0.06, 0.17, 0.11), (-0.11, 0.13, 0.10), (-0.14, 0.05, 0.05)]], jeans, subsurf=1)
    P['torso'] = loft('P_torso', [ellipse_ring(z, rx, ry, 28, p=2.3, cy=cy) for z, rx, ry, cy in
                                   [(-0.10, 0.17, 0.115, 0.0), (-0.03, 0.182, 0.122, 0.0), (0.12, 0.17, 0.118, 0.005), (0.30, 0.19, 0.128, 0.01),
                                    (0.42, 0.205, 0.12, 0.0), (0.48, 0.17, 0.10, -0.005), (0.53, 0.08, 0.075, 0.0), (0.58, 0.07, 0.07, 0.0), (0.60, 0.04, 0.04, 0.0)]],
                      jacket, subsurf=1, render_sub=2)
    # collar
    P['collar'] = loft('P_collar', [ellipse_ring(z, rx, ry, 24) for z, rx, ry in
                                     [(0.50, 0.095, 0.085), (0.56, 0.085, 0.08), (0.60, 0.083, 0.078), (0.605, 0.07, 0.065)]], jacket, cap=False, subsurf=1)
    P['collar'].data.polygons.foreach_set('use_smooth', [True] * len(P['collar'].data.polygons))
    hd = [ellipse_ring(z, rx, ry, 24, cy=cy) for z, rx, ry, cy in
          [(0.0, 0.045, 0.045, 0.0), (0.04, 0.05, 0.05, 0.0), (0.07, 0.068, 0.08, 0.015), (0.12, 0.076, 0.096, 0.012),
           (0.17, 0.078, 0.10, 0.0), (0.21, 0.068, 0.088, -0.005), (0.235, 0.04, 0.05, -0.005), (0.245, 0.01, 0.01, 0.0)]]
    P['head'] = loft('P_head', hd, skin, subsurf=2)
    hr = [ellipse_ring(z, rx, ry, 24, cy=cy) for z, rx, ry, cy in
          [(0.10, 0.075, 0.09, -0.02), (0.15, 0.082, 0.103, -0.008), (0.20, 0.075, 0.095, -0.008), (0.235, 0.05, 0.062, -0.008), (0.252, 0.012, 0.012, -0.01)]]
    P['hair'] = loft('P_hair', hr, hair, subsurf=2)
    for s, nm in ((1, 'R'), (-1, 'L')):
        P['uarm' + nm] = limb('P_uarm' + nm, 0.29, 0.058, 0.048, jacket)
        P['farm' + nm] = limb('P_farm' + nm, 0.25, 0.049, 0.043, jacket)
        P['hand' + nm] = loft('P_hand' + nm, [ellipse_ring(z, rx, ry, 16, cy=cy) for z, rx, ry, cy in
                                              [(0.01, 0.025, 0.02, 0), (-0.02, 0.032, 0.02, 0.0), (-0.08, 0.045, 0.022, 0.004), (-0.13, 0.043, 0.02, 0.012),
                                               (-0.17, 0.035, 0.016, 0.022), (-0.185, 0.015, 0.01, 0.026)]], glove, subsurf=1)
        th = limb('P_thumb' + nm, 0.06, 0.013, 0.011, glove, n=10); th.parent = P['hand' + nm]
        th.location = (s * -0.03 * 0 + 0.0, 0.02, -0.04); th.rotation_euler = (math.radians(35), s * math.radians(25), 0)
        P['thigh' + nm] = limb('P_thigh' + nm, 0.44, 0.082, 0.058, jeans)
        P['shin' + nm] = limb('P_shin' + nm, 0.39, 0.056, 0.045, jeans)
        ft = loft('P_foot' + nm, [ellipse_ring(z, rx, ry, 16, cy=cy) for z, rx, ry, cy in
                                   [(-0.075, 0.040, 0.12, 0.05), (-0.06, 0.048, 0.135, 0.055), (-0.02, 0.046, 0.125, 0.05), (0.02, 0.040, 0.08, 0.02), (0.035, 0.03, 0.04, 0.0)]], shoe, subsurf=1)
        P['foot' + nm] = ft
    for o in P.values():
        o.rotation_mode = 'QUATERNION'

def T(x, y, z): return Matrix.Translation((x, y, z))
def R(rx, ry, rz): return Euler((rx, ry, rz), 'XYZ').to_matrix().to_4x4()
D = math.radians

UA, FA = 0.29, 0.25

def frame_from_axis(down, fwd):
    z = -down.normalized()
    y = (fwd - z * fwd.dot(z)).normalized()
    x = y.cross(z)
    M = Matrix((x, y, z)).transposed()
    return M.to_4x4()

def solve_arm(S, Tgt, pole):
    d = (Tgt - S); dist = d.length
    dist_c = min(dist, UA + FA - 1e-3); dirv = d.normalized()
    cosA = (UA * UA + dist_c * dist_c - FA * FA) / (2 * UA * dist_c)
    cosA = max(-1, min(1, cosA)); sinA = math.sqrt(1 - cosA * cosA)
    pn = (pole - dirv * pole.dot(dirv))
    pn = pn.normalized() if pn.length > 1e-6 else Vector((0, 0, -1))
    E = S + dirv * UA * cosA + pn * UA * sinA
    W = S + dirv * dist_c
    return E, W

def person_pose(st):
    """st: dict root(x,y), yaw, phase, amt, lean, rhand: None or (wrist_target, hand_matrix_rot), bob."""
    out = {}
    ph = st['phase']; amt = st['amt']
    Mroot = T(st['x'], st['y'], 0) @ Matrix.Rotation(st['yaw'], 4, 'Z')
    bob = 0.022 * amt * (abs(math.cos(ph)) - 0.6) + st.get('crouch', 0.0)
    pel_yaw = D(6) * amt * math.sin(ph)
    breath = 0.004 * math.sin(st.get('t', 0) * 1.6)
    lean = st.get('lean', 0.0) + D(4) * amt
    Mpel = Mroot @ T(0, 0, 0.95 + bob) @ R(0, 0, pel_yaw)
    out['pelvis'] = Mpel
    Mtor = Mpel @ R(lean, 0, -pel_yaw * 1.6 + st.get('twist', 0.0))
    out['torso'] = Mtor @ T(0, 0, breath)
    out['collar'] = out['torso']
    Mneck = Mtor @ T(0, 0, 0.555) @ R(st.get('head_pitch', 0.0) - lean * 0.5, 0, st.get('head_yaw', 0.0))
    out['head'] = Mneck; out['hair'] = Mneck
    knees = {}
    for s, nm, off in ((1, 'R', math.pi), (-1, 'L', 0.0)):
        p = ph + off
        hip = D(24) * amt * math.sin(p) - st.get('hipflex', 0.0)
        knee = -amt * (D(6) + D(52) * max(0.0, math.cos(p - 0.35)) ** 1.6) - st.get('kneeflex', 0.0)
        ank = -(hip + knee) * 0.9 - D(10) * amt * max(0.0, -math.sin(p - 0.6))
        Mth = Mpel @ T(s * 0.095, 0, -0.045) @ R(hip, 0, s * D(-1.5))
        Msh = Mth @ T(0, 0, -0.44) @ R(knee, 0, 0)
        Mft = Msh @ T(0, 0, -0.39) @ R(ank, 0, s * D(-6))
        out['thigh' + nm] = Mth; out['shin' + nm] = Msh; out['foot' + nm] = Mft
        # arms
        sw = -D(20) * amt * math.sin(p)
        Msho = Mtor @ T(s * 0.195, 0, 0.44)
        Mua = Msho @ R(sw + st.get('arm_fwd', 0.0), s * D(-7), 0)
        Mfa = Mua @ T(0, 0, -UA) @ R(D(14) + D(10) * amt * max(0, math.sin(p)), 0, 0)
        Mh = Mfa @ T(0, 0, -FA) @ R(D(5), 0, s * D(10))
        out['uarm' + nm] = Mua; out['farm' + nm] = Mfa; out['hand' + nm] = Mh
    rh = st.get('rhand')
    if rh is not None:
        wrist_t, hand_rot, w = rh
        S = (out['torso'] @ T(0.195, 0, 0.44)).translation
        fkW = out['handR'].translation
        Tg = fkW.lerp(wrist_t, w)
        pole = (Mroot.to_3x3() @ Vector((0.6, -0.5, -0.6)))
        E, Wp = solve_arm(S, Tg, pole)
        fwd = Mroot.to_3x3() @ Vector((0, 1, 0))
        out['uarmR'] = Matrix.Translation(S) @ frame_from_axis(E - S, fwd)
        out['farmR'] = Matrix.Translation(E) @ frame_from_axis(Wp - E, fwd)
        fk_rot = out['farmR'].to_quaternion() @ Quaternion((1, 0, 0), D(5))
        q = fk_rot.slerp(hand_rot, w)
        out['handR'] = Matrix.Translation(Wp) @ q.to_matrix().to_4x4()
    return out

_prevq = {}
def apply_pose(pose, frame):
    for k, M in pose.items():
        o = PARTS[k]
        loc, q, _ = M.decompose()
        pq = _prevq.get(k)
        if pq is not None and pq.dot(q) < 0: q.negate()
        _prevq[k] = q.copy()
        o.location = loc; o.rotation_quaternion = q
        if frame is not None:
            o.keyframe_insert('location', frame=frame); o.keyframe_insert('rotation_quaternion', frame=frame)

# hand rotation that grips connector (hand -Z along connector +Y, palm down onto handle)
def grip_from_conn(Mc):
    rot = Mc.to_3x3()
    fwd = rot @ Vector((0, 1, 0)); up = rot @ Vector((0, 0, 1))
    Mh = frame_from_axis(fwd, -up)
    hd = (rot @ Vector((0, 0.866, 0.5))).normalized()
    hn = (rot @ Vector((0, -0.5, 0.866))).normalized()
    Mh = frame_from_axis(hd, -hn)
    wrist = Mc @ Vector((0.0, -0.24, -0.026))
    return wrist, Mh.to_quaternion()

# ---------------------------------------------------------------- rain
def add_rain(center, size, count, zmax=4.5):
    if args.norain: return
    rm, _ = emis_mat('RainE', (0.75, 0.8, 0.9), 0.35, False)
    drop = cylinder('Drop', 0.0016, 0.02, (0, 0, -50), rm, n=6, smooth=False, axis='X')
    noshadow(drop)
    try: drop.visible_diffuse = False; drop.visible_glossy = False
    except Exception: pass
    emitter = plane('RainEmit', size[0], size[1], (center[0], center[1], zmax), None, uv=False)
    emitter.hide_render = True
    ps = emitter.modifiers.new('rain', 'PARTICLE_SYSTEM').particle_system
    s = ps.settings
    s.count = count; s.frame_start = -60; s.frame_end = NF + 5; s.lifetime = 60
    s.emit_from = 'FACE'; s.normal_factor = 0.0; s.object_align_factor = (0, 0, -7.5)
    s.effector_weights.gravity = 0.0
    s.render_type = 'OBJECT'; s.instance_object = drop; s.particle_size = 1.0; s.size_random = 0.4
    s.use_rotations = True; s.rotation_mode = 'VEL'
    try: s.rotation_mode = 'VEL'
    except Exception: pass
    s.physics_type = 'NEWTON'
    return emitter

# ================================================================ SHOT SETUP
cam_d = bpy.data.cameras.new('Cam'); cam = bpy.data.objects.new('Cam', cam_d); link(cam)
scene.camera = cam; cam.rotation_mode = 'QUATERNION'
cam_d.dof.use_dof = True; cam_d.dof.aperture_blades = 7; cam_d.dof.aperture_rotation = D(10)
cam_d.sensor_width = 36
focus = bpy.data.objects.new('Focus', None); link(focus); cam_d.dof.focus_object = focus

def key_cam(f, loc, tgt, focus_pt, lens=None):
    cam.location = loc; cam.rotation_quaternion = look_quat(loc, tgt)
    cam.keyframe_insert('location', frame=f); cam.keyframe_insert('rotation_quaternion', frame=f)
    focus.location = focus_pt; focus.keyframe_insert('location', frame=f)
    if lens:
        cam_d.lens = lens; cam_d.keyframe_insert('lens', frame=f)

def ease(t): t = max(0, min(1, t)); return t * t * (3 - 2 * t)

def key_led(f, color, strength):
    strength *= 0.45
    led_e.inputs['Color'].default_value = (*color, 1); led_e.inputs['Strength'].default_value = strength
    led_e.inputs['Color'].keyframe_insert('default_value', frame=f); led_e.inputs['Strength'].keyframe_insert('default_value', frame=f)
    ledL.color = color; ledL.energy = strength * 0.6
    ledL.keyframe_insert('color', frame=f); ledL.keyframe_insert('energy', frame=f)

CYAN = (0.08, 0.55, 1.0)
GLED = (0.12, 1.0, 0.18)

if SHOT == 'shot1':
    # grey car plugged in; red LED
    set_flap(main, 1.0)
    bpy.context.view_layer.update()
    PM = port_world(main)
    Mc = PM @ Matrix.Rotation(D(-90), 4, 'X')        # connector +Y -> port inward(-Z)
    Mc = PM @ Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))
    # orient: conn +Y = -port Z (into body), conn +Z = world up-ish
    nrm = (PM.to_3x3() @ Vector((0, 0, 1))).normalized()
    Mc = frame_from_axis(-Vector((0, 0, 1)), nrm)  # placeholder
    def conn_into_port(PM, depth):
        nrm = (PM.to_3x3() @ Vector((0, 0, 1))).normalized()
        yax = -nrm; zax = Vector((0, 0, 1)); zax = (zax - yax * zax.dot(yax)).normalized(); xax = yax.cross(zax)
        M = Matrix((xax, yax, zax)).transposed().to_4x4()
        M.translation = PM.translation + nrm * (0.0735 - depth)
        return M
    Mc = conn_into_port(PM, 0.045)
    conn.matrix_world = Mc
    ex = pil.matrix_world @ cable_exit_local
    set_cable(cable_points(ex, Vector((1, 0, -0.3)).normalized(), Mc @ CONN_TAIL, (Mc.to_3x3() @ CONN_TAIL_DIR).normalized(), cable_len))
    main['ring_e'].inputs['Color'].default_value = (0.1, 0.9, 1.0, 1); main['ring_e'].inputs['Strength'].default_value = 6
    main['head_e'].inputs['Strength'].default_value = 0.0; main['tail_e'].inputs['Strength'].default_value = 1.5
    key_led(1, (1.0, 0.03, 0.02), 22)
    for f in range(1, NF + 1):
        t = (f - 1) / (NF - 1); e = t
        loc = Vector((3.35, 2.15, 1.25)).lerp(Vector((2.55, 3.55, 1.12)), e)
        key_cam(f, loc, Vector((0.80, 5.55, 0.98)).lerp(Vector((0.92, 5.55, 1.02)), e), PIL + Vector((0.0, -0.12, 1.0)), lens=35)
    cam_d.dof.aperture_fstop = 2.0
    add_rain((1.5, 4.0), (8, 8), 2500)

elif SHOT == 'shot2':
    for f in range(1, NF + 1):
        t = (f - 1) / (NF - 1)
        puls = 0.55 + 0.45 * (0.5 + 0.5 * math.sin(2 * math.pi * (t * 2.5 / 1.6)))
        key_led(f, GLED, 26 * puls)
        a = D(-62) + D(26) * ease(t) * 0.6 + D(26) * t * 0.4
        r = 3.4 - 0.6 * t
        c = PIL + Vector((0.0, 0.0, 0))
        loc = c + Vector((math.cos(a) * r, math.sin(a) * r, 0)) ; loc.z = 0.75 + 0.08 * t
        key_cam(f, loc, PIL + Vector((-0.25, -0.6, 0.72)), PIL + Vector((0, -0.12, 0.95)), lens=32)
    cam_d.dof.aperture_fstop = 2.2
    set_cable(cable_points(pil.matrix_world @ cable_exit_local, Vector((1, 0, -0.3)).normalized(),
                           holster_matrix() @ CONN_TAIL, (holster_matrix().to_3x3() @ CONN_TAIL_DIR).normalized(), cable_len))
    conn.matrix_world = holster_matrix()
    add_rain((0.5, 4.0), (8, 8), 2500)

elif SHOT in ('shot3', 'shot4'):
    make_person()
    main['head_e'].inputs['Strength'].default_value = 6.0
    main['tail_e'].inputs['Strength'].default_value = 6.0
    bpy.context.view_layer.update()
    PM0 = port_world(main)

    def conn_port_matrix(depth):
        set_flap(main, 1.0); bpy.context.view_layer.update()
        PM = port_world(main)
        nrm = (PM.to_3x3() @ Vector((0, 0, 1))).normalized()
        yax = -nrm; zax = Vector((0, 0, 1)); zax = (zax - yax * zax.dot(yax)).normalized(); xax = yax.cross(zax)
        M = Matrix((xax, yax, zax)).transposed().to_4x4()
        M.translation = PM.translation + nrm * (0.0735 - depth)
        return M, nrm
    MP_in, NRM = conn_port_matrix(0.045)
    def at_depth(d):
        M = MP_in.copy(); M.translation = MP_in.translation + NRM * (0.045 - d); return M
    HOL = holster_matrix()
    ex = pil.matrix_world @ cable_exit_local
    exdir = Vector((1, 0, -0.3)).normalized()

    def mlerp(A, B, t):
        la, qa, _ = A.decompose(); lb, qb, _ = B.decompose()
        if qa.dot(qb) < 0: qb.negate()
        M = qa.slerp(qb, t).to_matrix().to_4x4(); M.translation = la.lerp(lb, t); return M

    def carry_matrix(rootM):
        # held at right side, nozzle forward and slightly down
        M = rootM @ T(0.24, 0.28, 0.93) @ R(D(-15), 0, D(-8))
        return M

    if SHOT == 'shot3':
        A = Vector((1.47, 2.30)); B = Vector((1.70, 5.18)); C = Vector((1.38, 4.20))
        yawAB = math.atan2(-(B - A).x, (B - A).y)
        yaw_hol = math.atan2(-((PIL.xy + Vector((0.27, 0.0))) - B).x, ((PIL.xy + Vector((0.27, 0))) - B).y)
        yaw_hol = D(90)  # face -X
        yaw_port = D(90)
        dist_total = 0.0
        F_WALK0, F_WALK1 = 8, 88
        F_REACH0, F_GRAB, F_LIFT1 = 88, 100, 112
        F_W2_0, F_W2_1 = 110, 140
        F_INS0, F_PRE, F_CLICK = 140, 152, 160
        F_REL0, F_REL1 = 168, 180
        prev_xy = A.copy(); dist = 0.0
        for f in range(1, NF + 1):
            st = dict(t=f / 30.0, amt=0.0, phase=0.0, lean=0.0)
            # ---- root path
            if f <= F_WALK0:
                xy = A; yaw = yawAB - D(25) * (1 - f / F_WALK0); amt = 0.0
            elif f <= F_WALK1:
                t = (f - F_WALK0) / (F_WALK1 - F_WALK0)
                te = t + 0.0
                # ease in/out on ends
                s = (t ** 2 * 2) if t < 0.1 else (t if t < 0.9 else t)
                tt = ease(t) * 0.25 + t * 0.75
                xy = A.lerp(B, tt); yaw = yawAB + (yaw_hol - yawAB) * smoothstep(0.88, 1.0, t)
                amt = min(1.0, smoothstep(0.0, 0.10, t) * (1 - smoothstep(0.90, 1.0, t)) * 1.0)
            elif f <= F_W2_0:
                xy = B; yaw = yaw_hol; amt = 0.0
            elif f <= F_W2_1:
                t = (f - F_W2_0) / (F_W2_1 - F_W2_0)
                xy = B.lerp(C, ease(t)); amt = math.sin(math.pi * t) * 0.85
                yaw = yaw_hol + D(85) * math.sin(math.pi * min(1, t * 1.3)) * (1 - smoothstep(0.6, 1.0, t)) if False else \
                    yaw_hol + (D(72) * smoothstep(0.0, 0.35, t)) * (1 - smoothstep(0.45, 1.0, t))
            else:
                xy = C; yaw = yaw_port; amt = 0.0
            dist += (xy - prev_xy).length; prev_xy = xy.copy()
            st.update(x=xy.x, y=xy.y, yaw=yaw, amt=amt, phase=2 * math.pi * dist / 1.45)
            rootM = T(xy.x, xy.y, 0) @ Matrix.Rotation(yaw, 4, 'Z')
            # ---- connector + hand
            w = 0.0; Mc = HOL
            if f < F_REACH0:
                Mc = HOL
            elif f <= F_GRAB:
                t = (f - F_REACH0) / (F_GRAB - F_REACH0); w = ease(t); Mc = HOL
                st['lean'] = D(10) * ease(t)
            elif f <= F_LIFT1:
                t = (f - F_GRAB) / (F_LIFT1 - F_GRAB)
                out = HOL.copy(); out.translation = HOL.translation + Vector((0.16, 0, 0.03))
                Mc = mlerp(HOL, out, ease(min(1, t * 2))) if t < 0.5 else mlerp(out, carry_matrix(rootM), ease((t - 0.5) * 2))
                w = 1.0; st['lean'] = D(10) * (1 - ease(t))
            elif f <= F_INS0:
                Mc = carry_matrix(rootM); w = 1.0
            elif f <= F_PRE:
                t = (f - F_INS0) / (F_PRE - F_INS0)
                Mc = mlerp(carry_matrix(rootM), at_depth(-0.07), ease(t)); w = 1.0
                st['lean'] = D(16) * ease(t); st['crouch'] = -0.05 * ease(t)
            elif f <= F_CLICK:
                t = (f - F_PRE) / (F_CLICK - F_PRE)
                Mc = at_depth(-0.07 + 0.115 * ease(t)); w = 1.0
                st['lean'] = D(16); st['crouch'] = -0.05
            else:
                Mc = at_depth(0.045)
                tr = smoothstep(F_REL0, F_REL1, f)
                w = 1.0 - tr; st['lean'] = D(16) * (1 - tr); st['crouch'] = -0.05 * (1 - tr)
            st['head_pitch'] = D(12) if f > F_REACH0 else D(4)
            wrist, hq = grip_from_conn(Mc)
            st['rhand'] = (wrist, hq, w)
            if f >= F_W2_0 and f <= F_W2_1:
                st['arm_fwd'] = 0
            pose = person_pose(st)
            apply_pose(pose, f)
            conn.matrix_world = Mc
            conn.keyframe_insert('location', frame=f); conn.keyframe_insert('rotation_quaternion', frame=f)
            set_cable(cable_points(ex, exdir, Mc @ CONN_TAIL, (Mc.to_3x3() @ CONN_TAIL_DIR).normalized(), cable_len), frame=f)
            # flap
            set_flap(main, ease((f - 118) / 14.0), frame=f)
            # LEDs
            if f < F_CLICK + 3:
                key_led(f, GLED, 24)
                main['ring_e'].inputs['Strength'].default_value = 0.0
            else:
                tt = (f - F_CLICK - 3) / 30.0
                puls = 0.55 + 0.45 * (0.5 + 0.5 * math.cos(2 * math.pi * tt / 1.2))
                key_led(f, CYAN, 30 * puls)
                main['ring_e'].inputs['Strength'].default_value = 8 * puls
            main['ring_e'].inputs['Strength'].keyframe_insert('default_value', frame=f)
            # camera
            t = (f - 1) / (NF - 1)
            loc = Vector((6.0, -1.5, 1.6)).lerp(Vector((5.45, -0.85, 1.5)), t)
            tgt = Vector((0.65, 3.4, 0.85)).lerp(Vector((0.7, 3.6, 0.85)), t)
            fp = pose['torso'].translation
            key_cam(f, loc, tgt, fp, lens=35)
        cam_d.dof.aperture_fstop = 2.8
        add_rain((2.0, 3.5), (9, 9), 3000)

    else:  # shot4 macro
        C = Vector((1.40, 4.32))
        for f in range(1, NF + 1):
            t = (f - 1)
            st = dict(t=f / 30.0, amt=0.0, phase=0.0, x=C.x, y=C.y, yaw=D(90), lean=D(16), crouch=-0.05, head_pitch=D(12))
            if f <= 30:
                u = (f - 1) / 29.0
                d = -0.30 + 0.345 * (1 - (1 - u) ** 2.0)
            else:
                d = 0.045 + 0.003 * math.exp(-(f - 30) / 3.0) * math.sin((f - 30) * 1.5)
            Mc = at_depth(d)
            # slight hand wobble
            if f < 30:
                Mc = Mc @ R(D(2.0) * (1 - f / 30) * math.sin(f * 0.4), 0, D(1.5) * (1 - f / 30))
            wrist, hq = grip_from_conn(Mc)
            st['rhand'] = (wrist, hq, 1.0)
            pose = person_pose(st)
            apply_pose(pose, f)
            conn.matrix_world = Mc
            conn.keyframe_insert('location', frame=f); conn.keyframe_insert('rotation_quaternion', frame=f)
            set_cable(cable_points(ex, exdir, Mc @ CONN_TAIL, (Mc.to_3x3() @ CONN_TAIL_DIR).normalized(), cable_len), frame=f)
            if f < 30:
                main['ring_e'].inputs['Strength'].default_value = 0.0; main['ring_e'].inputs['Color'].default_value = (0.1, 0.9, 1.0, 1)
            else:
                k = min(1.0, (f - 30) / 4.0)
                main['ring_e'].inputs['Strength'].default_value = 14 * k * (0.8 + 0.2 * math.cos((f - 30) / 30 * 2 * math.pi / 1.2))
                main['ring_e'].inputs['Color'].default_value = (0.1 + 0.0 * k, 0.95, 0.75 + 0.25 * min(1, (f - 30) / 20), 1)
            main['ring_e'].inputs['Strength'].keyframe_insert('default_value', frame=f)
            main['ring_e'].inputs['Color'].keyframe_insert('default_value', frame=f)
            key_led(f, GLED if f < 34 else CYAN, 24)
            # camera: grazing along the body side looking at port
            P0 = MP_in.translation
            u = (f - 1) / (NF - 1)
            loc = P0 + NRM * 0.52 + Vector((0.0, -0.44, 0.15)) + Vector((-0.03, 0.04, -0.01)) * u
            tgt = P0 + NRM * 0.07 + Vector((0, 0.01, -0.01))
            key_cam(f, loc, tgt, P0 + NRM * 0.03, lens=44)
        cam_d.dof.aperture_fstop = 3.2
        add_rain((1.2, 4.3), (3, 3), 900, zmax=2.2)
        kl = bpy.data.lights.new('MacroKey', 'AREA'); kl.energy = 6; kl.size = 0.6; kl.color = (0.75, 0.85, 1.0)
        klo = bpy.data.objects.new('MacroKey', kl); link(klo); klo.location = P0 + NRM * 0.7 + Vector((0, -0.5, 0.8))
        klo.rotation_mode = 'QUATERNION'; klo.rotation_quaternion = look_quat(klo.location, P0)
        set_flap(main, 1.0)
    # pose test: ensure hand hidden parts ok
else:
    pass

if args.camcheck:
    v = [float(x) for x in args.camcheck.split(',')]
    cam.animation_data_clear(); focus.animation_data_clear()
    try: cam_d.animation_data_clear()
    except Exception: pass
    cam.location = v[:3]; cam.rotation_quaternion = look_quat(v[:3], v[3:6]); focus.location = v[3:6]
    cam_d.lens = v[6] if len(v) > 6 else 40
# ---------------------------------------------------------------- render settings
rx, ry = [int(v) for v in args.res.split('x')]
r = scene.render
r.engine = 'CYCLES'; r.resolution_x = rx; r.resolution_y = ry; r.resolution_percentage = 100
r.fps = 30
scene.frame_start = 1; scene.frame_end = NF
c = scene.cycles
c.device = 'CPU'; c.samples = args.spp; c.use_adaptive_sampling = True; c.adaptive_threshold = 0.02
c.use_denoising = True; c.denoiser = 'OPENIMAGEDENOISE'
try: c.denoising_input_passes = 'RGB_ALBEDO_NORMAL'
except Exception: pass
try: c.denoising_prefilter = 'ACCURATE'
except Exception: pass
c.max_bounces = 4; c.diffuse_bounces = 1; c.glossy_bounces = 2; c.transmission_bounces = 2; c.transparent_max_bounces = 6
c.volume_bounces = 0; c.caustics_reflective = False; c.caustics_refractive = False
c.blur_glossy = 1.0; c.sample_clamp_indirect = 8.0
try: c.use_light_tree = True
except Exception: pass
r.use_motion_blur = not args.nomb; r.motion_blur_shutter = 0.5
r.use_persistent_data = True
scene.view_settings.view_transform = 'AgX'
scene.view_settings.look = 'AgX - Medium High Contrast'
scene.view_settings.exposure = {'shot1': 0.6, 'shot2': 0.9, 'shot3': 0.9, 'shot4': 0.7}[SHOT]
r.image_settings.file_format = 'PNG'; r.image_settings.color_mode = 'RGB'; r.image_settings.color_depth = '8'
r.threads_mode = 'FIXED'; r.threads = args.threads
try: r.film_transparent = False
except Exception: pass

if args.blend:
    bpy.ops.wm.save_as_mainfile(filepath=args.blend)

out = args.out or os.path.join(BASE, 'raw', SHOT)
if args.frames:
    if '-' in args.frames:
        a, b = [int(v) for v in args.frames.split('-')]; frames = list(range(a, b + 1))
    else:
        frames = [int(v) for v in args.frames.split(',')]
else:
    frames = list(range(1, NF + 1))
os.makedirs(out, exist_ok=True)
t0 = time.time()
contiguous = frames == list(range(frames[0], frames[-1] + 1))
if contiguous and len(frames) > 1:
    scene.frame_start = frames[0]; scene.frame_end = frames[-1]
    r.filepath = os.path.join(out, '####')
    bpy.ops.render.render(animation=True)
else:
    for f in frames:
        scene.frame_set(f)
        r.filepath = os.path.join(out, '%04d.png' % f)
        bpy.ops.render.render(write_still=True)
print('RENDER_DONE %s %d frames %.1fs' % (SHOT, len(frames), time.time() - t0))
