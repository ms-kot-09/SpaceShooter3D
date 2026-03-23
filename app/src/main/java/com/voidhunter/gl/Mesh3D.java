package com.voidhunter.gl;

import android.opengl.GLES20;
import java.nio.*;

public class Mesh3D {
    private final FloatBuffer vbuf;
    private final ShortBuffer ibuf;
    private final int vertCount, indexCount;
    public float[] color = {1,1,1,1};

    // stride: x,y,z, nx,ny,nz, r,g,b,a  (10 floats)
    public static final int STRIDE = 10 * 4;

    public Mesh3D(float[] verts, short[] indices) {
        vertCount  = verts.length / 10;
        indexCount = indices.length;
        vbuf = ByteBuffer.allocateDirect(verts.length * 4)
               .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vbuf.put(verts).position(0);
        ibuf = ByteBuffer.allocateDirect(indices.length * 2)
               .order(ByteOrder.nativeOrder()).asShortBuffer();
        ibuf.put(indices).position(0);
    }

    public void draw(ShaderProgram sh) {
        int aPos   = sh.attr("aPos");
        int aNorm  = sh.attr("aNorm");
        int aColor = sh.attr("aColor");

        vbuf.position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos,   3, GLES20.GL_FLOAT, false, STRIDE, vbuf);

        vbuf.position(3);
        GLES20.glEnableVertexAttribArray(aNorm);
        GLES20.glVertexAttribPointer(aNorm,  3, GLES20.GL_FLOAT, false, STRIDE, vbuf);

        vbuf.position(6);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, STRIDE, vbuf);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount,
                              GLES20.GL_UNSIGNED_SHORT, ibuf);

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aNorm);
        GLES20.glDisableVertexAttribArray(aColor);
    }
}
