package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;

public class AttackCard {
    public enum CardType { ATTACK, HEALING, MANA, SPECIAL }
    private CardType type;
    private String word;
    private int value;
    private float animTime;
    private boolean finished;
    private float x, y, scale, opacity;
    private float startX, startY, endX, endY;
    private float duration = 0.7f;
    private float displayDuration = 1.0f;
    private float fadeOutDuration = 0.5f;
    private int charsToShow = 0;
    private Runnable onExplode; // Callback khi card "nổ"
    private static BitmapFont FONT = new BitmapFont();

    // Card size
    private int cardWidth = 180;
    private int cardHeight = 80;

    // Textures cho từng loại card
    private static Texture texAttack;
    private static Texture texHeal;
    private static Texture texMana;
    private static Texture texSpecial;

    public static void setTextures(Texture attack, Texture heal, Texture mana, Texture special) {
        texAttack = attack;
        texHeal = heal;
        texMana = mana;
        texSpecial = special;
    }

    public AttackCard(CardType type, String word, int value, float startX, float startY, float endX, float endY) {
        this.type = type;
        this.word = word;
        this.value = value;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.animTime = 0f;
        this.scale = 0.8f;
        this.opacity = 0f;
        this.finished = false;
        this.x = startX;
        this.y = startY;
        this.onExplode = null;
    }

    public void setOnExplode(Runnable callback) {
        this.onExplode = callback;
    }

    public void update(float delta) {
        animTime += delta;

        // Đổi Interpolation.elasticOut thành Interpolation.smooth hoặc Interpolation.fade
        float t = Math.min(animTime / duration, 1f);
        float interp = com.badlogic.gdx.math.Interpolation.smooth.apply(t);
        x = startX + (endX - startX) * interp;
        y = startY + (endY - startY) * interp;

        scale = 0.8f + 0.2f * interp;
        opacity = Math.min(1f, t * 2);

        charsToShow = Math.min(word.length(), (int)(word.length() * t * 1.3f));

        if (animTime > duration + displayDuration) {
            float fadeT = Math.min((animTime - duration - displayDuration) / fadeOutDuration, 1f);
            opacity = 1f - fadeT;
            if (fadeT >= 0.5f && !finished) {
                finished = true;
                if (onExplode != null) onExplode.run();
            }
        }
    }

    // Gọi trong batch đã begin
    public void render(SpriteBatch batch) {
        if (opacity <= 0) return;
        float drawX = x;
        float drawY = y;
        float w = cardWidth * scale;
        float h = cardHeight * scale;

        // Chọn texture phù hợp
        Texture tex = getTextureForType();
        if (tex != null) {
            batch.setColor(1f, 1f, 1f, opacity);
            batch.draw(tex, drawX, drawY, w, h);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            // Nếu chưa có texture, vẽ nền màu
            batch.setColor(getTypeColor().r, getTypeColor().g, getTypeColor().b, 0.93f * opacity);
            batch.draw(getWhitePixel(), drawX, drawY, w, h);
            batch.setColor(1f,1f,1f,1f);
        }

        // Draw word (typewriter)
        FONT.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, opacity);
        FONT.getData().setScale(scale);
        String shown = word.substring(0, Math.max(0, charsToShow));
        FONT.draw(batch, shown, drawX + 18 * scale, drawY + h - 22 * scale);

        // Damage/heal number với hiệu ứng bounce nhẹ
        if (value != 0) {
            float impactT = Math.min(1f, animTime / 0.25f);
            float bounce = Interpolation.elasticOut.apply(impactT) * 16f * scale;
            FONT.setColor(Color.YELLOW.r, Color.YELLOW.g, 0f, opacity);
            FONT.getData().setScale(scale * 1.2f);
            FONT.draw(batch, (value > 0 ? "+" : "") + value, drawX + w/2 - 14*scale, drawY + 32*scale + bounce);
        }
        FONT.setColor(Color.WHITE);
        FONT.getData().setScale(1f);
    }

    public void playSFX() {
        // play SFX theo type nếu muốn (Gdx.audio.newSound,...)
    }
    public boolean isFinished() { return finished; }

    public static Color getTypeColor(CardType type) {
        switch (type) {
            case ATTACK: return new Color(0.8f,0.2f,0.2f,1f);
            case HEALING: return new Color(0.2f,0.8f,0.4f,1f);
            case MANA: return new Color(0.3f,0.5f,1.0f,1f);
            case SPECIAL: return new Color(0.7f,0.3f,0.9f,1f);
            default: return Color.LIGHT_GRAY;
        }
    }
    public Color getTypeColor() {
        return getTypeColor(type);
    }

    private Texture getTextureForType() {
        switch (type) {
            case ATTACK: return texAttack;
            case HEALING: return texHeal;
            case MANA: return texMana;
            case SPECIAL: return texSpecial;
            default: return null;
        }
    }

    // Helper: Texture 1x1 màu trắng
    private static Texture whitePix;
    private static Texture getWhitePixel() {
        if (whitePix == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1,1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(1,1,1,1);
            pm.fill();
            whitePix = new Texture(pm);
            pm.dispose();
        }
        return whitePix;
    }
}