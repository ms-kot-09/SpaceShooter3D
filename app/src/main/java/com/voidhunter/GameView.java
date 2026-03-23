package com.voidhunter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private GameEngine engine;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        engine = new GameEngine(context);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        engine.init(getWidth(), getHeight());
        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        engine.init(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        engine.handleTouch(event);
        return true;
    }

    public void update() {
        engine.update();
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            engine.render(canvas);
        }
    }

    public void resume() {
        if (thread == null || !thread.isAlive()) {
            thread = new GameThread(getHolder(), this);
            thread.setRunning(true);
            thread.start();
        }
    }

    public void pause() {
        if (thread != null) {
            thread.setRunning(false);
            try { thread.join(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}
