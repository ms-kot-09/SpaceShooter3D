package com.voidhunter;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    private final SurfaceHolder holder;
    private final GameView view;
    private volatile boolean running = false;

    public GameThread(SurfaceHolder holder, GameView view) {
        this.holder = holder;
        this.view = view;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (running) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastTime;
            lastTime = now;

            view.update();

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                synchronized (holder) {
                    if (canvas != null) view.draw(canvas);
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            long sleepTime = FRAME_TIME - (System.currentTimeMillis() - now);
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime); } catch (InterruptedException e) { break; }
            }
        }
    }
}
