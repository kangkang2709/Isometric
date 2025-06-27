package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import ctu.game.isometric.view.animation.AttackCard;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages card-based animations for attacks, healing, and other effects
 * Replaces the particle-based EffectManager for attack animations
 */
public class CardAnimationManager implements Disposable {
    private final Array<AttackCard> activeCards = new Array<>();
    private Map<String, Sound> cardSounds;
    private boolean sfxEnabled = true;
    
    // Fonts for rendering text on cards
    private BitmapFont cardFont;
    private BitmapFont damageFont;
    
    // Card texture (white texture that can be tinted)
    private Texture cardTexture;
    
    public CardAnimationManager() {
        this.cardSounds = new HashMap<>();
        initializeAssets();
        loadCardSounds();
    }
    
    private void initializeAssets() {
        // Create a simple white texture for the card background
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        cardTexture = new Texture(pixmap);
        pixmap.dispose();
    }
    
    private void loadCardSounds() {
        try {
            loadSound("attack", "audio/effects");
            loadSound("healing", "audio/effects");
            loadSound("magic", "audio/effects");
            // Fallback to existing sounds
            loadSound("Starlight", "effects/sound");
        } catch (Exception e) {
            Gdx.app.debug("CardAnimationManager", "Some card sounds could not be loaded: " + e.getMessage());
        }
    }
    
    public void loadSound(String effectName, String soundDir) {
        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal(soundDir + "/" + effectName + ".ogg"));
            cardSounds.put(effectName, sound);
        } catch (Exception e) {
            Gdx.app.debug("CardAnimationManager", "Could not load sound: " + soundDir + "/" + effectName + ".ogg");
        }
    }
    
    /**
     * Set fonts for rendering text on cards
     */
    public void setFonts(BitmapFont cardFont, BitmapFont damageFont) {
        this.cardFont = cardFont;
        this.damageFont = damageFont;
    }
    
    /**
     * Spawn a card animation for an attack effect
     * @param word The word that triggered the attack
     * @param damage The damage amount to display
     * @param x X position for the card
     * @param y Y position for the card
     */
    public void spawnAttackCard(String word, int damage, float x, float y) {
        spawnCard(word, "+" + damage + " DMG", AttackCard.CardType.ATTACK, x, y);
        playSound("attack");
    }
    
    /**
     * Spawn a card animation for a healing effect
     * @param word The word that triggered the healing
     * @param healing The healing amount to display
     * @param x X position for the card
     * @param y Y position for the card
     */
    public void spawnHealingCard(String word, int healing, float x, float y) {
        spawnCard(word, "+" + healing + " HP", AttackCard.CardType.HEALING, x, y);
        playSound("healing");
    }
    
    /**
     * Spawn a card animation for a mana effect
     * @param word The word that triggered the mana effect
     * @param mana The mana amount to display
     * @param x X position for the card
     * @param y Y position for the card
     */
    public void spawnManaCard(String word, int mana, float x, float y) {
        spawnCard(word, "+" + mana + " MP", AttackCard.CardType.MANA, x, y);
        playSound("magic");
    }
    
    /**
     * Spawn a card animation for a special effect
     * @param word The word that triggered the special effect
     * @param effect The effect description to display
     * @param x X position for the card
     * @param y Y position for the card
     */
    public void spawnSpecialCard(String word, String effect, float x, float y) {
        spawnCard(word, effect, AttackCard.CardType.SPECIAL, x, y);
        playSound("magic");
    }
    
    /**
     * Generic method to spawn a card with specific type and text
     */
    private void spawnCard(String word, String effectText, AttackCard.CardType cardType, float x, float y) {
        AttackCard card = new AttackCard(word, effectText, cardType, x, y, cardFont, damageFont);
        activeCards.add(card);
    }
    
    /**
     * Compatibility method to replace effectManager.spawnEffect calls
     * @param effectName The effect name (used for sound)
     * @param x X position
     * @param y Y position
     */
    public void spawnEffect(String effectName, float x, float y) {
        // Create a generic card for compatibility
        spawnCard("ATTACK", "Effect!", AttackCard.CardType.ATTACK, x, y);
        playSound(effectName);
    }
    
    private void playSound(String soundName) {
        if (!sfxEnabled) return;
        
        Sound sound = cardSounds.get(soundName);
        if (sound != null) {
            sound.play();
        } else {
            // Fallback to Starlight sound if specific sound not found
            Sound fallback = cardSounds.get("Starlight");
            if (fallback != null) {
                fallback.play();
            }
        }
    }
    
    public void update(float delta) {
        // Update all active cards
        for (int i = activeCards.size - 1; i >= 0; i--) {
            AttackCard card = activeCards.get(i);
            card.update(delta);
            
            if (card.isComplete()) {
                activeCards.removeIndex(i);
            }
        }
    }
    
    public void render(SpriteBatch batch) {
        if (!batch.isDrawing()) return;
        
        for (AttackCard card : activeCards) {
            card.render(batch, cardTexture);
        }
    }
    
    public void setSFXEnabled(boolean enabled) {
        this.sfxEnabled = enabled;
    }
    
    public boolean isSFXEnabled() {
        return sfxEnabled;
    }
    
    public void toggleSFX() {
        sfxEnabled = !sfxEnabled;
    }
    
    /**
     * Stop all active card animations
     */
    public void stopAllCards() {
        activeCards.clear();
    }
    
    @Override
    public void dispose() {
        activeCards.clear();
        
        if (cardTexture != null) {
            cardTexture.dispose();
        }
        
        for (Sound sound : cardSounds.values()) {
            sound.dispose();
        }
        cardSounds.clear();
    }
}