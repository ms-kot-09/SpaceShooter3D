package com.voidhunter;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

public class MainActivity extends Activity {
    private GameSurface surface;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        surface = new GameSurface(this);
        setContentView(surface);
    }

    @Override protected void onResume() { super.onResume(); surface.onResume(); }
    @Override protected void onPause()  { super.onPause();  surface.onPause();  }
}
