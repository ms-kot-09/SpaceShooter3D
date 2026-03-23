package com.voidhunter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import com.voidhunter.game.*;
import com.voidhunter.gl.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;
import java.util.ArrayList;

public class GameRenderer implements GLSurfaceView.Renderer {

    private final Context ctx;
    private int W, H;

    // GL
    private ShaderProgram shader3D, shaderHUD;
    private Mesh3D meshPlayer, meshSaucer, meshFighter, meshGunship;
    private Mesh3D meshBulletP, meshBulletE, meshMissile;

    // Starfield
    private FloatBuffer starVerts;
    private int starCount = 800;
    private ShaderProgram starShader;

    // HUD (Canvas overlay on a texture)
    private int hudTexture;
    private FloatBuffer quadVerts;
    private ShaderProgram quadShader;
    private Bitmap hudBitmap;
    private Canvas hudCanvas;
    private final Paint[] paints = new Paint[10];

    // Game
    private final GameState state = new GameState();
    private GameLogic logic;

    // Input
    private volatile float joyX=0, joyY=0;
    private volatile boolean fireDown=false, missileDown=false, prevMissile=false;
    private int joystickPtr=-1, firePtr=-1;
    private float joyCX, joyCY, joyRadius;

    // Time
    private long lastNanos = System.nanoTime();

    // Prefs
    private SharedPreferences prefs;

    public GameRenderer(Context ctx) {
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("vh_prefs", Context.MODE_PRIVATE);
        state.bestScore = prefs.getInt("best", 0);
        state.musicVol   = prefs.getFloat("music",0.7f);
        state.sfxVol     = prefs.getFloat("sfx",1f);
        state.difficulty = prefs.getInt("diff",1);
        state.vibration  = prefs.getBoolean("vib",true);
        state.sensitivity= prefs.getFloat("sens",1f);

        logic = new GameLogic(state, (x,y,z,big) -> {
            int cnt = big==2 ? 35 : 15;
            if (big==2) {
                logic.spawnParticle(x,y,z,1f,0.4f,0,0.3f,cnt,0.8f);
                logic.spawnParticle(x,y,z,1f,0.8f,0.2f,0.2f,cnt/2,0.6f);
            } else {
                logic.spawnParticle(x,y,z,1f,0.5f,0,0.2f,cnt,0.5f);
            }
        });
        initPaints();
    }

    private void initPaints() {
        paints[0] = makePaint(0xFFFF6B35, 52, true);  // score
        paints[1] = makePaint(0xAAFF6B35, 24, false); // label
        paints[2] = makePaint(0xFFFF4444, 0, false);  // health bar fill
        paints[3] = makePaint(0xFF44AAFF, 0, false);  // shield bar fill
        paints[4] = makePaint(0x33FFFFFF, 0, false);  // bar bg
        paints[5] = makePaint(0xFFFF6B35, 88, true);  // wave announce
        paints[6] = makePaint(0xFFFF6B35, 44, true);  // button text
        paints[7] = makePaint(0xFFFFFF44, 48, true);  // combo
        paints[8] = makePaint(0xFFFFFFFF, 56, true);  // stat
        paints[9] = makePaint(0x88FFFFFF, 32, false); // sub
    }

    private Paint makePaint(int color, int size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        if (size > 0) p.setTextSize(size);
        if (bold) p.setFakeBoldText(true);
        p.setTextAlign(Paint.Align.CENTER);
        p.setLetterSpacing(0.12f);
        return p;
    }

    // ── GL Init ───────────────────────────────────────────────────────────
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        GLES20.glClearColor(0.01f,0.01f,0.05f,1);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        build3DShader();
        buildStarShader();
        buildQuadShader();
        buildModels();
        buildStarfield();
    }

    private void build3DShader() {
        String vert =
            "uniform mat4 uMVP;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec3 aNorm;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec4 vColor;\n" +
            "varying vec3 vNorm;\n" +
            "void main(){\n" +
            "  gl_Position = uMVP * vec4(aPos,1.0);\n" +
            "  vColor = aColor;\n" +
            "  vNorm  = aNorm;\n" +
            "}\n";
        String frag =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "varying vec3 vNorm;\n" +
            "uniform float uTime;\n" +
            "uniform float uAlpha;\n" +
            "void main(){\n" +
            "  vec3 light = normalize(vec3(0.5,1.0,-0.5));\n" +
            "  float diff = max(dot(normalize(vNorm),light),0.0);\n" +
            "  vec3 rim   = vec3(0.2,0.5,1.0) * pow(1.0-diff,3.0) * 0.5;\n" +
            "  vec3 col   = vColor.rgb * (0.3 + diff*0.7) + rim;\n" +
            "  gl_FragColor = vec4(col, vColor.a * uAlpha);\n" +
            "}\n";
        shader3D = new ShaderProgram(vert, frag);
    }

    private void buildStarShader() {
        String vert =
            "uniform mat4 uMVP;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec4 vColor;\n" +
            "void main(){\n" +
            "  gl_Position = uMVP * vec4(aPos,1.0);\n" +
            "  gl_PointSize = 2.0;\n" +
            "  vColor = aColor;\n" +
            "}\n";
        String frag =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main(){\n" +
            "  gl_FragColor = vColor;\n" +
            "}\n";
        starShader = new ShaderProgram(vert, frag);
    }

    private void buildQuadShader() {
        String vert =
            "attribute vec2 aPos;\n" +
            "attribute vec2 aUV;\n" +
            "varying vec2 vUV;\n" +
            "void main(){\n" +
            "  gl_Position = vec4(aPos,0,1);\n" +
            "  vUV = aUV;\n" +
            "}\n";
        String frag =
            "precision mediump float;\n" +
            "uniform sampler2D uTex;\n" +
            "varying vec2 vUV;\n" +
            "void main(){\n" +
            "  gl_FragColor = texture2D(uTex, vUV);\n" +
            "}\n";
        quadShader = new ShaderProgram(vert, frag);

        float[] q = {-1,-1,0,1, 1,-1,1,1, -1,1,0,0, 1,1,1,0};
        quadVerts = ByteBuffer.allocateDirect(q.length*4)
                   .order(ByteOrder.nativeOrder()).asFloatBuffer();
        quadVerts.put(q).position(0);
    }

    private void buildModels() {
        meshPlayer   = Models.playerShip();
        meshSaucer   = Models.enemySaucer();
        meshFighter  = Models.enemyFighter();
        meshGunship  = Models.enemyGunship();
        meshBulletP  = Models.bullet(true);
        meshBulletE  = Models.bullet(false);
        meshMissile  = Models.missile();
    }

    private void buildStarfield() {
        java.util.Random r = new java.util.Random(42);
        float[] v = new float[starCount * 7]; // x,y,z, r,g,b,a
        for (int i=0; i<starCount; i++) {
            v[i*7]   = (r.nextFloat()-0.5f)*120;
            v[i*7+1] = (r.nextFloat()-0.5f)*60;
            v[i*7+2] = (r.nextFloat()-0.5f)*120;
            float b  = 0.4f+r.nextFloat()*0.6f;
            v[i*7+3]=b; v[i*7+4]=b; v[i*7+5]=b+0.1f; v[i*7+6]=1;
        }
        starVerts = ByteBuffer.allocateDirect(v.length*4)
                   .order(ByteOrder.nativeOrder()).asFloatBuffer();
        starVerts.put(v).position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        W=w; H=h;
        GLES20.glViewport(0,0,w,h);
        joyRadius = w*0.13f;
        joyCX = w*0.18f; joyCY = h*0.82f;

        // HUD bitmap
        hudBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        hudCanvas = new Canvas(hudBitmap);

        // HUD texture
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        hudTexture = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hudTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    // ── Main render loop ──────────────────────────────────────────────────
    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNanos) / 1e9f, 0.05f);
        lastNanos = now;

        // Update game
        boolean ms = missileDown; boolean prev = prevMissile; prevMissile = ms;
        logic.update(dt, joyX, joyY, fireDown, ms, prev);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] proj = Mat4.perspective(60, (float)W/H, 0.1f, 300f);
        float[] view = Mat4.lookAt(0,3,-14, 0,0,10, 0,1,0);
        float[] vp   = Mat4.mul(proj, view);

        // Stars
        drawStars(vp);

        // 3D scene
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        shader3D.use();
        GLES20.glUniform1f(shader3D.uniform("uTime"), state.time);
        GLES20.glUniform1f(shader3D.uniform("uAlpha"), 1f);

        if (state.state == GameState.ST_PLAYING || state.state == GameState.ST_GAMEOVER
            || state.state == GameState.ST_PAUSE) {
            drawPlayer(vp);
            drawEnemies(vp);
            drawBullets(vp);
            drawMissiles(vp);
            drawParticles(vp);
            drawPowerUps(vp);
        }

        // HUD overlay
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        drawHUD();
    }

    private void drawStars(float[] vp) {
        starShader.use();
        GLES20.glUniformMatrix4fv(starShader.uniform("uMVP"), 1, false, vp, 0);
        int aPos = starShader.attr("aPos");
        int aCol = starShader.attr("aColor");
        starVerts.position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 28, starVerts);
        starVerts.position(3);
        GLES20.glEnableVertexAttribArray(aCol);
        GLES20.glVertexAttribPointer(aCol, 4, GLES20.GL_FLOAT, false, 28, starVerts);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, starCount);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aCol);
    }

    private void drawMesh(Mesh3D mesh, float[] mvp, float alpha) {
        GLES20.glUniformMatrix4fv(shader3D.uniform("uMVP"),1,false,mvp,0);
        GLES20.glUniform1f(shader3D.uniform("uAlpha"), alpha);
        mesh.draw(shader3D);
    }

    private void drawPlayer(float[] vp) {
        if (state.pInvincible > 0 && ((int)(state.time*10))%2==0) return;
        float[] m = Mat4.translate(Mat4.identity(), state.px, state.py, state.pz);
        m = Mat4.rotZ(m, state.ptiltZ);
        m = Mat4.rotX(m, state.ptiltX);
        m = Mat4.scale(m, 0.9f,0.9f,0.9f);
        drawMesh(meshPlayer, Mat4.mul(vp,m), 1f);
    }

    private void drawEnemies(float[] vp) {
        for (GameState.Enemy e : state.enemies) {
            Mesh3D mesh = e.type==0?meshSaucer:e.type==1?meshFighter:meshGunship;
            float alpha = e.deathTimer>=0 ? e.deathTimer*2 : 1f;
            if (alpha<=0) continue;
            float[] m = Mat4.translate(Mat4.identity(), e.x, e.y, e.z);
            m = Mat4.rotY(m, e.rotY);
            m = Mat4.rotX(m, (float)Math.PI); // face toward player
            drawMesh(mesh, Mat4.mul(vp,m), alpha);
        }
    }

    private void drawBullets(float[] vp) {
        for (GameState.Projectile b : state.pBullets) {
            float[] m = Mat4.translate(Mat4.identity(), b.x,b.y,b.z);
            drawMesh(meshBulletP, Mat4.mul(vp,m), 1f);
        }
        for (GameState.Projectile b : state.eBullets) {
            float[] m = Mat4.translate(Mat4.identity(), b.x,b.y,b.z);
            drawMesh(meshBulletE, Mat4.mul(vp,m), 1f);
        }
    }

    private void drawMissiles(float[] vp) {
        for (GameState.Missile m2 : state.missiles) {
            float[] m = Mat4.translate(Mat4.identity(), m2.x,m2.y,m2.z);
            drawMesh(meshMissile, Mat4.mul(vp,m), 1f);
        }
    }

    private void drawParticles(float[] vp) {
        for (GameState.Particle p : state.particles) {
            float alpha = p.life/p.maxLife;
            float sc = p.size * (0.3f + alpha*0.8f);
            float[] m = Mat4.translate(Mat4.identity(), p.x,p.y,p.z);
            m = Mat4.scale(m,sc,sc,sc);
            GLES20.glUniformMatrix4fv(shader3D.uniform("uMVP"),1,false,Mat4.mul(vp,m),0);
            GLES20.glUniform1f(shader3D.uniform("uAlpha"), alpha);
            // Draw as small octahedron using existing mesh scaled
            meshBulletE.draw(shader3D);
        }
    }

    private void drawPowerUps(float[] vp) {
        int[][] colors = {{0,255,80},{80,160,255},{255,200,0},{255,100,0}};
        for (GameState.PowerUp p : state.powerups) {
            float[] m = Mat4.translate(Mat4.identity(), p.x,p.y,p.z);
            m = Mat4.rotY(m, p.t*2);
            m = Mat4.scale(m,0.5f,0.5f,0.5f);
            GLES20.glUniformMatrix4fv(shader3D.uniform("uMVP"),1,false,Mat4.mul(vp,m),0);
            GLES20.glUniform1f(shader3D.uniform("uAlpha"),0.9f);
            meshSaucer.draw(shader3D);
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────────
    private void drawHUD() {
        hudBitmap.eraseColor(0);

        switch (state.state) {
            case GameState.ST_MENU:     drawMenuHUD(); break;
            case GameState.ST_PLAYING:  drawGameHUD(); break;
            case GameState.ST_PAUSE:    drawGameHUD(); drawPauseHUD(); break;
            case GameState.ST_GAMEOVER: drawGameHUD(); drawGameOverHUD(); break;
            case GameState.ST_SETTINGS: drawSettingsHUD(); break;
        }

        // Upload bitmap to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hudTexture);
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, hudBitmap, 0);

        // Draw fullscreen quad
        quadShader.use();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hudTexture);
        GLES20.glUniform1i(quadShader.uniform("uTex"), 0);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        quadVerts.position(0);
        int aPos = quadShader.attr("aPos");
        int aUV  = quadShader.attr("aUV");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,16,quadVerts);
        quadVerts.position(2);
        GLES20.glEnableVertexAttribArray(aUV);
        GLES20.glVertexAttribPointer(aUV,2,GLES20.GL_FLOAT,false,16,quadVerts);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aUV);
    }

    private void drawGameHUD() {
        float pad = 28;
        // Hull bar
        label("HULL", pad+130, 55);
        bar(pad, 65, 260, 14, state.hp/100f, state.hp>50?0xFFFF4444:state.hp>25?0xFFFF8800:0xFFFF2200);
        // Shield bar
        label("SHIELD", pad+130, 98);
        bar(pad, 108, 260, 14, state.shield/50f, 0xFF44AAFF);

        // Score
        paints[0].setTextAlign(Paint.Align.CENTER);
        label("SCORE", W/2f, 50);
        paints[0].setTextSize(56);
        hudCanvas.drawText(String.valueOf(state.score), W/2f, 102, paints[0]);
        paints[0].setTextSize(52);

        // Wave
        paints[0].setTextAlign(Paint.Align.RIGHT);
        label2("WAVE", W-pad, 50);
        paints[0].setTextSize(56);
        hudCanvas.drawText(String.valueOf(state.wave), W-pad, 102, paints[0]);
        paints[0].setTextSize(52);
        paints[0].setTextAlign(Paint.Align.CENTER);

        // Missiles icons
        label("MSL", pad+55, H*0.76f-28);
        for (int i=0;i<5;i++) {
            Paint mp=new Paint(Paint.ANTI_ALIAS_FLAG);
            mp.setColor(i<state.missileAmmo?0xFFFF6B35:0x33FF6B35);
            Path p=new Path();
            float mx=pad+i*20, my=H*0.76f-20;
            p.moveTo(mx+8,my-16); p.lineTo(mx+14,my+2);
            p.lineTo(mx+2,my+2); p.close();
            hudCanvas.drawPath(p,mp);
        }

        // Combo
        if (state.combo>2) {
            paints[7].setTextAlign(Paint.Align.CENTER);
            hudCanvas.drawText(state.combo+"x COMBO!", W/2f, H*0.2f, paints[7]);
        }

        // Wave announce
        if (state.waveShowTimer>0) {
            float alpha=Math.min(state.waveShowTimer*1.5f,1f);
            paints[5].setAlpha((int)(alpha*255));
            hudCanvas.drawText("— WAVE "+state.wave+" —", W/2f, H*0.38f, paints[5]);
            paints[5].setAlpha(255);
        }

        // Damage flash
        if (state.dmgFlash>0) {
            Paint fp=new Paint(); fp.setColor(Color.argb((int)(state.dmgFlash*80),255,0,0));
            hudCanvas.drawRect(0,0,W,H,fp);
        }

        // Joystick
        drawJoystick();

        // Fire / missile buttons
        drawButtons();

        // Pause button
        Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
        pp.setColor(0x44FFFFFF);
        pp.setStyle(Paint.Style.FILL);
        hudCanvas.drawCircle(W-50,50,32,pp);
        pp.setColor(0xAAFFFFFF);
        pp.setStyle(Paint.Style.FILL);
        hudCanvas.drawRect(W-62,38,W-55,62,pp);
        hudCanvas.drawRect(W-48,38,W-41,62,pp);
    }

    private void drawJoystick() {
        Paint base=new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setColor(0x22FF6B35); base.setStyle(Paint.Style.FILL);
        hudCanvas.drawCircle(joyCX,joyCY,joyRadius,base);
        base.setColor(0x55FF6B35); base.setStyle(Paint.Style.STROKE); base.setStrokeWidth(3);
        hudCanvas.drawCircle(joyCX,joyCY,joyRadius,base);
        Paint knob=new Paint(Paint.ANTI_ALIAS_FLAG);
        knob.setColor(0x88FF6B35);
        hudCanvas.drawCircle(joyCX+joyX*joyRadius, joyCY+joyY*joyRadius, joyRadius*0.35f, knob);
    }

    private void drawButtons() {
        float fbX=W*0.82f, fbY=H*0.82f, fbR=W*0.1f;
        Paint bp=new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(fireDown?0x88FF4444:0x44FF4444); bp.setStyle(Paint.Style.FILL);
        hudCanvas.drawCircle(fbX,fbY,fbR,bp);
        bp.setColor(0xAAFF4444); bp.setStyle(Paint.Style.STROKE); bp.setStrokeWidth(3);
        hudCanvas.drawCircle(fbX,fbY,fbR,bp);
        paints[6].setColor(0xFFFF4444); paints[6].setTextSize(38);
        hudCanvas.drawText("FIRE", fbX, fbY+14, paints[6]);

        float mbX=W*0.65f, mbY=H*0.84f, mbR=W*0.07f;
        bp.setColor(state.missileAmmo>0?0x44FF8800:0x22888888);
        bp.setStyle(Paint.Style.FILL);
        hudCanvas.drawCircle(mbX,mbY,mbR,bp);
        bp.setColor(state.missileAmmo>0?0xAAFF8800:0x44888888);
        bp.setStyle(Paint.Style.STROKE);
        hudCanvas.drawCircle(mbX,mbY,mbR,bp);
        paints[6].setColor(state.missileAmmo>0?0xFFFF8800:0x66888888);
        paints[6].setTextSize(28);
        hudCanvas.drawText("MSL", mbX, mbY+10, paints[6]);
        paints[6].setColor(0xFFFF6B35); paints[6].setTextSize(44);
    }

    private void drawMenuHUD() {
        // Gradient overlay
        Paint grad=new Paint();
        grad.setShader(new RadialGradient(W/2f,H*0.4f,H*0.7f,
            new int[]{0x44FF4400,0x11330066,0x00000000},
            new float[]{0,0.5f,1}, android.graphics.Shader.TileMode.CLAMP));
        hudCanvas.drawRect(0,0,W,H,grad);

        // Title
        Paint title=new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(0xFFFF6B35); title.setTextSize(120); title.setFakeBoldText(true);
        title.setTextAlign(Paint.Align.CENTER);
        title.setMaskFilter(new BlurMaskFilter(25,BlurMaskFilter.Blur.NORMAL));
        hudCanvas.drawText("VOID", W/2f, H*0.35f, title);
        hudCanvas.drawText("HUNTER", W/2f, H*0.45f, title);

        paints[9].setTextSize(36);
        hudCanvas.drawText("3D  SPACE  COMBAT", W/2f, H*0.51f, paints[9]);

        // Best score
        if (state.bestScore>0) {
            paints[1].setTextAlign(Paint.Align.CENTER);
            hudCanvas.drawText("BEST: "+state.bestScore, W/2f, H*0.56f, paints[1]);
        }

        // Difficulty indicator
        String[] diffs={"EASY","NORMAL","HARD"};
        int[] dcols={0xFF44FF44,0xFFFFAA00,0xFFFF4444};
        Paint dc=new Paint(Paint.ANTI_ALIAS_FLAG);
        dc.setColor(dcols[state.difficulty]); dc.setTextSize(30);
        dc.setTextAlign(Paint.Align.CENTER); dc.setFakeBoldText(true);
        hudCanvas.drawText("[ "+diffs[state.difficulty]+" ]", W/2f, H*0.61f, dc);

        btn(W/2f, H*0.69f, 420, 90, "LAUNCH MISSION");
        btn(W/2f, H*0.79f, 420, 80, "SETTINGS");
        paints[9].setTextSize(28);
        hudCanvas.drawText("TAP TO START", W/2f, H*0.88f, paints[9]);
    }

    private void drawPauseHUD() {
        Paint ov=new Paint(); ov.setColor(0xBB000010);
        hudCanvas.drawRect(0,0,W,H,ov);
        paints[5].setTextAlign(Paint.Align.CENTER);
        hudCanvas.drawText("PAUSED", W/2f, H*0.35f, paints[5]);
        btn(W/2f, H*0.5f, 400, 90, "RESUME");
        btn(W/2f, H*0.62f, 400, 90, "SETTINGS");
        btn(W/2f, H*0.74f, 400, 90, "MAIN MENU");
    }

    private void drawGameOverHUD() {
        Paint ov=new Paint(); ov.setColor(0xCC000010);
        hudCanvas.drawRect(0,0,W,H,ov);

        paints[5].setColor(0xFFFF4444);
        hudCanvas.drawText("SHIP DESTROYED", W/2f, H*0.28f, paints[5]);
        paints[5].setColor(0xFFFF6B35);

        // Stats
        float sy=H*0.42f;
        paints[8].setTextSize(64); paints[8].setTextAlign(Paint.Align.CENTER);
        hudCanvas.drawText(String.valueOf(state.score),W/2f-190,sy,paints[8]);
        hudCanvas.drawText(String.valueOf(state.wave), W/2f,     sy,paints[8]);
        hudCanvas.drawText(String.valueOf(state.kills),W/2f+190, sy,paints[8]);
        paints[1].setTextAlign(Paint.Align.CENTER);
        hudCanvas.drawText("SCORE",W/2f-190,sy+36,paints[1]);
        hudCanvas.drawText("WAVE", W/2f,    sy+36,paints[1]);
        hudCanvas.drawText("KILLS",W/2f+190,sy+36,paints[1]);

        // New best?
        if (state.score>=state.bestScore && state.score>0) {
            Paint nb=new Paint(Paint.ANTI_ALIAS_FLAG);
            nb.setColor(0xFFFFDD00); nb.setTextSize(36);
            nb.setTextAlign(Paint.Align.CENTER); nb.setFakeBoldText(true);
            hudCanvas.drawText("✦ NEW BEST SCORE ✦", W/2f, H*0.54f, nb);
        }

        btn(W/2f, H*0.63f, 420, 90, "RETRY MISSION");
        btn(W/2f, H*0.74f, 420, 90, "MAIN MENU");
    }

    private void drawSettingsHUD() {
        Paint ov=new Paint(); ov.setColor(0xEE000010);
        hudCanvas.drawRect(0,0,W,H,ov);
        paints[5].setTextAlign(Paint.Align.CENTER);
        hudCanvas.drawText("SETTINGS", W/2f, H*0.12f, paints[5]);

        float y=H*0.22f, step=H*0.1f;
        String[] diffs={"EASY","NORMAL","HARD"};
        int[] dcols={0xFF44FF44,0xFFFFAA00,0xFFFF4444};

        settingRow("DIFFICULTY", diffs[state.difficulty], dcols[state.difficulty], y);
        settingRow("MUSIC VOL",  pct(state.musicVol),  0xFFFF6B35, y+=step);
        settingRow("SFX VOL",    pct(state.sfxVol),    0xFFFF6B35, y+=step);
        settingRow("SENSITIVITY",pct(state.sensitivity),0xFFFF6B35,y+=step);
        settingRow("VIBRATION",  state.vibration?"ON":"OFF",
                   state.vibration?0xFF44FF44:0xFFFF4444, y+=step);

        btn(W/2f, H*0.76f, 380, 90, "◀  BACK");
    }

    private void settingRow(String label, String val, int valColor, float y) {
        paints[1].setTextAlign(Paint.Align.LEFT);
        hudCanvas.drawText(label, W*0.08f, y, paints[1]);
        Paint vp=new Paint(Paint.ANTI_ALIAS_FLAG);
        vp.setColor(valColor); vp.setTextSize(38); vp.setFakeBoldText(true);
        vp.setTextAlign(Paint.Align.RIGHT);
        hudCanvas.drawText(val, W*0.92f, y, vp);
        Paint line=new Paint(); line.setColor(0x22FFFFFF);
        hudCanvas.drawLine(W*0.06f,y+12,W*0.94f,y+12,line);
        paints[1].setTextAlign(Paint.Align.CENTER);
    }

    private void btn(float cx, float cy, float w, float h, String text) {
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0x22FF6B35); bg.setStyle(Paint.Style.FILL);
        RectF r=new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2);
        hudCanvas.drawRoundRect(r,12,12,bg);
        bg.setColor(0xAAFF6B35); bg.setStyle(Paint.Style.STROKE); bg.setStrokeWidth(2.5f);
        hudCanvas.drawRoundRect(r,12,12,bg);
        paints[6].setTextSize(40);
        hudCanvas.drawText(text, cx, cy+15, paints[6]);
    }

    private void bar(float x, float y, float w, float h, float frac, int col) {
        Paint bp=new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(0x33FFFFFF); bp.setStyle(Paint.Style.FILL);
        hudCanvas.drawRoundRect(new RectF(x,y,x+w,y+h),6,6,bp);
        bp.setColor(col);
        hudCanvas.drawRoundRect(new RectF(x,y,x+w*frac,y+h),6,6,bp);
    }

    private void label(String t, float x, float y) {
        paints[1].setTextAlign(Paint.Align.CENTER);
        hudCanvas.drawText(t, x, y, paints[1]);
    }
    private void label2(String t, float x, float y) {
        paints[1].setTextAlign(Paint.Align.RIGHT);
        hudCanvas.drawText(t, x, y, paints[1]);
        paints[1].setTextAlign(Paint.Align.LEFT);
    }
    private String pct(float v) { return (int)(v*100)+"%"; }

    // ── Touch input ───────────────────────────────────────────────────────
    public void onTouch(MotionEvent e) {
        int action = e.getActionMasked();
        int idx    = e.getActionIndex();
        int pid    = e.getPointerId(idx);
        float tx   = e.getX(idx), ty = e.getY(idx);

        if (state.state == GameState.ST_MENU) {
            if (action == MotionEvent.ACTION_DOWN) handleMenuTouch(tx, ty);
            return;
        }
        if (state.state == GameState.ST_SETTINGS) {
            if (action == MotionEvent.ACTION_DOWN) handleSettingsTouch(tx, ty);
            return;
        }
        if (state.state == GameState.ST_GAMEOVER) {
            if (action == MotionEvent.ACTION_DOWN) handleGameOverTouch(tx, ty);
            return;
        }
        if (state.state == GameState.ST_PAUSE) {
            if (action == MotionEvent.ACTION_DOWN) handlePauseTouch(tx, ty);
            return;
        }

        // Playing
        float fbX=W*0.82f, fbY=H*0.82f, fbR=W*0.1f;
        float mbX=W*0.65f, mbY=H*0.84f, mbR=W*0.07f;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Pause button
                if (dist(tx,ty,W-50,50)<35) { state.state=GameState.ST_PAUSE; return; }
                if (dist(tx,ty,fbX,fbY)<fbR) { fireDown=true; firePtr=pid; }
                else if (dist(tx,ty,mbX,mbY)<mbR) { missileDown=true; }
                else { joystickPtr=pid; joyCX=tx; joyCY=ty; }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i=0;i<e.getPointerCount();i++) {
                    if (e.getPointerId(i)==joystickPtr) {
                        float dx=e.getX(i)-joyCX, dy=e.getY(i)-joyCY;
                        float d=(float)Math.sqrt(dx*dx+dy*dy);
                        if (d>joyRadius){dx=dx/d*joyRadius;dy=dy/d*joyRadius;}
                        joyX=dx/joyRadius; joyY=dy/joyRadius;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pid==joystickPtr){joyX=0;joyY=0;joystickPtr=-1;}
                if (pid==firePtr){fireDown=false;firePtr=-1;}
                missileDown=false;
                break;
        }
    }

    private void handleMenuTouch(float tx, float ty) {
        if (inBtn(tx,ty, W/2f,H*0.69f,420,90)) { logic.startGame(); return; }
        if (inBtn(tx,ty, W/2f,H*0.79f,420,80)) { state.state=GameState.ST_SETTINGS; return; }
        // Tap difficulty
        if (inBtn(tx,ty, W/2f,H*0.61f,200,40)) {
            state.difficulty=(state.difficulty+1)%3;
            savePrefs();
        }
    }

    private void handleSettingsTouch(float tx, float ty) {
        float step=H*0.1f, y0=H*0.22f;
        if (inBtn(tx,ty,W/2f,H*0.76f,380,90)){state.state=GameState.ST_MENU; savePrefs(); return;}
        // Difficulty
        if (ty>y0-30 && ty<y0+30) { state.difficulty=(state.difficulty+1)%3; return; }
        // Music vol
        if (ty>y0+step-30 && ty<y0+step+30) { state.musicVol=cycle(state.musicVol); return; }
        // SFX
        if (ty>y0+step*2-30 && ty<y0+step*2+30) { state.sfxVol=cycle(state.sfxVol); return; }
        // Sensitivity
        if (ty>y0+step*3-30 && ty<y0+step*3+30) { state.sensitivity=cycleSens(state.sensitivity); }
        // Vibration
        if (ty>y0+step*4-30 && ty<y0+step*4+30) { state.vibration=!state.vibration; }
    }

    private void handleGameOverTouch(float tx, float ty) {
        if (inBtn(tx,ty,W/2f,H*0.63f,420,90)){logic.startGame();}
        if (inBtn(tx,ty,W/2f,H*0.74f,420,90)){state.state=GameState.ST_MENU;}
    }

    private void handlePauseTouch(float tx, float ty) {
        if (inBtn(tx,ty,W/2f,H*0.5f,400,90)){state.state=GameState.ST_PLAYING;}
        if (inBtn(tx,ty,W/2f,H*0.62f,400,90)){state.state=GameState.ST_SETTINGS;}
        if (inBtn(tx,ty,W/2f,H*0.74f,400,90)){state.state=GameState.ST_MENU;}
    }

    private boolean inBtn(float tx,float ty,float cx,float cy,float w,float h) {
        return tx>cx-w/2&&tx<cx+w/2&&ty>cy-h/2&&ty<cy+h/2;
    }

    private float dist(float x1,float y1,float x2,float y2) {
        float dx=x1-x2,dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy);
    }

    private float cycle(float v) {
        if (v<0.3f) return 0.5f;
        if (v<0.6f) return 0.8f;
        if (v<0.9f) return 1.0f;
        return 0.0f;
    }
    private float cycleSens(float v) {
        if (v<0.7f) return 0.8f;
        if (v<1.1f) return 1.3f;
        return 0.6f;
    }

    private void savePrefs() {
        prefs.edit()
            .putInt("best", state.bestScore)
            .putFloat("music", state.musicVol)
            .putFloat("sfx", state.sfxVol)
            .putInt("diff", state.difficulty)
            .putBoolean("vib", state.vibration)
            .putFloat("sens", state.sensitivity)
            .apply();
    }
}
