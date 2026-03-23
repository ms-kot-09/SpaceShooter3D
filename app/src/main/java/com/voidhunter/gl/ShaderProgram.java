package com.voidhunter.gl;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderProgram {
    public final int id;

    public ShaderProgram(String vert, String frag) {
        int vs = compile(GLES20.GL_VERTEX_SHADER, vert);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, frag);
        id = GLES20.glCreateProgram();
        GLES20.glAttachShader(id, vs);
        GLES20.glAttachShader(id, fs);
        GLES20.glLinkProgram(id);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
    }

    private int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) Log.e("VH", GLES20.glGetShaderInfoLog(s));
        return s;
    }

    public void use() { GLES20.glUseProgram(id); }
    public int attr(String n)    { return GLES20.glGetAttribLocation(id, n); }
    public int uniform(String n) { return GLES20.glGetUniformLocation(id, n); }
}
