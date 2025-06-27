package ctu.game.isometric.view.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;

/**
 * Visual representation of spell cards that display words and damage values
 * with smooth entrance and exit animations
 */
public class AttackCard {
    public enum CardType {
        ATTACK(Color.valueOf("FF6B6BFF")),     // Red/Orange for attack
        HEALING(Color.valueOf("4ECDC4FF")),    // Green for healing  
        MANA(Color.valueOf("45B7D1FF")),       // Blue for mana
        SPECIAL(Color.valueOf("A855F7FF"));    // Purple for special effects
        
        public final Color color;
        
        CardType(Color color) {
            this.color = color;
        }
    }
    
    // Card properties
    private float x, y;
    private float targetX, targetY;
    private float startX, startY;
    private String word;
    private String damageText;
    private CardType cardType;
    
    // Animation state
    private float animationTime;
    private float duration;
    private boolean isComplete;
    
    // Animation phases
    private static final float SLIDE_IN_DURATION = 0.4f;
    private static final float DISPLAY_DURATION = 2.0f;
    private static final float FADE_OUT_DURATION = 0.6f;
    private static final float TOTAL_DURATION = SLIDE_IN_DURATION + DISPLAY_DURATION + FADE_OUT_DURATION;
    
    // Visual properties
    private float width = 200f;
    private float height = 120f;
    private float scale = 1f;
    private float alpha = 1f;
    
    // Text rendering
    private BitmapFont font;
    private BitmapFont damageFont;
    private GlyphLayout layout;
    private float typewriterProgress = 0f;
    
    public AttackCard(String word, String damageText, CardType cardType, float targetX, float targetY, BitmapFont font, BitmapFont damageFont) {
        this.word = word;
        this.damageText = damageText;
        this.cardType = cardType;
        this.targetX = targetX;
        this.targetY = targetY;
        this.font = font;
        this.damageFont = damageFont;
        this.layout = new GlyphLayout();
        
        // Start position (off-screen)
        this.startX = targetX - 300f; // Slide in from left
        this.startY = targetY;
        this.x = startX;
        this.y = startY;
        
        this.animationTime = 0f;
        this.duration = TOTAL_DURATION;
        this.isComplete = false;
    }
    
    public void update(float delta) {
        if (isComplete) return;
        
        animationTime += delta;
        
        if (animationTime >= duration) {
            isComplete = true;
            return;
        }
        
        float progress = animationTime / duration;
        
        if (animationTime <= SLIDE_IN_DURATION) {
            // Slide in phase
            float slideProgress = animationTime / SLIDE_IN_DURATION;
            x = Interpolation.pow2Out.apply(startX, targetX, slideProgress);
            scale = Interpolation.elasticOut.apply(0.3f, 1f, slideProgress);
            alpha = slideProgress;
            typewriterProgress = slideProgress; // Start typewriter effect during slide-in
        } else if (animationTime <= SLIDE_IN_DURATION + DISPLAY_DURATION) {
            // Display phase
            x = targetX;
            y = targetY;
            scale = 1f;
            alpha = 1f;
            typewriterProgress = 1f; // Show full text
        } else {
            // Fade out phase
            float fadeProgress = (animationTime - SLIDE_IN_DURATION - DISPLAY_DURATION) / FADE_OUT_DURATION;
            x = targetX;
            y = targetY;
            scale = Interpolation.pow2In.apply(1f, 1.2f, fadeProgress);
            alpha = 1f - fadeProgress;
            typewriterProgress = 1f;
        }
    }
    
    public void render(SpriteBatch batch, Texture cardTexture) {
        if (isComplete || alpha <= 0) return;
        
        Color originalColor = batch.getColor();
        
        // Draw card background with type-specific color and alpha
        Color cardColor = cardType.color.cpy();
        cardColor.a = alpha;
        batch.setColor(cardColor);
        
        float cardWidth = width * scale;
        float cardHeight = height * scale;
        float cardX = x - cardWidth / 2f;
        float cardY = y - cardHeight / 2f;
        
        // Draw card background (using white texture tinted with color)
        batch.draw(cardTexture, cardX, cardY, cardWidth, cardHeight);
        
        // Draw word text with typewriter effect
        if (font != null) {
            font.setColor(1f, 1f, 1f, alpha);
            // Calculate how much of the word to show based on typewriter progress
            int charsToShow = Math.min(word.length(), (int) Math.ceil(word.length() * typewriterProgress));
            String displayWord = word.substring(0, charsToShow);
            
            layout.setText(font, displayWord);
            float wordX = x - layout.width / 2f;
            float wordY = y + layout.height / 4f;
            font.draw(batch, displayWord, wordX, wordY);
        }
        
        // Draw damage/effect text with impact animation
        if (damageFont != null && damageText != null && !damageText.isEmpty()) {
            // Add a subtle bounce effect to damage numbers
            float damageScale = 1f;
            if (animationTime <= SLIDE_IN_DURATION + 0.3f) {
                float bounceProgress = Math.min(1f, (animationTime - SLIDE_IN_DURATION * 0.7f) / 0.3f);
                if (bounceProgress > 0) {
                    damageScale = 1f + Interpolation.elasticOut.apply(0f, 0.3f, bounceProgress);
                }
            }
            
            damageFont.setColor(1f, 1f, 0f, alpha); // Yellow damage numbers
            layout.setText(damageFont, damageText);
            float damageX = x - layout.width / 2f;
            float damageY = y - layout.height / 2f - 10f;
            
            // Apply scale transformation for impact effect
            if (damageScale != 1f) {
                damageFont.getData().setScale(damageScale);
                damageFont.draw(batch, damageText, damageX, damageY);
                damageFont.getData().setScale(1f); // Reset scale
            } else {
                damageFont.draw(batch, damageText, damageX, damageY);
            }
        }
        
        batch.setColor(originalColor);
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public String getWord() { return word; }
    public CardType getCardType() { return cardType; }
}