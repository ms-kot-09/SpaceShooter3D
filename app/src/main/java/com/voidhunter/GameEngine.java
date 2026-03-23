package com.voidhunter;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameEngine {

    // ── Constants ─────────────────────────
    private static final int STATE_MENU    = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAMEOVER= 2;

    // ── Screen ────────────────────────────
    private int W, H;
    private float scaleX, scaleY;
    private Context ctx;

    // ── State ─────────────────────────────
    private int state = STATE_MENU;
    private int score, wave, kills, combo;
    private float comboTimer;
    private int playerHP = 100, playerShield = 50;
    private int missileAmmo = 5;
    private float spawnTimer, spawnInterval = 2.2f;
    private int waveKillCount, waveKillTarget = 6;
    private float waveAnnounceTimer = 0;
    private String waveAnnounceText = "";
    private float damageFlash = 0;

    // ── Entities ──────────────────────────
    private Player player;
    private ArrayList<Enemy>    enemies  = new ArrayList<>();
    private ArrayList<Bullet>   pBullets = new ArrayList<>();
    private ArrayList<Bullet>   eBullets = new ArrayList<>();
    private ArrayList<Missile>  missiles = new ArrayList<>();
    private ArrayList<Particle> particles= new ArrayList<>();
    private ArrayList<Star>     stars    = new ArrayList<>();

    // ── Input ─────────────────────────────
    private float joyX = 0, joyY = 0;
    private boolean fireDown = false, missilePressed = false;
    private boolean prevMissile = false;
    private int joystickPointerId = -1, firePointerId = -1;
    private float joyCX, joyCY;
    private static final float JOY_RADIUS = 0;  // set in init
    private float joyRadius;

    // ── Paints ────────────────────────────
    private Paint bgPaint, starPaint, hudPaint, hudLabelPaint;
    private Paint healthBarPaint, shieldBarPaint, barBgPaint;
    private Paint playerPaint, wingPaint, enginePaint, thrusterPaint;
    private Paint enemyPaint, enemyGlowPaint;
    private Paint bulletPaint, eBulletPaint;
    private Paint particlePaint;
    private Paint joyBasePaint, joyKnobPaint;
    private Paint fireBtnPaint, fireBtnTextPaint;
    private Paint menuTitlePaint, menuSubPaint, menuBtnPaint, menuBtnTextPaint;
    private Paint waveAnnouncePaint;
    private Paint explosionPaint;
    private Paint missilePaint;
    private Typeface orbitron;

    private Random rand = new Random();

    // ── Inner classes ─────────────────────

    static class Star {
        float x, y, r, speed, alpha;
    }

    static class Player {
        float x, y;
        float tiltZ = 0; // degrees
        float fireTimer = 0;
        float shieldRegenTimer = 0;
        float boostEffect = 0;
    }

    static class Enemy {
        float x, y;
        float speed;
        int hp, maxHp;
        int type; // 0=saucer 1=fighter 2=gunship
        float t = 0;
        float waveAmp, waveFreq;
        float fireTimer, fireRate;
        float glowPulse = 0;
        float size;
    }

    static class Bullet {
        float x, y, vx, vy;
        float life;
        boolean player;
        float size;
    }

    static class Missile {
        float x, y, vx, vy;
        float life;
        Enemy target;
        float trail = 0;
    }

    static class Particle {
        float x, y, vx, vy;
        float life, maxLife;
        int color;
        float size;
    }

    // ── Init ──────────────────────────────
    public GameEngine(Context ctx) {
        this.ctx = ctx;
        initPaints();
    }

    public void init(int w, int h) {
        W = w; H = h;
        scaleX = w / 1080f;
        scaleY = h / 1920f;
        joyRadius = w * 0.13f;
        joyCX = w * 0.18f;
        joyCY = h * 0.82f;

        stars.clear();
        for (int i = 0; i < 200; i++) {
            Star s = new Star();
            s.x = rand.nextFloat() * W;
            s.y = rand.nextFloat() * H;
            s.r = rand.nextFloat() * 2.5f + 0.5f;
            s.speed = rand.nextFloat() * 2 + 0.5f;
            s.alpha = rand.nextFloat() * 0.8f + 0.2f;
            stars.add(s);
        }
    }

    private void initPaints() {
        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.WHITE);

        hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudPaint.setColor(0xFFFF6B35);
        hudPaint.setTextSize(52);
        hudPaint.setFakeBoldText(true);
        hudPaint.setTextAlign(Paint.Align.CENTER);

        hudLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudLabelPaint.setColor(0xAAFF6B35);
        hudLabelPaint.setTextSize(24);
        hudLabelPaint.setLetterSpacing(0.15f);

        healthBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        healthBarPaint.setColor(0xFFFF4444);
        healthBarPaint.setStyle(Paint.Style.FILL);

        shieldBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldBarPaint.setColor(0xFF44AAFF);
        shieldBarPaint.setStyle(Paint.Style.FILL);

        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(0x33FFFFFF);
        barBgPaint.setStyle(Paint.Style.FILL);

        playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playerPaint.setColor(0xFF4488CC);
        playerPaint.setStyle(Paint.Style.FILL);

        wingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wingPaint.setColor(0xFF223355);
        wingPaint.setStyle(Paint.Style.FILL);

        enginePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        enginePaint.setColor(0xFF334466);
        enginePaint.setStyle(Paint.Style.FILL);

        thrusterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thrusterPaint.setColor(0xFFFF6600);
        thrusterPaint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));

        enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        enemyPaint.setStyle(Paint.Style.FILL);

        enemyGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        enemyGlowPaint.setStyle(Paint.Style.FILL);
        enemyGlowPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));

        bulletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bulletPaint.setColor(0xFF44FFFF);
        bulletPaint.setStyle(Paint.Style.FILL);
        bulletPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

        eBulletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eBulletPaint.setColor(0xFFFF3300);
        eBulletPaint.setStyle(Paint.Style.FILL);
        eBulletPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        joyBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        joyBasePaint.setColor(0x22FF6B35);
        joyBasePaint.setStyle(Paint.Style.FILL);

        Paint joyBaseBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        joyKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        joyKnobPaint.setColor(0x99FF6B35);
        joyKnobPaint.setStyle(Paint.Style.FILL);

        fireBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fireBtnPaint.setColor(0x44FF4444);
        fireBtnPaint.setStyle(Paint.Style.FILL);

        fireBtnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fireBtnTextPaint.setColor(0xFFFF4444);
        fireBtnTextPaint.setTextSize(36);
        fireBtnTextPaint.setTextAlign(Paint.Align.CENTER);
        fireBtnTextPaint.setFakeBoldText(true);

        menuTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        menuTitlePaint.setColor(0xFFFF6B35);
        menuTitlePaint.setTextSize(110);
        menuTitlePaint.setFakeBoldText(true);
        menuTitlePaint.setTextAlign(Paint.Align.CENTER);
        menuTitlePaint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));

        menuSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        menuSubPaint.setColor(0x88FFFFFF);
        menuSubPaint.setTextSize(36);
        menuSubPaint.setTextAlign(Paint.Align.CENTER);
        menuSubPaint.setLetterSpacing(0.2f);

        menuBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        menuBtnPaint.setColor(0x00000000);
        menuBtnPaint.setStyle(Paint.Style.STROKE);
        menuBtnPaint.setStrokeWidth(3);
        menuBtnPaint.setColor(0xFFFF6B35);

        menuBtnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        menuBtnTextPaint.setColor(0xFFFF6B35);
        menuBtnTextPaint.setTextSize(44);
        menuBtnTextPaint.setTextAlign(Paint.Align.CENTER);
        menuBtnTextPaint.setFakeBoldText(true);
        menuBtnTextPaint.setLetterSpacing(0.15f);

        waveAnnouncePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waveAnnouncePaint.setColor(0xFFFF6B35);
        waveAnnouncePaint.setTextSize(90);
        waveAnnouncePaint.setFakeBoldText(true);
        waveAnnouncePaint.setTextAlign(Paint.Align.CENTER);
        waveAnnouncePaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));

        explosionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        explosionPaint.setStyle(Paint.Style.FILL);
        explosionPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

        missilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        missilePaint.setColor(0xFFFF8800);
        missilePaint.setStyle(Paint.Style.FILL);
        missilePaint.setMaskFilter(new BlurMaskFilter(12, BlurMaskFilter.Blur.NORMAL));
    }

    // ── Touch ─────────────────────────────
    public void handleTouch(MotionEvent e) {
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        int pid = e.getPointerId(idx);
        float tx = e.getX(idx), ty = e.getY(idx);

        float fireBtnX = W * 0.82f, fireBtnY = H * 0.82f, fireBtnR = W * 0.1f;
        float msiBtnX  = W * 0.65f, msiBtnY  = H * 0.84f, msiBtnR = W * 0.07f;

        if (state == STATE_MENU || state == STATE_GAMEOVER) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                startGame();
            }
            return;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float dx = tx - fireBtnX, dy2 = ty - fireBtnY;
                if (dx*dx + dy2*dy2 < fireBtnR*fireBtnR) {
                    fireDown = true; firePointerId = pid;
                } else {
                    float mdx = tx - msiBtnX, mdy = ty - msiBtnY;
                    if (mdx*mdx + mdy*mdy < msiBtnR*msiBtnR) {
                        missilePressed = true;
                    } else {
                        joystickPointerId = pid;
                        joyCX = tx; joyCY = ty;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int p = e.getPointerId(i);
                    if (p == joystickPointerId) {
                        float jdx = e.getX(i) - joyCX;
                        float jdy = e.getY(i) - joyCY;
                        float dist = (float)Math.sqrt(jdx*jdx+jdy*jdy);
                        float maxR = joyRadius;
                        if (dist > maxR) { jdx = jdx/dist*maxR; jdy = jdy/dist*maxR; }
                        joyX = jdx / maxR;
                        joyY = jdy / maxR;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pid == joystickPointerId) { joyX = 0; joyY = 0; joystickPointerId = -1; }
                if (pid == firePointerId)     { fireDown = false; firePointerId = -1; }
                missilePressed = false;
                break;
        }
    }

    // ── Update ────────────────────────────
    private long lastTime = System.currentTimeMillis();

    public void update() {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.05f);
        lastTime = now;

        if (state != STATE_PLAYING) return;

        updateStars(dt);
        updatePlayer(dt);
        updateEnemies(dt);
        updateBullets(dt);
        updateMissiles(dt);
        updateParticles(dt);
        checkCollisions();
        checkWave();

        // Timers
        if (waveAnnounceTimer > 0) waveAnnounceTimer -= dt;
        if (damageFlash > 0) damageFlash -= dt * 4;
        if (comboTimer > 0) { comboTimer -= dt; if (comboTimer <= 0) combo = 0; }

        // Spawn
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            spawnTimer = spawnInterval * (0.7f + rand.nextFloat() * 0.6f);
            int count = Math.min(1 + wave / 3, 3);
            for (int i = 0; i < count; i++) spawnEnemy();
        }
    }

    private void updateStars(float dt) {
        for (Star s : stars) {
            s.y += s.speed * (1 + joyY * 0.3f);
            if (s.y > H) { s.y = 0; s.x = rand.nextFloat() * W; }
        }
    }

    private void updatePlayer(float dt) {
        float speed = 900 * dt;
        player.x += joyX * speed;
        player.y += joyY * speed;
        player.x = Math.max(60, Math.min(W - 60, player.x));
        player.y = Math.max(H * 0.3f, Math.min(H * 0.88f, player.y));
        player.tiltZ = joyX * 25f;
        player.boostEffect = Math.abs(joyX) + Math.abs(joyY);

        // Fire
        player.fireTimer -= dt;
        if (fireDown && player.fireTimer <= 0) {
            player.fireTimer = 0.12f;
            spawnPlayerBullets();
        }

        // Missile
        if (missilePressed && !prevMissile) launchMissile();
        prevMissile = missilePressed;

        // Shield regen
        if (playerShield < 50) playerShield = Math.min(50, (int)(playerShield + 5 * dt));
    }

    private void spawnPlayerBullets() {
        float[] offsets = {-30, 30};
        for (float ox : offsets) {
            Bullet b = new Bullet();
            b.x = player.x + ox;
            b.y = player.y - 80;
            b.vy = -1400;
            b.vx = ox * 0.5f;
            b.life = 1.5f;
            b.player = true;
            b.size = 14;
            pBullets.add(b);
        }
    }

    private void launchMissile() {
        if (missileAmmo <= 0) return;
        missileAmmo--;
        Missile m = new Missile();
        m.x = player.x;
        m.y = player.y - 60;
        m.vy = -600;
        m.life = 4f;
        // Find closest enemy
        float minDist = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            float d = dist(m.x, m.y, e.x, e.y);
            if (d < minDist) { minDist = d; m.target = e; }
        }
        missiles.add(m);
    }

    private void updateEnemies(float dt) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.t += dt;
            e.y += e.speed * dt;
            e.x += (float)Math.sin(e.t * e.waveFreq) * e.waveAmp * dt;
            e.glowPulse = (float)Math.sin(e.t * 3) * 0.5f + 0.5f;
            e.fireTimer -= dt;
            if (e.fireTimer <= 0) {
                e.fireTimer = e.fireRate;
                spawnEnemyBullet(e);
            }
            if (e.y > H + 80) it.remove();
        }
    }

    private void spawnEnemyBullet(Enemy e) {
        float dx = player.x - e.x, dy = player.y - e.y;
        float len = (float)Math.sqrt(dx*dx+dy*dy);
        Bullet b = new Bullet();
        b.x = e.x; b.y = e.y;
        b.vx = dx/len * 500; b.vy = dy/len * 500;
        b.life = 3f; b.player = false; b.size = 12;
        eBullets.add(b);
    }

    private void updateBullets(float dt) {
        Iterator<Bullet> it = pBullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
            if (b.life <= 0 || b.y < -50) it.remove();
        }
        it = eBullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt;
            if (b.life <= 0 || b.y > H + 50) it.remove();
        }
    }

    private void updateMissiles(float dt) {
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            if (m.target != null && enemies.contains(m.target)) {
                float dx = m.target.x - m.x, dy = m.target.y - m.y;
                float len = (float)Math.sqrt(dx*dx+dy*dy);
                float tx = dx/len * 800, ty = dy/len * 800;
                m.vx += (tx - m.vx) * dt * 4;
                m.vy += (ty - m.vy) * dt * 4;
            }
            m.x += m.vx * dt; m.y += m.vy * dt; m.life -= dt;
            // Particle trail
            if (rand.nextInt(3) == 0) spawnParticle(m.x, m.y, 0xFFFF8800, 8, 0.4f);
            // Hit check
            boolean hit = false;
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                if (dist(m.x, m.y, e.x, e.y) < e.size + 20) {
                    explosion(e.x, e.y, 30);
                    eit.remove();
                    score += 200 * wave;
                    kills++;
                    waveKillCount++;
                    hit = true;
                    break;
                }
            }
            if (hit || m.life <= 0) it.remove();
        }
    }

    private void updateParticles(float dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx * dt; p.y += p.vy * dt;
            p.vx *= 0.94f; p.vy *= 0.94f;
            p.life -= dt;
            if (p.life <= 0) it.remove();
        }
    }

    private void checkCollisions() {
        // Player bullets vs enemies
        Iterator<Bullet> bit = pBullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                if (dist(b.x, b.y, e.x, e.y) < e.size + b.size) {
                    bit.remove();
                    e.hp -= 10;
                    spawnParticle(e.x, e.y, 0xFFFF8800, 6, 0.3f);
                    if (e.hp <= 0) {
                        explosion(e.x, e.y, 20);
                        eit.remove();
                        score += 100 * wave * (1 + combo / 3);
                        kills++;
                        combo++;
                        comboTimer = 3;
                        waveKillCount++;
                    }
                    break;
                }
            }
        }

        // Enemy bullets vs player
        Iterator<Bullet> ebit = eBullets.iterator();
        while (ebit.hasNext()) {
            Bullet b = ebit.next();
            if (dist(b.x, b.y, player.x, player.y) < 50) {
                ebit.remove();
                if (playerShield > 0) {
                    playerShield = Math.max(0, playerShield - 12);
                } else {
                    playerHP -= 10;
                    damageFlash = 1;
                }
                if (playerHP <= 0) { gameOver(); return; }
            }
        }

        // Enemies vs player
        Iterator<Enemy> eit = enemies.iterator();
        while (eit.hasNext()) {
            Enemy e = eit.next();
            if (dist(e.x, e.y, player.x, player.y) < e.size + 45) {
                explosion(e.x, e.y, 20);
                eit.remove();
                playerHP -= 25;
                damageFlash = 1;
                waveKillCount++;
                if (playerHP <= 0) { gameOver(); return; }
            }
        }
    }

    private void checkWave() {
        if (waveKillCount >= waveKillTarget) {
            wave++;
            waveKillCount = 0;
            waveKillTarget = 6 + wave * 2;
            spawnInterval = Math.max(0.5f, spawnInterval - 0.15f);
            waveAnnounceText = "WAVE " + wave;
            waveAnnounceTimer = 2.5f;
            playerHP = Math.min(100, playerHP + 20);
            playerShield = 50;
            missileAmmo = Math.min(5, missileAmmo + 1);
        }
    }

    private void spawnEnemy() {
        Enemy e = new Enemy();
        e.type = Math.min(rand.nextInt(wave + 1), 2);
        e.x = 80 + rand.nextFloat() * (W - 160);
        e.y = -80;
        e.speed = (120 + wave * 15 + rand.nextFloat() * 60);
        e.hp = e.maxHp = 20 + e.type * 15 + wave * 5;
        e.waveAmp = 80 + rand.nextFloat() * 80;
        e.waveFreq = 1 + rand.nextFloat() * 2;
        e.fireRate = Math.max(0.8f, 2.0f - wave * 0.1f);
        e.fireTimer = 0.5f + rand.nextFloat() * 1.5f;
        e.t = rand.nextFloat() * (float)Math.PI * 2;
        e.size = 50 + e.type * 15;
        enemies.add(e);
    }

    private void explosion(float x, float y, int count) {
        int[] colors = {0xFFFF6600, 0xFFFF4400, 0xFFFFAA00, 0xFFFFFF44};
        for (int i = 0; i < count; i++) {
            spawnParticle(x, y, colors[rand.nextInt(colors.length)],
                12 + rand.nextInt(15),
                0.5f + rand.nextFloat() * 0.5f);
        }
    }

    private void spawnParticle(float x, float y, int color, float size, float life) {
        Particle p = new Particle();
        p.x = x + (rand.nextFloat()-0.5f)*20;
        p.y = y + (rand.nextFloat()-0.5f)*20;
        float angle = rand.nextFloat() * (float)Math.PI * 2;
        float spd = 100 + rand.nextFloat() * 500;
        p.vx = (float)Math.cos(angle) * spd;
        p.vy = (float)Math.sin(angle) * spd;
        p.color = color;
        p.size = size;
        p.life = p.maxLife = life;
        particles.add(p);
    }

    private void startGame() {
        score = 0; wave = 1; kills = 0; combo = 0; comboTimer = 0;
        playerHP = 100; playerShield = 50; missileAmmo = 5;
        spawnTimer = 2; spawnInterval = 2.2f;
        waveKillCount = 0; waveKillTarget = 6;
        enemies.clear(); pBullets.clear(); eBullets.clear();
        missiles.clear(); particles.clear();
        player = new Player();
        player.x = W / 2f;
        player.y = H * 0.78f;
        waveAnnounceText = "WAVE 1";
        waveAnnounceTimer = 2f;
        state = STATE_PLAYING;
    }

    private void gameOver() {
        state = STATE_GAMEOVER;
    }

    // ── Render ────────────────────────────
    public void render(Canvas c) {
        // Background
        c.drawColor(Color.BLACK);

        // Stars
        for (Star s : stars) {
            starPaint.setAlpha((int)(s.alpha * 255));
            c.drawCircle(s.x, s.y, s.r, starPaint);
        }

        if (state == STATE_MENU) {
            renderMenu(c);
        } else if (state == STATE_PLAYING) {
            renderGame(c);
        } else {
            renderGame(c);
            renderGameOver(c);
        }
    }

    private void renderGame(Canvas c) {
        // Particles (behind everything)
        for (Particle p : particles) {
            particlePaint.setColor(p.color);
            particlePaint.setAlpha((int)(p.life / p.maxLife * 255));
            c.drawCircle(p.x, p.y, p.size * (p.life / p.maxLife + 0.5f), particlePaint);
        }

        // Enemies
        for (Enemy e : enemies) renderEnemy(c, e);

        // Missiles
        for (Missile m : missiles) {
            c.drawCircle(m.x, m.y, 10, missilePaint);
        }

        // Player bullets
        for (Bullet b : pBullets) {
            bulletPaint.setAlpha((int)(Math.min(b.life * 2, 1) * 255));
            c.drawOval(b.x - b.size*0.4f, b.y - b.size, b.x + b.size*0.4f, b.y + b.size, bulletPaint);
        }

        // Enemy bullets
        for (Bullet b : eBullets) {
            eBulletPaint.setAlpha((int)(Math.min(b.life, 1) * 255));
            c.drawOval(b.x - b.size*0.4f, b.y - b.size, b.x + b.size*0.4f, b.y + b.size, eBulletPaint);
        }

        // Player ship
        if (state == STATE_PLAYING) renderPlayer(c);

        // HUD
        renderHUD(c);

        // Mobile controls
        renderControls(c);

        // Damage flash
        if (damageFlash > 0) {
            Paint fp = new Paint();
            fp.setColor(Color.argb((int)(damageFlash * 100), 255, 0, 0));
            c.drawRect(0, 0, W, H, fp);
        }

        // Wave announce
        if (waveAnnounceTimer > 0) {
            float alpha = Math.min(waveAnnounceTimer * 2, 1);
            waveAnnouncePaint.setAlpha((int)(alpha * 255));
            c.drawText(waveAnnounceText, W / 2f, H * 0.35f, waveAnnouncePaint);
        }
    }

    private void renderPlayer(Canvas c) {
        c.save();
        c.translate(player.x, player.y);
        c.rotate(player.tiltZ);

        // Thruster glow
        float pulse = (float)Math.sin(System.currentTimeMillis() * 0.015) * 0.4f + 0.8f;
        thrusterPaint.setAlpha((int)(180 * pulse));
        c.drawCircle(0, 55, 22 * pulse, thrusterPaint);
        // Side thrusters
        c.drawCircle(-38, 40, 12 * pulse, thrusterPaint);
        c.drawCircle(38, 40, 12 * pulse, thrusterPaint);

        // Wings
        wingPaint.setColor(0xFF1a2a44);
        Path wingPath = new Path();
        wingPath.moveTo(0, -50);
        wingPath.lineTo(-120, 30);
        wingPath.lineTo(-80, 50);
        wingPath.lineTo(0, 20);
        wingPath.lineTo(80, 50);
        wingPath.lineTo(120, 30);
        wingPath.close();
        c.drawPath(wingPath, wingPaint);

        // Body
        playerPaint.setColor(0xFF2244AA);
        Path bodyPath = new Path();
        bodyPath.moveTo(0, -70);
        bodyPath.lineTo(-28, 20);
        bodyPath.lineTo(-20, 55);
        bodyPath.lineTo(20, 55);
        bodyPath.lineTo(28, 20);
        bodyPath.close();
        c.drawPath(bodyPath, playerPaint);

        // Cockpit
        playerPaint.setColor(0xFF44AAFF);
        c.drawOval(-16, -50, 16, -10, playerPaint);

        // Engine glow (cockpit light)
        Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowP.setColor(0xFF44AAFF);
        glowP.setAlpha(80);
        glowP.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(0, -30, 15, glowP);

        // Wing tips
        glowP.setColor(0xFFFF6600);
        glowP.setAlpha(160);
        c.drawCircle(-120, 30, 8, glowP);
        c.drawCircle(120, 30, 8, glowP);

        c.restore();
    }

    private void renderEnemy(Canvas c, Enemy e) {
        c.save();
        c.translate(e.x, e.y);
        float rot = (float)Math.sin(e.t) * 15;
        c.rotate(rot);

        int[] bodyColors = {0xFF442200, 0xFF330044, 0xFF003322};
        int[] glowColors = {0xFFFF2200, 0xFFAA00FF, 0xFF00FF88};
        enemyPaint.setColor(bodyColors[e.type]);
        enemyGlowPaint.setColor(glowColors[e.type]);
        enemyGlowPaint.setAlpha((int)(150 + e.glowPulse * 80));

        float s = e.size;
        if (e.type == 0) {
            // Saucer
            c.drawOval(-s, -s*0.3f, s, s*0.3f, enemyPaint);
            enemyPaint.setColor(adjustColor(bodyColors[0], 1.3f));
            c.drawOval(-s*0.5f, -s*0.5f, s*0.5f, s*0.5f, enemyPaint);
            c.drawOval(-s*0.3f, -s*0.3f, s*0.3f, s*0.3f, enemyGlowPaint);
        } else if (e.type == 1) {
            // Fighter
            Path p = new Path();
            p.moveTo(0, -s);
            p.lineTo(-s*0.8f, s*0.5f);
            p.lineTo(0, s*0.3f);
            p.lineTo(s*0.8f, s*0.5f);
            p.close();
            c.drawPath(p, enemyPaint);
            c.drawCircle(0, 0, s*0.25f, enemyGlowPaint);
        } else {
            // Gunship
            RectF r = new RectF(-s*0.4f, -s, s*0.4f, s);
            c.drawRoundRect(r, 15, 15, enemyPaint);
            RectF rw = new RectF(-s, -s*0.3f, s, s*0.3f);
            c.drawRoundRect(rw, 10, 10, enemyPaint);
            c.drawCircle(0, 0, s*0.22f, enemyGlowPaint);
        }

        // HP bar
        if (e.hp < e.maxHp) {
            float bw = s * 2;
            barBgPaint.setColor(0x44FFFFFF);
            c.drawRect(-bw/2, s+10, bw/2, s+18, barBgPaint);
            Paint hpP = new Paint();
            hpP.setColor(0xFF44FF44);
            c.drawRect(-bw/2, s+10, -bw/2 + bw*(e.hp/(float)e.maxHp), s+18, hpP);
        }

        c.restore();
    }

    private int adjustColor(int color, float factor) {
        int r = Math.min(255, (int)(Color.red(color) * factor));
        int g = Math.min(255, (int)(Color.green(color) * factor));
        int b = Math.min(255, (int)(Color.blue(color) * factor));
        return Color.rgb(r, g, b);
    }

    private void renderHUD(Canvas c) {
        float pad = 28;

        // Health bar
        hudLabelPaint.setColor(0xAAFF6B35);
        c.drawText("HULL", pad, 60, hudLabelPaint);
        barBgPaint.setColor(0x33FFFFFF);
        c.drawRoundRect(pad, 68, pad + 260, 82, 6, 6, barBgPaint);
        healthBarPaint.setColor(playerHP > 50 ? 0xFFFF4444 : playerHP > 25 ? 0xFFFF8800 : 0xFFFF2200);
        c.drawRoundRect(pad, 68, pad + 260 * (playerHP / 100f), 82, 6, 6, healthBarPaint);

        // Shield bar
        c.drawText("SHIELD", pad, 108, hudLabelPaint);
        c.drawRoundRect(pad, 116, pad + 260, 130, 6, 6, barBgPaint);
        c.drawRoundRect(pad, 116, pad + 260 * (playerShield / 50f), 130, 6, 6, shieldBarPaint);

        // Score
        hudPaint.setTextAlign(Paint.Align.CENTER);
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText("SCORE", W / 2f, 55, hudLabelPaint);
        c.drawText(String.valueOf(score), W / 2f, 105, hudPaint);

        // Wave
        hudPaint.setTextAlign(Paint.Align.RIGHT);
        hudLabelPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("WAVE", W - pad, 55, hudLabelPaint);
        c.drawText(String.valueOf(wave), W - pad, 105, hudPaint);

        // Missiles
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        c.drawText("MSL", pad, H * 0.75f - 30, hudLabelPaint);
        for (int i = 0; i < 5; i++) {
            Paint mp = new Paint(Paint.ANTI_ALIAS_FLAG);
            mp.setColor(i < missileAmmo ? 0xFFFF6B35 : 0x33FF6B35);
            mp.setStyle(Paint.Style.FILL);
            float mx = pad + i * 22;
            float my = H * 0.75f - 20;
            Path missPath = new Path();
            missPath.moveTo(mx + 6, my - 18);
            missPath.lineTo(mx + 12, my);
            missPath.lineTo(mx, my);
            missPath.close();
            c.drawPath(missPath, mp);
        }

        // Combo
        if (combo > 2) {
            Paint comboPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            comboPaint.setColor(0xFFFFFF44);
            comboPaint.setTextSize(48);
            comboPaint.setTextAlign(Paint.Align.CENTER);
            comboPaint.setFakeBoldText(true);
            c.drawText(combo + "x COMBO!", W / 2f, H * 0.2f, comboPaint);
        }

        hudPaint.setTextAlign(Paint.Align.CENTER);
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void renderControls(Canvas c) {
        // Joystick base
        joyBasePaint.setColor(0x22FF6B35);
        c.drawCircle(joyCX, joyCY, joyRadius, joyBasePaint);
        Paint borderP = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderP.setColor(0x66FF6B35);
        borderP.setStyle(Paint.Style.STROKE);
        borderP.setStrokeWidth(3);
        c.drawCircle(joyCX, joyCY, joyRadius, borderP);

        // Knob
        float knobX = joyCX + joyX * joyRadius;
        float knobY = joyCY + joyY * joyRadius;
        c.drawCircle(knobX, knobY, joyRadius * 0.35f, joyKnobPaint);

        // Fire button
        float fbX = W * 0.82f, fbY = H * 0.82f, fbR = W * 0.1f;
        fireBtnPaint.setColor(fireDown ? 0x88FF4444 : 0x44FF4444);
        c.drawCircle(fbX, fbY, fbR, fireBtnPaint);
        borderP.setColor(0xAAFF4444);
        c.drawCircle(fbX, fbY, fbR, borderP);
        fireBtnTextPaint.setColor(0xFFFF4444);
        c.drawText("FIRE", fbX, fbY + 12, fireBtnTextPaint);

        // Missile button
        float mbX = W * 0.65f, mbY = H * 0.84f, mbR = W * 0.07f;
        Paint mbp = new Paint(Paint.ANTI_ALIAS_FLAG);
        mbp.setColor(missileAmmo > 0 ? 0x44FF8800 : 0x22888888);
        mbp.setStyle(Paint.Style.FILL);
        c.drawCircle(mbX, mbY, mbR, mbp);
        borderP.setColor(missileAmmo > 0 ? 0x99FF8800 : 0x44888888);
        c.drawCircle(mbX, mbY, mbR, borderP);
        fireBtnTextPaint.setColor(missileAmmo > 0 ? 0xFFFF8800 : 0x66888888);
        fireBtnTextPaint.setTextSize(28);
        c.drawText("MSL", mbX, mbY + 10, fireBtnTextPaint);
        fireBtnTextPaint.setTextSize(36);
    }

    private void renderMenu(Canvas c) {
        // Nebula BG
        Paint nebula = new Paint(Paint.ANTI_ALIAS_FLAG);
        nebula.setShader(new RadialGradient(W/2f, H/2f, H*0.7f,
            new int[]{0x33FF4400, 0x11220066, 0x00000000},
            new float[]{0, 0.5f, 1},
            Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, nebula);

        c.drawText("VOID", W/2f, H*0.38f, menuTitlePaint);
        c.drawText("HUNTER", W/2f, H*0.47f, menuTitlePaint);
        c.drawText("3D SPACE COMBAT", W/2f, H*0.53f, menuSubPaint);

        // Button
        RectF btn = new RectF(W*0.25f, H*0.62f, W*0.75f, H*0.72f);
        Paint btnFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnFill.setColor(0x22FF6B35);
        c.drawRoundRect(btn, 8, 8, btnFill);
        c.drawRoundRect(btn, 8, 8, menuBtnPaint);
        c.drawText("LAUNCH MISSION", W/2f, H*0.685f, menuBtnTextPaint);

        menuSubPaint.setTextSize(28);
        c.drawText("TAP TO START", W/2f, H*0.8f, menuSubPaint);
        menuSubPaint.setTextSize(36);
    }

    private void renderGameOver(Canvas c) {
        Paint overlay = new Paint();
        overlay.setColor(0xCC000010);
        c.drawRect(0, 0, W, H, overlay);

        menuTitlePaint.setTextSize(80);
        c.drawText("SHIP", W/2f, H*0.3f, menuTitlePaint);
        c.drawText("DESTROYED", W/2f, H*0.39f, menuTitlePaint);
        menuTitlePaint.setTextSize(110);

        Paint statP = new Paint(Paint.ANTI_ALIAS_FLAG);
        statP.setColor(0xFFFFFFFF);
        statP.setTextSize(56);
        statP.setFakeBoldText(true);
        statP.setTextAlign(Paint.Align.CENTER);
        c.drawText(String.valueOf(score), W/2f - 180, H*0.52f, statP);
        c.drawText(String.valueOf(wave), W/2f, H*0.52f, statP);
        c.drawText(String.valueOf(kills), W/2f + 180, H*0.52f, statP);

        menuSubPaint.setTextSize(24);
        c.drawText("SCORE", W/2f - 180, H*0.56f, menuSubPaint);
        c.drawText("WAVE", W/2f, H*0.56f, menuSubPaint);
        c.drawText("KILLS", W/2f + 180, H*0.56f, menuSubPaint);
        menuSubPaint.setTextSize(36);

        RectF btn = new RectF(W*0.25f, H*0.63f, W*0.75f, H*0.73f);
        Paint btnFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnFill.setColor(0x22FF6B35);
        c.drawRoundRect(btn, 8, 8, btnFill);
        c.drawRoundRect(btn, 8, 8, menuBtnPaint);
        c.drawText("RETRY MISSION", W/2f, H*0.695f, menuBtnTextPaint);
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1-x2, dy = y1-y2;
        return (float)Math.sqrt(dx*dx+dy*dy);
    }
}
