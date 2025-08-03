package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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

    private float dissolvePhase = 1.2f; // Duration of dissolve effect
    private boolean dissolveComplete = false;

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

        if (type == CardType.ATTACK || type == CardType.STRONG) {
            this.dissolvePhase = 1.2f;
        } else {
            this.dissolvePhase = 0f;
            this.dissolveComplete = true; // Đánh dấu là đã hoàn thành
            this.opacity = 1f; // Hiển thị ngay lập tức
        }
    }

    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }


    private float rotation = 0f;

    public void update(float delta) {
        animTime += delta;

        float phase0 = dissolvePhase; // Dissolve appear phase
        float phase1 = phase0 + toMidDuration;
        float phase2 = phase1 + impactDuration;
        float phase3 = phase2 + toEndDuration;
        float totalDuration = phase3 + displayDuration + fadeOutDuration;


        // Xử lý khi dissolvePhase = 0 (bỏ qua giai đoạn dissolve)
        if (dissolvePhase == 0 && animTime <= toMidDuration) {
            float t = animTime / toMidDuration;
            float interp = Interpolation.pow2Out.apply(t);
            x = startX + (midX - startX) * interp;
            y = startY + (midY - startY) * interp;
            scale = 0.85f + 0.17f * interp;
            opacity = 1f;

            float dx = midX - startX;
            float dy = midY - startY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));
        }
        // Xử lý dissolve phase nếu có
        else if (animTime <= phase0 && !dissolveComplete) {
            float t = animTime / dissolvePhase;
            x = startX;
            y = startY;
            scale = 0.85f;
            opacity = 1f;

            // Hiệu ứng xuất hiện từ bóng tối
            if (t < 0.6f) {
                // Hiệu ứng nhỏ lên khi xuất hiện
                scale = 0.7f + t * 0.25f;
            }
            if (t > 0.3f && t < 0.9f) {
                float vibration = (float) Math.sin(t * Math.PI * 16) * 2f * (1f - t) * 0.5f;
                x += vibration * (float) Math.cos(t * 25);
                y += vibration * 0.3f * (float) Math.sin(t * 25);
            }
            if (t >= 1f && !dissolveComplete) {
                dissolveComplete = true;
            }
        } else if (animTime <= phase0) {
            x = startX;
            y = startY;
            scale = 0.85f;
            opacity = 1f;
            dissolveComplete = true;
        }

        // A -> B (prepare for impact)
        else if (animTime <= phase1) {
            float t = (animTime - phase0) / toMidDuration;
            float interp = Interpolation.pow2Out.apply(t);
            x = startX + (midX - startX) * interp;
            y = startY + (midY - startY) * interp;
            scale = 0.85f + 0.17f * interp;
            opacity = 1f;

            float dx = midX - startX;
            float dy = midY - startY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));
        }
        // Rest of the phases remain the same, but adjust timing
        else if (animTime <= phase2) {
            float dx = midX - startX;
            float dy = midY - startY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));

            float t = (animTime - phase1) / impactDuration;
            x = midX;
            y = midY;
            float shake = (float) Math.sin(t * Math.PI * 12) * 5f * (1f - t);
            scale = 1.09f + 0.06f * (float) Math.sin(t * Math.PI * 8);
            x += shake;
            y += shake * 0.5f;
            opacity = 1f;
        } else if (animTime <= phase3) {
            float dx = endX - midX;
            float dy = endY - midY;
            rotation = (float) Math.toDegrees(Math.atan2(dy, dx));

            float t = (animTime - phase2) / toEndDuration;
            float interp = Interpolation.exp10In.apply(t);
            x = midX + (endX - midX) * interp;
            y = midY + (endY - midY) * interp;
            scale = 1.0f - 0.23f * interp;
            opacity = 1f - 0.25f * t;
        } else if (animTime <= totalDuration - fadeOutDuration) {
            if (!impactPlayed) {
                playImpactEffect();
                impactPlayed = true;
            }
            x = endX;
            y = endY;
            scale = 0.77f;
            opacity = 0.70f;
        } else {
            float t = (animTime - (totalDuration - fadeOutDuration)) / fadeOutDuration;
            opacity = Math.max(0f, 0.70f - t);
            if (!finished && opacity <= 0.05f) {
                finished = true;
                if (onComplete != null) onComplete.run();
            }
        }

        // Typewriter effect - start after dissolve
        float showT = Math.max(0f, Math.min(1f, (animTime - phase0) / 0.7f));
        charsToShow = Math.min(word.length(), (int) (word.length() * showT * 1.2f));

        // Update effects
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

        Texture tex = getTextureForType();
        float dissolveAmount = 0; // Declare dissolveAmount at the start

        if (animTime <= dissolvePhase && dissolvePhase > 0 && tex != null) {
            dissolveAmount = Interpolation.fade.apply(animTime / dissolvePhase);
            // Store original shader
            ShaderProgram originalShader = batch.getShader();

            // Apply dissolve shader
            batch.setShader(DissolveShaderManager.getDissolveShader());

            // Set shader uniforms
            ShaderProgram shader = batch.getShader();
            shader.setUniformf("u_dissolveAmount", dissolveAmount);
            shader.setUniformf("u_dissolveEdgeWidth", 0.35f);
            shader.setUniform3fv("u_dissolveEdgeColor", new float[]{1.0f, 0.3f, 0.1f}, 0, 3);
            shader.setUniformf("u_time", DissolveShaderManager.getShaderTime());
            shader.setUniformf("u_intensity", 2.0f);
            shader.setUniformf("u_edgeSharpness", 0.6f);
            shader.setUniform2fv("u_dissolveDirection", new float[]{0.0f, 1.0f}, 0, 2);

            // Bind noise texture
            DissolveShaderManager.getNoiseTexture().bind(1);
            shader.setUniformi("u_dissolveTexture", 1);

            // Bind burn texture if available
            Texture burnOverlay = DissolveShaderManager.getBurnTexture();
            if (burnOverlay != null) {
                burnOverlay.bind(2);
                shader.setUniformi("u_burnTexture", 2);
                shader.setUniformi("u_useBurnTexture", 1);
            } else {
                shader.setUniformi("u_useBurnTexture", 0);
            }

            // Bind main texture
            tex.bind(0);
            shader.setUniformi("u_texture", 0);

            // Draw with shader
            batch.setColor(1, 1, 1, opacity);
            batch.draw(
                    tex,
                    drawX + w / 2, drawY + h / 2,
                    w / 2, h / 2,
                    w, h,
                    1, 1,
                    rotation,
                    0, 0,
                    tex.getWidth(), tex.getHeight(),
                    false, false
            );

            // Restore original shader
            batch.setShader(originalShader);
        }
        // Normal rendering after dissolve
        else if (tex != null) {
            batch.setColor(1, 1, 1, opacity);
            batch.draw(
                    tex,
                    drawX + w / 2, drawY + h / 2,
                    w / 2, h / 2,
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

        // Render effects and text (rest remains the same)
        for (ImpactEffect effect : impactEffects)
            effect.render(batch);

        // Text rendering during and after dissolve
        FONT.getData().setScale(scale + 0.3f);
        String shown = word.substring(0, Math.max(0, charsToShow));
        float textX = drawX + 48 * scale;
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

    public void playSFX() {
        if (sfxCallback != null) {
            sfxCallback.run();
            System.out.println("Playing SFX for card: " + type + " with word: " + word);
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