package com.voidhunter.game;

import java.util.Iterator;
import java.util.Random;

public class GameLogic {
    private final GameState s;
    private final Random rand = new Random();
    public interface Callback { void onExplosion(float x,float y,float z,int big); }
    private final Callback cb;

    // Difficulty multipliers
    private static final float[] SPD = {0.6f, 1.0f, 1.5f};
    private static final float[] DMG = {0.5f, 1.0f, 1.8f};
    private static final float[] RATE= {1.4f, 1.0f, 0.6f};

    public GameLogic(GameState s, Callback cb) { this.s=s; this.cb=cb; }

    public void startGame() {
        s.score=0; s.wave=1; s.kills=0; s.combo=0; s.comboTimer=0;
        s.hp=100; s.shield=50; s.missileAmmo=5;
        s.spawnTimer=2; s.spawnInterval=2.2f;
        s.waveKills=0; s.waveTarget=6; s.waveShowTimer=2.5f;
        s.dmgFlash=0; s.time=0; s.pInvincible=0;
        s.px=0; s.py=0; s.pz=0;
        s.enemies.clear(); s.pBullets.clear(); s.eBullets.clear();
        s.missiles.clear(); s.particles.clear(); s.powerups.clear();
        s.state = GameState.ST_PLAYING;
    }

    public void update(float dt, float joyX, float joyY,
                       boolean fire, boolean missilePressed, boolean wasMissile) {
        if (s.state != GameState.ST_PLAYING) return;
        s.time += dt;

        updatePlayer(dt, joyX, joyY, fire, missilePressed, wasMissile);
        updateEnemies(dt);
        updateBullets(dt);
        updateMissiles(dt);
        updateParticles(dt);
        updatePowerUps(dt);
        spawnLogic(dt);
        checkCollisions();
        checkWave();

        if (s.dmgFlash > 0) s.dmgFlash -= dt * 3;
        if (s.comboTimer > 0) { s.comboTimer -= dt; if (s.comboTimer<=0) s.combo=0; }
        if (s.waveShowTimer > 0) s.waveShowTimer -= dt;
        if (s.pInvincible > 0) s.pInvincible -= dt;
    }

    private void updatePlayer(float dt, float jx, float jy,
                               boolean fire, boolean missile, boolean wasMissile) {
        float spd = 8f * s.sensitivity;
        s.px = clamp(s.px + jx*spd*dt, -7f, 7f);
        s.py = clamp(s.py - jy*spd*dt, -3.5f, 3.5f);
        s.ptiltZ = lerp(s.ptiltZ, -jx*0.4f, 8*dt);
        s.ptiltX = lerp(s.ptiltX,  jy*0.15f, 8*dt);

        s.pFireTimer -= dt;
        if (fire && s.pFireTimer <= 0) {
            s.pFireTimer = 0.1f;
            fireBullets();
        }
        if (missile && !wasMissile) launchMissile();

        if (s.shield < 50) s.shield = Math.min(50, (int)(s.shield + 6*dt));
    }

    private void fireBullets() {
        float[] ox = {-0.5f, 0.5f};
        for (float o : ox) {
            GameState.Projectile b = new GameState.Projectile();
            b.x=s.px+o; b.y=s.py; b.z=s.pz+1.5f;
            b.vz=28; b.vx=o*0.1f; b.life=2f; b.player=true;
            s.pBullets.add(b);
        }
    }

    private void launchMissile() {
        if (s.missileAmmo <= 0) return;
        s.missileAmmo--;
        GameState.Missile m = new GameState.Missile();
        m.x=s.px; m.y=s.py; m.z=s.pz+1;
        m.vz=15; m.life=5f;
        float minD = Float.MAX_VALUE;
        for (GameState.Enemy e : s.enemies) {
            float d = dist(m.x,m.y,m.z,e.x,e.y,e.z);
            if (d < minD) { minD=d; m.target=e; }
        }
        s.missiles.add(m);
    }

    private void updateEnemies(float dt) {
        Iterator<GameState.Enemy> it = s.enemies.iterator();
        while (it.hasNext()) {
            GameState.Enemy e = it.next();
            if (e.deathTimer >= 0) { e.deathTimer -= dt; if (e.deathTimer<0) it.remove(); continue; }
            e.t += dt;
            e.z -= e.speed * dt * SPD[s.difficulty];
            e.x += (float)Math.sin(e.t*e.waveFreq)*e.waveAmp*dt;
            e.rotY += e.rotSpeed * dt;
            e.fireTimer -= dt;
            if (e.fireTimer <= 0) {
                e.fireTimer = e.fireRate * RATE[s.difficulty];
                spawnEnemyBullet(e);
            }
            if (e.z < -15) it.remove();
        }
    }

    private void spawnEnemyBullet(GameState.Enemy e) {
        float dx=s.px-e.x, dy=s.py-e.y, dz=s.pz-e.z;
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (len < 0.001f) return;
        GameState.Projectile b = new GameState.Projectile();
        b.x=e.x; b.y=e.y; b.z=e.z;
        b.vx=dx/len*12; b.vy=dy/len*12; b.vz=dz/len*12;
        b.life=4f; b.player=false;
        s.eBullets.add(b);
    }

    private void updateBullets(float dt) {
        Iterator<GameState.Projectile> it = s.pBullets.iterator();
        while (it.hasNext()) { GameState.Projectile b=it.next();
            b.x+=b.vx*dt; b.y+=b.vy*dt; b.z+=b.vz*dt; b.life-=dt;
            if (b.life<=0||b.z>60) it.remove();
        }
        it = s.eBullets.iterator();
        while (it.hasNext()) { GameState.Projectile b=it.next();
            b.x+=b.vx*dt; b.y+=b.vy*dt; b.z+=b.vz*dt; b.life-=dt;
            if (b.life<=0) it.remove();
        }
    }

    private void updateMissiles(float dt) {
        Iterator<GameState.Missile> it = s.missiles.iterator();
        while (it.hasNext()) {
            GameState.Missile m = it.next();
            if (m.target != null && s.enemies.contains(m.target)) {
                float dx=m.target.x-m.x, dy=m.target.y-m.y, dz=m.target.z-m.z;
                float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
                if (len>0.1f) {
                    m.vx=lerp(m.vx,dx/len*20,dt*4);
                    m.vy=lerp(m.vy,dy/len*20,dt*4);
                    m.vz=lerp(m.vz,dz/len*20,dt*4);
                }
            }
            m.x+=m.vx*dt; m.y+=m.vy*dt; m.z+=m.vz*dt;
            m.life-=dt;
            m.trailTimer-=dt;
            if (m.trailTimer<=0) {
                m.trailTimer=0.04f;
                spawnParticle(m.x,m.y,m.z,1,0.5f,0,0.25f,4,0.6f);
            }
            boolean hit=false;
            Iterator<GameState.Enemy> eit=s.enemies.iterator();
            while (eit.hasNext()) {
                GameState.Enemy e=eit.next();
                if (e.deathTimer>=0) continue;
                if (dist(m.x,m.y,m.z,e.x,e.y,e.z)<e.size+0.5f) {
                    killEnemy(e,eit,true); hit=true; break;
                }
            }
            if (hit||m.life<=0) it.remove();
        }
    }

    private void updateParticles(float dt) {
        Iterator<GameState.Particle> it=s.particles.iterator();
        while (it.hasNext()) {
            GameState.Particle p=it.next();
            p.x+=p.vx*dt; p.y+=p.vy*dt; p.z+=p.vz*dt;
            p.vx*=0.93f; p.vy*=0.93f; p.vz*=0.93f;
            p.life-=dt;
            if (p.life<=0) it.remove();
        }
    }

    private void updatePowerUps(float dt) {
        Iterator<GameState.PowerUp> it=s.powerups.iterator();
        while (it.hasNext()) {
            GameState.PowerUp p=it.next();
            p.z+=p.vy*dt;
            p.t+=dt;
            if (p.z<-15) { it.remove(); continue; }
            if (dist(p.x,p.y,p.z,s.px,s.py,s.pz)<1.2f) {
                applyPowerUp(p); it.remove();
            }
        }
    }

    private void applyPowerUp(GameState.PowerUp p) {
        switch (p.type) {
            case 0: s.hp=Math.min(100,s.hp+30); break;
            case 1: s.shield=50; break;
            case 2: s.pFireTimer=-1; break; // brief rapid fire
            case 3: s.missileAmmo=Math.min(5,s.missileAmmo+2); break;
        }
    }

    private void spawnLogic(float dt) {
        s.spawnTimer-=dt;
        if (s.spawnTimer<=0) {
            s.spawnTimer=s.spawnInterval*(0.7f+rand.nextFloat()*0.6f);
            int count=Math.min(1+s.wave/3,4);
            for (int i=0;i<count;i++) spawnEnemy();
        }
    }

    private void spawnEnemy() {
        GameState.Enemy e=new GameState.Enemy();
        e.type=Math.min(rand.nextInt(s.wave+1),2);
        e.x=(rand.nextFloat()-0.5f)*14;
        e.y=(rand.nextFloat()-0.5f)*5;
        e.z=45+rand.nextFloat()*10;
        e.speed=3+s.wave*0.4f+rand.nextFloat()*1.5f;
        e.hp=e.maxHp=20+e.type*20+s.wave*8;
        e.waveAmp=1.5f+rand.nextFloat()*2;
        e.waveFreq=0.8f+rand.nextFloat()*1.5f;
        e.fireRate=Math.max(0.6f,2f-s.wave*0.08f);
        e.fireTimer=0.5f+rand.nextFloat()*2;
        e.t=rand.nextFloat()*(float)Math.PI*2;
        e.rotSpeed=(rand.nextFloat()-0.5f)*2;
        e.size=0.8f+e.type*0.3f;
        s.enemies.add(e);
    }

    private void checkCollisions() {
        // Player bullets vs enemies
        Iterator<GameState.Projectile> bit=s.pBullets.iterator();
        while (bit.hasNext()) {
            GameState.Projectile b=bit.next();
            Iterator<GameState.Enemy> eit=s.enemies.iterator();
            while (eit.hasNext()) {
                GameState.Enemy e=eit.next();
                if (e.deathTimer>=0) continue;
                if (dist(b.x,b.y,b.z,e.x,e.y,e.z)<e.size+0.15f) {
                    bit.remove();
                    e.hp-=(int)(10* DMG[s.difficulty]);
                    spawnParticle(e.x,e.y,e.z,1,0.5f,0,0.15f,5,0.4f);
                    if (e.hp<=0) killEnemy(e,eit,false);
                    break;
                }
            }
        }

        // Enemy bullets vs player
        if (s.pInvincible<=0) {
            Iterator<GameState.Projectile> ebit=s.eBullets.iterator();
            while (ebit.hasNext()) {
                GameState.Projectile b=ebit.next();
                if (dist(b.x,b.y,b.z,s.px,s.py,s.pz)<0.8f) {
                    ebit.remove();
                    hitPlayer((int)(8*DMG[s.difficulty]));
                }
            }
        }

        // Enemies vs player
        Iterator<GameState.Enemy> eit=s.enemies.iterator();
        while (eit.hasNext()) {
            GameState.Enemy e=eit.next();
            if (e.deathTimer>=0) continue;
            if (dist(e.x,e.y,e.z,s.px,s.py,s.pz)<e.size+0.5f) {
                killEnemy(e,eit,true);
                hitPlayer((int)(20*DMG[s.difficulty]));
            }
        }
    }

    private void hitPlayer(int dmg) {
        if (s.godMode) return;
        if (s.shield>0) { s.shield=Math.max(0,s.shield-dmg); }
        else { s.hp-=dmg; s.dmgFlash=1; s.pInvincible=0.5f; }
        if (s.hp<=0) { s.hp=0; s.state=GameState.ST_GAMEOVER; if(s.score>s.bestScore)s.bestScore=s.score; }
    }

    private void killEnemy(GameState.Enemy e, Iterator<GameState.Enemy> it, boolean big) {
        e.deathTimer=0.5f;
        cb.onExplosion(e.x,e.y,e.z,big?2:1);
        it.remove(); // we remove immediately even though deathTimer is set - fine
        s.score+=100*s.wave*(1+s.combo/3);
        s.kills++;
        s.combo++;
        s.comboTimer=3;
        s.waveKills++;
        // Chance to drop powerup
        if (rand.nextInt(5)==0) spawnPowerUp(e.x,e.y,e.z);
    }

    private void spawnPowerUp(float x,float y,float z) {
        GameState.PowerUp p=new GameState.PowerUp();
        p.x=x; p.y=y; p.z=z;
        p.type=rand.nextInt(4);
        s.powerups.add(p);
    }

    private void checkWave() {
        if (s.waveKills>=s.waveTarget) {
            s.wave++;
            s.waveKills=0;
            s.waveTarget=6+s.wave*2;
            s.spawnInterval=Math.max(0.4f,s.spawnInterval-0.12f);
            s.waveShowTimer=2.5f;
            s.hp=Math.min(100,s.hp+20);
            s.shield=50;
            s.missileAmmo=Math.min(5,s.missileAmmo+1);
        }
    }

    public void spawnParticle(float x,float y,float z,
                               float r,float g,float b,
                               float size,int count,float life) {
        for (int i=0;i<count;i++) {
            GameState.Particle p=new GameState.Particle();
            p.x=x+(rand.nextFloat()-0.5f)*0.3f;
            p.y=y+(rand.nextFloat()-0.5f)*0.3f;
            p.z=z+(rand.nextFloat()-0.5f)*0.3f;
            float spd=3+rand.nextFloat()*8;
            double a=rand.nextDouble()*Math.PI*2, el=(rand.nextDouble()-0.5)*Math.PI;
            p.vx=(float)(Math.cos(el)*Math.cos(a)*spd);
            p.vy=(float)(Math.sin(el)*spd);
            p.vz=(float)(Math.cos(el)*Math.sin(a)*spd);
            p.r=r; p.g=g; p.b=b;
            p.size=size*(0.5f+rand.nextFloat()*0.8f);
            p.life=p.maxLife=life*(0.5f+rand.nextFloat()*0.8f);
            s.particles.add(p);
        }
    }

    private float dist(float x1,float y1,float z1,float x2,float y2,float z2) {
        float dx=x1-x2,dy=y1-y2,dz=z1-z2;
        return (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
    }
    private float lerp(float a,float b,float t){return a+(b-a)*Math.min(t,1);}
    private float clamp(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
}
