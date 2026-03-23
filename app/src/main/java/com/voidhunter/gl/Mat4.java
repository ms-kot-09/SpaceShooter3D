package com.voidhunter.gl;

public class Mat4 {
    public static float[] identity() {
        return new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    }

    public static float[] perspective(float fov, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(fov * Math.PI / 360.0));
        float nf = 1.0f / (near - far);
        return new float[]{
            f/aspect,0,0,0,
            0,f,0,0,
            0,0,(far+near)*nf,-1,
            0,0,2*far*near*nf,0
        };
    }

    public static float[] lookAt(float ex,float ey,float ez,
                                  float cx,float cy,float cz,
                                  float ux,float uy,float uz) {
        float fx=cx-ex, fy=cy-ey, fz=cz-ez;
        float fl=(float)Math.sqrt(fx*fx+fy*fy+fz*fz);
        fx/=fl; fy/=fl; fz/=fl;
        float sx=fy*uz-fz*uy, sy=fz*ux-fx*uz, sz=fx*uy-fy*ux;
        float sl=(float)Math.sqrt(sx*sx+sy*sy+sz*sz);
        sx/=sl; sy/=sl; sz/=sl;
        float bx=sy*fz-sz*fy, by=sz*fx-sx*fz, bz=sx*fy-sy*fx;
        return new float[]{
            sx, bx, -fx, 0,
            sy, by, -fy, 0,
            sz, bz, -fz, 0,
            -(sx*ex+sy*ey+sz*ez), -(bx*ex+by*ey+bz*ez), fx*ex+fy*ey+fz*ez, 1
        };
    }

    public static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        for (int i=0;i<4;i++) for (int j=0;j<4;j++)
            for (int k=0;k<4;k++) out[i*4+j]+=a[i*4+k]*b[k*4+j];
        return out;
    }

    public static float[] translate(float[] m, float x, float y, float z) {
        float[] t = identity();
        t[12]=x; t[13]=y; t[14]=z;
        return mul(m, t);
    }

    public static float[] scale(float[] m, float x, float y, float z) {
        float[] s = identity();
        s[0]=x; s[5]=y; s[10]=z;
        return mul(m, s);
    }

    public static float[] rotY(float[] m, float a) {
        float c=(float)Math.cos(a), s=(float)Math.sin(a);
        float[] r = identity();
        r[0]=c; r[2]=s; r[8]=-s; r[10]=c;
        return mul(m, r);
    }

    public static float[] rotZ(float[] m, float a) {
        float c=(float)Math.cos(a), s=(float)Math.sin(a);
        float[] r = identity();
        r[0]=c; r[1]=-s; r[4]=s; r[5]=c;
        return mul(m, r);
    }

    public static float[] rotX(float[] m, float a) {
        float c=(float)Math.cos(a), s=(float)Math.sin(a);
        float[] r = identity();
        r[5]=c; r[6]=-s; r[9]=s; r[10]=c;
        return mul(m, r);
    }
}
