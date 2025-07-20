package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;

import java.util.ArrayList;
import java.util.List;

public class AttackCard {
    public enum CardType {ATTACK, HEALING, MANA, SPECIAL, STRONG, SHIELD, MISS, POISON, FIRE}

    private CardType type;
    private String word;
    private int value;
    private float animTime;
    private boolean finished;
    private float x, y, scale, opacity;
    private float startX, startY, midX, midY, endX, endY;
    private float toMidDuration = 0.4f;     // Thời gian A->B
    private float impactDuration = 0.18f;   // Thời gian "va chạm" tại B
    private float toEndDuration = 0.3f;     // Thời gian B->C
    private float displayDuration = 0.7f;   // Giữ tại C
    private float fadeOutDuration = 0.4f;   // Fade out cuối
    private int charsToShow = 0;
    private Runnable onComplete;
    private boolean impactPlayed = false;
    private static BitmapFont FONT = new BitmapFont();

    // Card size
    private int cardWidth = 80;
    private int cardHeight = 120;


    // Textures cho từng loại card
    private static Texture texAttack;
    private static Texture texHeal;
    private static Texture texMana;
    private static Texture texSpecial;
    private static Texture glassCrackTexture; // Big crack texture
    private static Texture glassShardTexture; // Small shard texture
    private static Texture glowTexture;
    private static Texture missTexture;
    private static Texture poisonTexture;
    private static Texture fireTexture;
    private List<ImpactEffect> impactEffects = new ArrayList<>();


    public static void setTextures(Texture attack, Texture heal, Texture mana, Texture special, Texture crack, Texture shard, Texture glow, Texture miss, Texture poison, Texture fire) {
        texAttack = attack;
        texHeal = heal;
        texMana = mana;
        texSpecial = special;
        glassCrackTexture = crack;
        glassShardTexture = shard;
        glowTexture = glow;
        missTexture = miss;
        poisonTexture = poison;
        fireTexture = fire;
    }


    Runnable sfxCallback;

    public void setSFXCallback(Runnable callback) {
        this.sfxCallback = callback;
    }

    // Constructor: Truyền vào 3 điểm (A, B, C)
    public AttackCard(CardType type, String word, int value, float startX, float startY, float midX, float midY, float endX, float endY) {
        this.type = type;
        this.word = word;
        this.value = value;
        this.startX = startX;
        this.startY = startY;
        this.midX = midX;
        this.midY = midY;
        this.endX = endX;
        this.endY = endY;
        this.animTime = 0f;
        this.scale = 0.85f;
        this.opacity = 0f;
        this.finished = false;
        this.x = startX;
        this.y = startY;
        this.onComplete = null;
        this.impactPlayed = false;
        this.sfxCallback = null;
    }

    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }


    private float rotation = 0f;

    public void update(float delta) {
        animTime += delta;
        float phase1 = toMidDuration;
        float phase2 = phase1 + impactDuration;
        float phase3 = phase2 + toEndDuration;
        float totalDuration = phase3 + displayDuration + fadeOutDuration;

        // A -> B (chuẩn bị va chạm)
        if (animTime <= phase1) {
            float t = animTime / toMidDuration;
            float interp = Interpolation.pow2Out.apply(t);
            x = startX + (midX - startX) * interp;
            y = startY + (midY - startY) * interp;
            scale = 0.85f + 0.17f * interp; // bật nhẹ khi đến B
            opacity = Math.min(1f, t * 2f);

            float dx = midX - startX;
            float dy = midY - startY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));
        }
        // Hiệu ứng va chạm tại B (rung, pop, flash)
        else if (animTime <= phase2) {

            float dx = midX - startX;
            float dy = midY - startY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));

            float t = (animTime - phase1) / impactDuration;
            x = midX;
            y = midY;
            // Rung nhanh
            float shake = (float) Math.sin(t * Math.PI * 12) * 5f * (1f - t);
            scale = 1.09f + 0.06f * (float) Math.sin(t * Math.PI * 8);
            x += shake;
            y += shake * 0.5f;
            opacity = 1f;
        }
        // B -> C (lao nhanh, thu nhỏ, fade)
        else if (animTime <= phase3) {

            float dx = endX - midX;
            float dy = endY - midY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));

            float t = (animTime - phase2) / toEndDuration;
            float interp = Interpolation.exp10In.apply(t);
            x = midX + (endX - midX) * interp;
            y = midY + (endY - midY) * interp;
            scale = 1.0f - 0.23f * interp;
            opacity = 1f - 0.25f * t;
        }
        // Hiển thị tại điểm C rồi fade out
        else if (animTime <= totalDuration - fadeOutDuration) {
            if (!impactPlayed) {
                playImpactEffect();
                impactPlayed = true;
            }
            x = endX;
            y = endY;
            scale = 0.77f;
            opacity = 0.70f;
        }
        // Fade out cuối
        else {

            float t = (animTime - (totalDuration - fadeOutDuration)) / fadeOutDuration;
            opacity = Math.max(0f, 0.70f - t);
            if (!finished && opacity <= 0.05f) {
                finished = true;
                if (onComplete != null) onComplete.run();
            }
        }

        // Typewriter chữ
        float showT = Math.min(1f, animTime / 0.7f);
        charsToShow = Math.min(word.length(), (int) (word.length() * showT * 1.2f));

        // Update all effects (crack, shard, blood)
        for (int i = impactEffects.size() - 1; i >= 0; --i) {
            ImpactEffect effect = impactEffects.get(i);
            effect.update(delta);
            if (effect.isFinished()) impactEffects.remove(i);
        }
    }

    // Hiệu ứng va chạm: crack, shard, máu
    private void playImpactEffect() {
        playSFX();
        float centerX = endX + cardWidth * scale / 2;
        float centerY = endY + cardHeight * scale / 2;
        float width = cardWidth * scale;
        float height = cardHeight * scale;

        switch (type) {
            case ATTACK:
            case STRONG:
                impactEffects.add(new BloodSplatterEffect(centerX, centerY, getBloodPixel()));
                impactEffects.add(new GlassCrackEffect(endX, endY, width, height, glassCrackTexture, rotation));
                if (type == CardType.STRONG) {
                    impactEffects.add(new HealGlowEffect(endX, endY, width, height, glassShardTexture));
                }
                break;
            case POISON:
            case MISS:
            case SPECIAL:
            case MANA:
            case HEALING:
                impactEffects.add(new HealGlowEffect(endX, endY, width, height, glowTexture));
                break;
            case FIRE:
                impactEffects.add(new HealGlowEffect(endX, endY, width / scale, height / scale, glowTexture));
                break;
        }
    }


    public void render(SpriteBatch batch) {
        if (opacity <= 0) return;
        float drawX = x;
        float drawY = y;
        float w = cardWidth * scale;
        float h = cardHeight * scale;

        // Draw card bg with glow border
        Texture tex = getTextureForType();
        if (tex != null) {
            batch.setColor(1, 1, 1, opacity);
            batch.draw(
                    tex,
                    drawX + w / 2, drawY + h / 2, // center position
                    w / 2, h / 2,                 // origin
                    w, h,
                    1, 1,
                    rotation,
                    0, 0,
                    tex.getWidth(), tex.getHeight(),
                    false, false
            );
        } else {
            batch.setColor(getTypeColor().r, getTypeColor().g, getTypeColor().b, 0.98f * opacity);
            batch.draw(getWhitePixel(), drawX, drawY, w, h);
        }

        // Draw effects
        for (ImpactEffect effect : impactEffects)
            effect.render(batch);
        // No batch transform, just draw text
        FONT.getData().setScale(scale + 0.3f);
        String shown = word.substring(0, Math.max(0, charsToShow));
        float textX = drawX + 68 * scale;
        float textY = drawY + h + 20 * scale;
        // Glow effect
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                if (dx * dx + dy * dy != 0 && Math.abs(dx) + Math.abs(dy) <= 2) {
                    FONT.setColor(0.08f, 0.85f, 1f, 0.28f * opacity);
                    FONT.draw(batch, shown, textX + dx - 3, textY + dy - 3);
                }
        // Main text
        FONT.setColor(0.5f, 1f, 1f, opacity);
        FONT.draw(batch, shown, textX - 3, textY - 3);

        // Value pop
        if (value != 0) {
            float impactT = Math.min(1f, animTime / 0.18f);
            float bounce = Interpolation.sineOut.apply(impactT) * 13f * scale;
            float glowAlpha = 0.7f * opacity * (1f - Math.abs(impactT - 0.5f) * 2f);
            Color color = value > 0 ? new Color(1f, 0.95f, 0.2f, glowAlpha) : new Color(0.2f, 1f, 0.4f, glowAlpha);

            float valX = drawX + w / 2 - 18 * scale;
            float valY = drawY + 28 * scale + bounce;
            FONT.getData().setScale(scale * 1.25f);

            // Glow around value
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    if (dx * dx + dy * dy != 0 && Math.abs(dx) + Math.abs(dy) <= 2) {
                        FONT.setColor(color.r, color.g, color.b, 0.25f * glowAlpha);
                        FONT.draw(batch, (value > 0 ? "+" : "") + value, valX + dx, valY + dy);
                    }
            // Main value
            FONT.setColor(color);
            FONT.draw(batch, (value > 0 ? "+" : "") + value, valX, valY);
        }

        FONT.setColor(Color.WHITE);
        FONT.getData().setScale(1f);
    }


    private static float[] rotatePoint(float px, float py, float cx, float cy, float angleDeg) {
        double angleRad = Math.toRadians(angleDeg);
        float dx = px - cx;
        float dy = py - cy;
        float rx = (float) (dx * Math.cos(angleRad) - dy * Math.sin(angleRad)) + cx;
        float ry = (float) (dx * Math.sin(angleRad) + dy * Math.cos(angleRad)) + cy;
        return new float[]{rx, ry};
    }

    public void playSFX() {
        if (sfxCallback != null) {
            sfxCallback.run();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public static Color getTypeColor(CardType type) {
        switch (type) {
            case ATTACK:
                return new Color(0.8f, 0.2f, 0.2f, 1f);
            case HEALING:
                return new Color(0.2f, 0.8f, 0.4f, 1f);
            case MANA:
                return new Color(0.3f, 0.5f, 1.0f, 1f);
            case SPECIAL:
                return new Color(0.7f, 0.3f, 0.9f, 1f);
            default:
                return Color.LIGHT_GRAY;
        }
    }

    public Color getTypeColor() {
        return getTypeColor(type);
    }

    private Texture getTextureForType() {
        switch (type) {
            case ATTACK, STRONG, SHIELD:
                return texAttack;
            case HEALING:
                return texHeal;
            case MANA:
                return texMana;
            case SPECIAL:
                return texSpecial;
            case MISS:
                return missTexture;
            case POISON:
                return poisonTexture;
            case FIRE:
                return fireTexture;
            default:
                return null;
        }
    }

    // Helper: Texture 1x1 màu trắng
    private static Texture whitePix;

    private static Texture getWhitePixel() {
        if (whitePix == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(1, 1, 1, 1);
            pm.fill();
            whitePix = new Texture(pm);
            pm.dispose();
        }
        return whitePix;
    }

    // Helper: Texture máu đỏ 1x1
    private static Texture bloodPix;

    private static Texture getBloodPixel() {
        if (bloodPix == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(0, 0.8f, 0, 1); // Màu xanh lá cây
            pm.fill();
            bloodPix = new Texture(pm);
            pm.dispose();
        }
        return bloodPix;
    }


}