package com.voidhunter.gl;

public class Models {

    // ── Player ship: sleek fighter ──────────────────────────────────────────
    public static Mesh3D playerShip() {
        // x,y,z, nx,ny,nz, r,g,b,a
        float B = 0.18f, T = 0.06f; // body, thickness
        float[] v = {
            // Nose
             0,    0,  1.2f,   0, 0, 1,   0.3f,0.7f,1.0f,1,
            // Fuselage
            -0.12f, 0,  0.6f,  -1,0, 0,   0.2f,0.5f,0.9f,1,
             0.12f, 0,  0.6f,   1,0, 0,   0.2f,0.5f,0.9f,1,
             0,     T,  0.6f,   0,1, 0,   0.25f,0.6f,1.0f,1,
             0,    -T,  0.6f,   0,-1,0,   0.15f,0.4f,0.8f,1,
            // Wings
            -1.1f,  0, -0.3f,  -1,0,0,   0.15f,0.35f,0.75f,1,
             1.1f,  0, -0.3f,   1,0,0,   0.15f,0.35f,0.75f,1,
            -0.5f,  0,  0.2f,   0,0,1,   0.2f,0.5f,0.9f,1,
             0.5f,  0,  0.2f,   0,0,1,   0.2f,0.5f,0.9f,1,
            // Wing tips (orange glow)
            -1.2f,  0, -0.5f,  -1,0,0,   1.0f,0.45f,0.1f,1,
             1.2f,  0, -0.5f,   1,0,0,   1.0f,0.45f,0.1f,1,
            // Tail
            -0.18f, 0, -0.8f,  -1,0,0,   0.1f,0.3f,0.7f,1,
             0.18f, 0, -0.8f,   1,0,0,   0.1f,0.3f,0.7f,1,
             0,     0.3f,-0.6f, 0,1,0,   0.15f,0.4f,0.8f,1,  // dorsal fin
            // Engine glow
             0,     0, -1.0f,   0,0,-1,  1.0f,0.5f,0.0f,1,
        };
        short[] i = {
            // Nose to fuselage
            0,1,3,  0,3,2,  0,2,4,  0,4,1,
            // Fuselage body
            1,2,3,  1,4,2,
            // Left wing
            1,7,5,  7,9,5,  1,5,4,
            // Right wing
            2,6,8,  8,6,10, 2,4,6,
            // Tail
            11,14,13, 11,12,14,
            // Engine
            11,12,14, 12,13,14,
        };
        return new Mesh3D(v, i);
    }

    // ── Enemy type 0: Saucer ─────────────────────────────────────────────
    public static Mesh3D enemySaucer() {
        int segs = 12;
        float[] v = new float[(segs*2+2) * 10];
        short[] idx = new short[segs * 12];
        int vi = 0;
        // Center top
        set(v, vi++, 0, 0.18f, 0,  0,1,0,  0.8f,0.1f,0.1f,1);
        // Center bottom
        set(v, vi++, 0,-0.08f, 0,  0,-1,0, 0.6f,0.0f,0.0f,1);
        for (int s = 0; s < segs; s++) {
            float a = (float)(s * 2 * Math.PI / segs);
            float x = (float)Math.cos(a), z = (float)Math.sin(a);
            set(v, vi++, x*0.8f, 0.04f, z*0.8f, x,0.2f,z, 0.5f,0.05f,0.05f,1);
            set(v, vi++, x*0.6f,-0.04f, z*0.6f, x,-0.2f,z,0.4f,0.0f,0.0f,1);
        }
        int ii = 0;
        for (int s = 0; s < segs; s++) {
            int n = (s+1) % segs;
            short top=0, bot=1;
            short tr=(short)(2+s*2), tb=(short)(3+s*2);
            short nr=(short)(2+n*2), nb=(short)(3+n*2);
            idx[ii++]=top; idx[ii++]=tr; idx[ii++]=nr;
            idx[ii++]=bot; idx[ii++]=nb; idx[ii++]=tb;
            idx[ii++]=tr; idx[ii++]=tb; idx[ii++]=nr;
            idx[ii++]=tb; idx[ii++]=nb; idx[ii++]=nr;
        }
        return new Mesh3D(v, trim(idx, ii));
    }

    // ── Enemy type 1: Fighter ─────────────────────────────────────────────
    public static Mesh3D enemyFighter() {
        float[] v = {
             0,   0,  0.9f,  0,0,1,  0.6f,0.0f,0.6f,1,
            -0.6f,0,  0,    -1,0,0,  0.45f,0,0.45f,1,
             0.6f,0,  0,     1,0,0,  0.45f,0,0.45f,1,
             0,  0.1f,0,     0,1,0,  0.5f,0,0.5f,1,
             0, -0.1f,0,     0,-1,0, 0.35f,0,0.35f,1,
            -1.0f,0,-0.4f,  -1,0,0, 0.7f,0.1f,0.0f,1,
             1.0f,0,-0.4f,   1,0,0, 0.7f,0.1f,0.0f,1,
             0,   0, -0.7f,  0,0,-1, 1.0f,0.3f,0.0f,1,
        };
        short[] i = {
            0,1,3, 0,3,2, 0,2,4, 0,4,1,
            1,2,3, 1,4,2,
            1,5,4, 2,4,6,
            1,2,7,
        };
        return new Mesh3D(v, i);
    }

    // ── Enemy type 2: Gunship (box-like heavy) ────────────────────────────
    public static Mesh3D enemyGunship() {
        float w=0.5f, h=0.25f, d=0.7f;
        float[] v = {
            // Front face
            -w, h, d,  0,0,1,  0.0f,0.4f,0.3f,1,
             w, h, d,  0,0,1,  0.0f,0.4f,0.3f,1,
             w,-h, d,  0,0,1,  0.0f,0.3f,0.2f,1,
            -w,-h, d,  0,0,1,  0.0f,0.3f,0.2f,1,
            // Back
            -w, h,-d,  0,0,-1, 0.0f,0.2f,0.15f,1,
             w, h,-d,  0,0,-1, 0.0f,0.2f,0.15f,1,
             w,-h,-d,  0,0,-1, 0.0f,0.15f,0.1f,1,
            -w,-h,-d,  0,0,-1, 0.0f,0.15f,0.1f,1,
            // Wings
            -1.2f,0,0.1f, -1,0,0, 0.0f,0.5f,0.35f,1,
             1.2f,0,0.1f,  1,0,0, 0.0f,0.5f,0.35f,1,
            -1.2f,0,-0.3f,-1,0,0, 0.0f,0.3f,0.2f,1,
             1.2f,0,-0.3f, 1,0,0, 0.0f,0.3f,0.2f,1,
            // Cannon tips (bright)
            -0.3f,0, d+0.3f, 0,0,1, 0.0f,1.0f,0.7f,1,
             0.3f,0, d+0.3f, 0,0,1, 0.0f,1.0f,0.7f,1,
        };
        short[] i = {
            0,1,2, 0,2,3,       // front
            5,4,7, 5,7,6,       // back
            4,0,3, 4,3,7,       // left
            1,5,6, 1,6,2,       // right
            4,5,1, 4,1,0,       // top
            3,2,6, 3,6,7,       // bottom
            0,8,10, 0,10,4,     // left wing
            1,9,5,  1,11,9,     // right wing
            0,12,1, 1,12,13,    // cannons
        };
        return new Mesh3D(v, i);
    }

    // ── Bullet ────────────────────────────────────────────────────────────
    public static Mesh3D bullet(boolean player) {
        float r = player ? 0.06f : 0.09f;
        float l = player ? 0.35f : 0.2f;
        float[] rc = player ? new float[]{0.3f,1.0f,1.0f,1} : new float[]{1.0f,0.3f,0.1f,1};
        float[] v = {
             0,   0,  l,  0,0,1,  rc[0],rc[1],rc[2],rc[3],
            -r,   0,  0,  -1,0,0, rc[0]*0.7f,rc[1]*0.7f,rc[2]*0.7f,1,
             r,   0,  0,   1,0,0, rc[0]*0.7f,rc[1]*0.7f,rc[2]*0.7f,1,
             0,   r,  0,   0,1,0, rc[0]*0.8f,rc[1]*0.8f,rc[2]*0.8f,1,
             0,  -r,  0,   0,-1,0,rc[0]*0.6f,rc[1]*0.6f,rc[2]*0.6f,1,
             0,   0, -l*0.3f, 0,0,-1, rc[0]*0.5f,rc[1]*0.5f,rc[2]*0.5f,1,
        };
        short[] i = {0,1,3, 0,3,2, 0,2,4, 0,4,1, 5,3,1, 5,2,3, 5,1,4, 5,4,2};
        return new Mesh3D(v, i);
    }

    // ── Missile ───────────────────────────────────────────────────────────
    public static Mesh3D missile() {
        float[] v = {
             0,    0,  0.5f,  0,0,1,  1.0f,0.6f,0.1f,1,
            -0.06f,0,  0,    -1,0,0,  0.8f,0.4f,0.0f,1,
             0.06f,0,  0,     1,0,0,  0.8f,0.4f,0.0f,1,
             0, 0.06f, 0,     0,1,0,  0.9f,0.5f,0.05f,1,
             0,-0.06f, 0,     0,-1,0, 0.7f,0.3f,0.0f,1,
             0,    0, -0.3f,  0,0,-1, 1.0f,0.8f,0.2f,1,
        };
        short[] i = {0,1,3, 0,3,2, 0,2,4, 0,4,1, 5,3,1, 5,2,3, 5,4,2, 5,1,4};
        return new Mesh3D(v, i);
    }

    // ── Star (point sprite placeholder) ──────────────────────────────────
    public static Mesh3D starField(int count) {
        java.util.Random r = new java.util.Random(42);
        float[] v = new float[count * 10];
        short[] idx = new short[count * 3];
        for (int s = 0; s < count; s++) {
            float x = (r.nextFloat()-0.5f)*80;
            float y = (r.nextFloat()-0.5f)*40;
            float z = (r.nextFloat()-0.5f)*80;
            float bright = 0.5f + r.nextFloat()*0.5f;
            set(v, s, x, y, z, 0,0,1, bright,bright,bright+0.1f,1);
            idx[s*3]=(short)s; idx[s*3+1]=(short)s; idx[s*3+2]=(short)s;
        }
        return new Mesh3D(v, idx);
    }

    // Helpers
    private static void set(float[] v, int i, float x, float y, float z,
                             float nx, float ny, float nz,
                             float r, float g, float b, float a) {
        int o = i * 10;
        v[o]=x; v[o+1]=y; v[o+2]=z;
        v[o+3]=nx; v[o+4]=ny; v[o+5]=nz;
        v[o+6]=r; v[o+7]=g; v[o+8]=b; v[o+9]=a;
    }

    private static short[] trim(short[] arr, int len) {
        short[] out = new short[len];
        System.arraycopy(arr, 0, out, 0, len);
        return out;
    }
}
