package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class WeatherRenderer {
    public enum WeatherType {
        NONE, RAIN, SNOW, SUNNY, FOGGY
    }

    private WeatherType currentWeather = WeatherType.NONE;
    private float intensity = 1.0f;
    private OrthographicCamera camera;

    private ParticleEffect rainEffect;
    private ParticleEffect snowEffect;
    private ParticleEffectPool rainPool;
    private ParticleEffectPool snowPool;

    private ParticleEffectPool leafPool;

    private Array<PooledEffect> activeEffects = new Array<>();


    public void changeWeather(String type, float intensity) {
        setWeather(type, intensity);
    }

    public WeatherRenderer(OrthographicCamera camera) {
        this.camera = camera;

        // Initialize particle effects
        rainEffect = new ParticleEffect();
        rainEffect.load(Gdx.files.internal("effects/Rain/Rain.p"), Gdx.files.internal("effects/Rain/"));
        rainPool = new ParticleEffectPool(rainEffect, 5, 10);

//        snowEffect = new ParticleEffect();
//        snowEffect.load(Gdx.files.internal("effects/Leaf_fall/Leaf_fall.p"), Gdx.files.internal("effects/Leaf_fall/"));
//        snowPool = new ParticleEffectPool(snowEffect, 5, 10);

        snowEffect = new ParticleEffect();
        snowEffect.load(Gdx.files.internal("effects/Snow_fall/Snow_fall.p"), Gdx.files.internal("effects/Snow_fall/"));
        snowPool = new ParticleEffectPool(snowEffect, 5, 10);
    }

    public void update(float delta) {
        if (currentWeather == WeatherType.NONE) return;

        // Update active particle effects
        for (int i = activeEffects.size - 1; i >= 0; i--) {
            PooledEffect effect = activeEffects.get(i);
            effect.update(delta);

            // Reposition effect if it's moving off-screen
            updateEffectPosition(effect);

            // Remove completed effects
            if (effect.isComplete()) {
                effect.free();
                activeEffects.removeIndex(i);
            }
        }

        // Create new effects as needed
        if ((currentWeather == WeatherType.RAIN || currentWeather == WeatherType.SNOW)
                && activeEffects.size < 3) {
            createNewEffect();
        }
    }

    private void updateEffectPosition(PooledEffect effect) {
        float x = effect.getEmitters().first().getX();
        float y = effect.getEmitters().first().getY();

        float cameraX = camera.position.x;
        float cameraY = camera.position.y;
        float distanceX = Math.abs(x - cameraX);
        float distanceY = Math.abs(y - cameraY);

        if (distanceX > 400 || distanceY >  400) {
            x = cameraX + MathUtils.random(-camera.viewportWidth / 3, camera.viewportWidth / 3);
            y = cameraY + camera.viewportHeight / 2;
            if (currentWeather == WeatherType.SNOW) {
                y -= 150;
            }
            else y += 100;
            effect.setPosition(x, y);
        }
    }

    private void createNewEffect() {
        PooledEffect effect = null;

        if (currentWeather == WeatherType.RAIN) {
            effect = rainPool.obtain();
        } else if (currentWeather == WeatherType.SNOW) {
            effect = snowPool.obtain();
        }

        if (effect != null) {
            float x = camera.position.x + MathUtils.random(-camera.viewportWidth / 3, camera.viewportWidth / 3);
            float y = camera.position.y + camera.viewportHeight / 2;
            effect.setPosition(x, y);

            for (int i = 0; i < effect.getEmitters().size; i++) {
                effect.getEmitters().get(i).getEmission().setHigh((int) (20 * intensity));
                effect.getEmitters().get(i).getVelocity().setHigh(200 * intensity);
            }

            activeEffects.add(effect);
        }
    }

    public void render(SpriteBatch batch) {
        if (currentWeather == WeatherType.NONE) return;

        for (PooledEffect effect : activeEffects) {
            effect.draw(batch);
        }
    }

    public void setWeather(String type, float intensity) {
        this.intensity = MathUtils.clamp(intensity, 0.2f, 2.0f);

        if (!type.toLowerCase().equals(currentWeather.toString().toLowerCase())) {
            clearEffects();
        }

        switch (type.toLowerCase()) {
            case "rain":
                currentWeather = WeatherType.RAIN;
                break;
            case "snow":
                currentWeather = WeatherType.SNOW;
                break;
            case "foggy":
                currentWeather = WeatherType.FOGGY;
                break;
            case "sunny":
                currentWeather = WeatherType.SUNNY;
                break;
            default:
                currentWeather = WeatherType.NONE;
                clearEffects();
        }
    }

    private void clearEffects() {
        for (PooledEffect effect : activeEffects) {
            effect.free();
        }
        activeEffects.clear();
    }

    public void dispose() {
        clearEffects();

        if (rainEffect != null) {
            rainEffect.dispose();
        }
        if (snowEffect != null) {
            snowEffect.dispose();
        }
    }

    public WeatherType getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(WeatherType currentWeather) {
        this.currentWeather = currentWeather;
    }
}