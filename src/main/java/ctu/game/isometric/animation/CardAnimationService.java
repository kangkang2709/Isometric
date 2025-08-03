package ctu.game.isometric.animation;

import ctu.game.isometric.controller.EffectManager;

/**
 * Service class for handling all card animations in the game
 */
public class CardAnimationService {
    private final CardAnimationManager cardAnimationManager;
    private final EffectManager effectManager;

    public CardAnimationService(CardAnimationManager cardAnimationManager, EffectManager effectManager) {
        this.cardAnimationManager = cardAnimationManager;
        this.effectManager = effectManager;
    }

    public void playerAttack(String word, int dmg, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.ATTACK, word, dmg, 830, 600, 830, 600, 830, 470);
        Runnable extendedComplete = () -> {
            if (onComplete != null) onComplete.run();
        };
        card.setSFXCallback(effectManager::playAttackSound);
        card.setOnComplete(extendedComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerMiss(String word, int dmg, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.MISS, word, dmg, 316, 366, 316, 376, 316, 386);
        Runnable extendedComplete = () -> {
            if (onComplete != null) onComplete.run();
        };
        card.setSFXCallback(effectManager::playClickSound);
        card.setOnComplete(extendedComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerHealing(int heal, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.HEALING, "", heal, 316, 346, 316, 356, 316, 366);
        card.setSFXCallback(effectManager::playBuffSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerHealingMana(int mana, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.MANA, "", mana, 316, 346, 316, 356, 316, 366);
        card.setSFXCallback(effectManager::playBuffSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerBuff(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.SPECIAL, "", buff, 316, 346, 316, 356, 316, 366);
        card.setSFXCallback(effectManager::playBuffSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerToxic(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.POISON, "POSION", buff, 316, 336, 830, 470, 830, 450);
        card.setSFXCallback(effectManager::playPickSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void enemyToxic(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.POISON, "", buff, 830, 450, 800, 450, 316, 286);
        card.setSFXCallback(effectManager::playPickSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void enemyFire(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(AttackCard.CardType.FIRE, "", buff, 830, 450, 800, 450, 316, 286);
        card.setSFXCallback(effectManager::playClickSound);
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void enemyAttack(int dmg, int action, int heal, Runnable onComplete) {
        AttackCard card;
        if (dmg == 0) {
            card = new AttackCard(AttackCard.CardType.HEALING, "", heal, 830, 600, 830, 600, 830, 470);
            card.setSFXCallback(effectManager::playBuffSound);
        } else if (dmg < 0) {
            card = new AttackCard(AttackCard.CardType.MISS, "", 0, 840, 550, 840, 560, 840, 570);
            card.setSFXCallback(effectManager::playClickSound);
        } else {
            if (action < 8 && action > 4)
                card = new AttackCard(AttackCard.CardType.STRONG, "", dmg, 316, 416, 316, 416, 316, 286);
            else card = new AttackCard(AttackCard.CardType.ATTACK, "", dmg, 316, 416, 316, 416, 316, 286);
            card.setSFXCallback(effectManager::playAttackSound);
        }
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }
}