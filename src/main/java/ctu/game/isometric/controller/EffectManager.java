package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;
import java.util.Map;

public class EffectManager implements Disposable {
    private final ObjectMap<String, ParticleEffectPool> effectPools = new ObjectMap<>();
    private final Array<ParticleEffectPool.PooledEffect> activeEffects = new Array<>();
    private final ObjectMap<ParticleEffectPool.PooledEffect, EffectData> effectDataMap = new ObjectMap<>();
    private final Array<ScheduledEffect> scheduledEffects = new Array<>();
    private final String imageDir;
    Map<String, Sound> effectSound;

    public EffectManager(String imageDir) {
        this.imageDir = imageDir;
        this.effectSound = new HashMap<>();
        this.loadSoundEffects();
    }

    public void loadSoundEffects() {

    }

    public void loadSound(String effectName, String soundDir) {
        Sound sound = Gdx.audio.newSound(Gdx.files.internal(soundDir + "/" + effectName + ".ogg"));
        effectSound.put(effectName, sound);
    }

    public void loadEffect(String effectName, String effectPath) {
        try {
            ParticleEffect prototype = new ParticleEffect();
            prototype.load(Gdx.files.internal(effectPath), Gdx.files.internal(imageDir));
            effectPools.put(effectName, new ParticleEffectPool(prototype, 5, 30));
            loadSound(effectName, "effects/sound");
        } catch (Exception e) {
            Gdx.app.error("EffectManager", "Error loading effect: " + effectPath, e);
        }
    }

    // Spawn effect immediately
    public void spawnEffect(String effectName, float x, float y) {
        spawnEffect(effectName, x, y, 2.0f);
        playSound(effectName);
    }

    public void playSound(String effectName) {
        if (!sfxEnabled) return;
        Sound sound = effectSound.get(effectName);
        if (sound != null) {
            sound.play();
        } else {
            Gdx.app.debug("EffectManager", "Sound not found for effect: " + effectName);
        }
    }
    private boolean sfxEnabled = true;

    public void setEnabled(boolean enabled) {
        boolean wasDisabled = !this.sfxEnabled && enabled;
        this.sfxEnabled = enabled;
    }


    public void toggleSFX() {
        sfxEnabled = !sfxEnabled;
    }

    public void setSFXEnabled(boolean enabled) {
        sfxEnabled = enabled;
    }

    public boolean isSFXEnabled() {
        return sfxEnabled;
    }

    // Spawn effect with specific duration
    public void spawnEffect(String effectName, float x, float y, float duration) {
        ParticleEffectPool pool = effectPools.get(effectName);
        if (pool == null) {
            Gdx.app.error("EffectManager", "Effect not found: " + effectName);
            return;
        }

        ParticleEffectPool.PooledEffect effect = pool.obtain();
        effect.setPosition(x, y);
        effect.start();
        activeEffects.add(effect);
        effectDataMap.put(effect, new EffectData(duration));
    }



    // Schedule effect to appear after delay
    public void scheduleEffect(String effectName, float x, float y, float delay) {
        scheduleEffect(effectName, x, y, delay, -1);
    }

    // Schedule effect with delay and duration
    public void scheduleEffect(String effectName, float x, float y, float delay, float duration) {
        scheduledEffects.add(new ScheduledEffect(effectName, x, y, delay, duration));
    }

    // Stop all active effects
    public void stopAllEffects() {
        for (ParticleEffectPool.PooledEffect effect : activeEffects) {
            effect.allowCompletion();
        }
    }

    // Stop specific effect at position (nearest to x,y)
    public void stopEffectAt(float x, float y) {
        float minDist = Float.MAX_VALUE;
        ParticleEffectPool.PooledEffect targetEffect = null;

        for (ParticleEffectPool.PooledEffect effect : activeEffects) {
            float dx = effect.getEmitters().first().getX() - x;
            float dy = effect.getEmitters().first().getY() - y;
            float dist = dx * dx + dy * dy;

            if (dist < minDist) {
                minDist = dist;
                targetEffect = effect;
            }
        }

        if (targetEffect != null) {
            targetEffect.allowCompletion();
        }
    }

    public void update(float delta) {
        // Process scheduled effects
        for (int i = scheduledEffects.size - 1; i >= 0; i--) {
            ScheduledEffect scheduled = scheduledEffects.get(i);
            scheduled.delay -= delta;

            if (scheduled.delay <= 0) {
                spawnEffect(scheduled.effectName, scheduled.x, scheduled.y, scheduled.duration);
                scheduledEffects.removeIndex(i);
            }
        }

        // Update active effects
        for (int i = activeEffects.size - 1; i >= 0; i--) {
            ParticleEffectPool.PooledEffect effect = activeEffects.get(i);
            effect.update(delta);

            EffectData data = effectDataMap.get(effect);
            if (data.duration > 0) {
                data.timeActive += delta;
                if (data.timeActive >= data.duration) {
                    effect.allowCompletion();
                }
            }

            if (effect.isComplete()) {
                activeEffects.removeIndex(i);
                effectDataMap.remove(effect);
                effect.free();
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (ParticleEffectPool.PooledEffect effect : activeEffects) {
            effect.draw(batch);
        }
    }

    @Override
    public void dispose() {
        for (ParticleEffectPool.PooledEffect effect : activeEffects) {
            effect.free();
        }
        activeEffects.clear();
        effectDataMap.clear();
        scheduledEffects.clear();

        // Clear all effect pools
        for (ParticleEffectPool pool : effectPools.values()) {
            pool.clear();
        }
        effectPools.clear();
        for (Sound sound : effectSound.values()) {
            sound.dispose();
        }
        effectSound.clear();
    }

    // Helper classes for tracking effect data
    private static class EffectData {
        float duration;
        float timeActive = 0;

        EffectData(float duration) {
            this.duration = duration;
        }
    }

    private static class ScheduledEffect {
        String effectName;
        float x, y;
        float delay;
        float duration;

        ScheduledEffect(String effectName, float x, float y, float delay, float duration) {
            this.effectName = effectName;
            this.x = x;
            this.y = y;
            this.delay = delay;
            this.duration = duration;
        }
    }
}