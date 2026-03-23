package com.voidhunter.game;

import java.util.ArrayList;

public class GameState {
    public static final int ST_MENU     = 0;
    public static final int ST_PLAYING  = 1;
    public static final int ST_SETTINGS = 2;
    public static final int ST_GAMEOVER = 3;
    public static final int ST_PAUSE    = 4;

    public int state = ST_MENU;
    public int score, wave, kills, combo, bestScore;
    public float comboTimer;
    public int hp=100, shield=50, missileAmmo=5;
    public float spawnTimer=2, spawnInterval=2.2f;
    public int waveKills, waveTarget=6;
    public float waveShowTimer;
    public float dmgFlash;
    public float time;
    public boolean godMode = false;

    // Settings
    public float musicVol  = 0.7f;
    public float sfxVol    = 1.0f;
    public int   difficulty = 1;  // 0=easy 1=normal 2=hard
    public boolean vibration = true;
    public float sensitivity = 1.0f;

    public ArrayList<Enemy>    enemies   = new ArrayList<>();
    public ArrayList<Projectile> pBullets= new ArrayList<>();
    public ArrayList<Projectile> eBullets= new ArrayList<>();
    public ArrayList<Missile>  missiles  = new ArrayList<>();
    public ArrayList<Particle> particles = new ArrayList<>();
    public ArrayList<PowerUp>  powerups  = new ArrayList<>();

    // Player
    public float px=0, py=0, pz=0;
    public float ptiltZ=0, ptiltX=0;
    public float pFireTimer=0;
    public float pInvincible=0;

    // ── Inner types ───────────────────────────────────────────────────────
    public static class Enemy {
        public float x,y,z, vx,vy,vz;
        public float t, waveAmp, waveFreq;
        public float speed, fireTimer, fireRate;
        public int hp, maxHp, type;
        public float rotY, rotSpeed;
        public float deathTimer = -1;
        public float size;
    }

    public static class Projectile {
        public float x,y,z, vx,vy,vz;
        public float life;
        public boolean player;
    }

    public static class Missile {
        public float x,y,z, vx,vy,vz;
        public float life;
        public Enemy target;
        public float trailTimer;
    }

    public static class Particle {
        public float x,y,z, vx,vy,vz;
        public float life, maxLife;
        public float r,g,b;
        public float size;
    }

    public static class PowerUp {
        public float x,y,z;
        public float vy=-2;
        public int type; // 0=health 1=shield 2=rapidfire 3=missile
        public float t;
    }
}
