#!/usr/bin/env python3
"""Generate a simple launcher icon PNG for VoidHunter"""
import struct, zlib, os

def make_png(w, h, pixels):
    def chunk(name, data):
        c = zlib.crc32(name + data) & 0xffffffff
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', c)
    raw = b''
    for row in pixels:
        raw += b'\x00' + bytes(row)
    compressed = zlib.compress(raw, 9)
    return (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', compressed)
            + chunk(b'IEND', b''))

size = 72
pixels = []
for y in range(size):
    row = []
    for x in range(size):
        cx, cy = x - size//2, y - size//2
        dist = (cx*cx + cy*cy) ** 0.5
        # Background: deep space blue
        r, g, b = 5, 10, 30
        # Orange glow circle
        if dist < size*0.42:
            t = max(0, 1 - dist / (size*0.42))
            r = int(r + (255-r)*t*0.3)
            g = int(g + (107-g)*t*0.3)
            b = int(b + (53-b)*t*0.2)
        # Ship body (triangle pointing up)
        if abs(cx) < size*0.15 and cy > -size*0.35 and cy < size*0.25:
            slope = abs(cx) / max(1, (-cy + size*0.25))
            if slope < 0.5:
                r, g, b = 34, 68, 170
        # Cockpit
        if cx*cx + (cy+size*0.1)*(cy+size*0.1) < (size*0.08)**2:
            r, g, b = 68, 170, 255
        # Wing tips orange
        if abs(abs(cx) - size*0.3) < 8 and abs(cy - size*0.1) < 8:
            r, g, b = 255, 107, 53
        # Thruster
        if abs(cx) < size*0.06 and cy > size*0.2 and cy < size*0.35:
            r, g, b = 255, 100, 0
        row.extend([r, g, b])
    pixels.append(row)

os.makedirs('app/src/main/res/mipmap-hdpi', exist_ok=True)
with open('app/src/main/res/mipmap-hdpi/ic_launcher.png', 'wb') as f:
    f.write(make_png(size, size, pixels))
print("Icon generated!")
