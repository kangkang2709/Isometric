package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

public class CardAnimationManager {
    private final Array<AttackCard> activeCards = new Array<>();

    public void addCard(AttackCard card) {
        activeCards.add(card);
        card.playSFX();
    }

    public void update(float delta) {
        for (int i = activeCards.size - 1; i >= 0; i--) {
            AttackCard card = activeCards.get(i);
            card.update(delta);
            if (card.isFinished()) {
                activeCards.removeIndex(i);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (AttackCard card : activeCards) {
            card.render(batch);
        }
    }

    public void clear() {
        activeCards.clear();
    }
}