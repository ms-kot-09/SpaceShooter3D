package com.voidhunter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class GameSurface extends GLSurfaceView {
    private final GameRenderer renderer;

    public GameSurface(Context ctx) {
        super(ctx);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        renderer = new GameRenderer(ctx);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        renderer.onTouch(e);
        return true;
    }
}
