package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import ctu.game.isometric.controller.quest.BountyBoardController;
import ctu.game.isometric.model.quest.Quest;
import ctu.game.isometric.model.quest.QuestReward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BountyBoardView {
    private BountyBoardController controller;
    private Texture backgroundTexture;
    private Texture questSlotTexture;
    private Texture buttonTexture;
    private BitmapFont titleFont;
    private BitmapFont normalFont;
    private GlyphLayout layout;

    private float boardX;
    private float boardY;
    private float boardWidth;
    private float boardHeight;
    private List<QuestSlot> questSlots;
    private Quest selectedQuest;
    private Matrix4 uiMatrix;

    public BountyBoardView(BountyBoardController controller) {
        this.controller = controller;
        this.questSlots = new ArrayList<>();

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("ui/bounty_board.png"));
        questSlotTexture = new Texture(Gdx.files.internal("ui/quest_slot.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));

        // Initialize fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(1.5f);
        titleFont.setColor(Color.GOLD);

        normalFont = new BitmapFont();
        normalFont.getData().setScale(1.0f);
        normalFont.setColor(Color.WHITE);

        layout = new GlyphLayout();

        // Set board position and dimensions
        boardWidth = 800;
        boardHeight = 600;
        boardX = (Gdx.graphics.getWidth() - boardWidth) / 2;
        boardY = (Gdx.graphics.getHeight() - boardHeight) / 2;

        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void render(SpriteBatch batch) {
        // Update quest slots
        updateQuestSlots();

        batch.setProjectionMatrix(uiMatrix);
        // Draw background
        batch.draw(backgroundTexture, boardX, boardY, boardWidth, boardHeight);

        // Draw title
        String title = "BOUNTY BOARD";
        layout.setText(titleFont, title);
        float titleX = boardX + (boardWidth - layout.width) / 2;
        float titleY = boardY + boardHeight - 30;
        titleFont.draw(batch, title, titleX, titleY);

        // Draw quest slots
        for (QuestSlot slot : questSlots) {
            batch.draw(questSlotTexture, slot.bounds.x, slot.bounds.y,
                    slot.bounds.width, slot.bounds.height);

            // Draw quest title
            normalFont.setColor(Color.GOLD);
            normalFont.draw(batch, slot.quest.getTitle(),
                    slot.bounds.x + 10, slot.bounds.y + slot.bounds.height - 10);

            // Draw quest description (shortened)
            normalFont.setColor(Color.WHITE);
            String description = slot.quest.getDescription();
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            normalFont.draw(batch, description,
                    slot.bounds.x + 10, slot.bounds.y + slot.bounds.height - 40);
        }

        // Draw selected quest details
        if (selectedQuest != null) {
            float detailX = boardX + 400;
            float detailY = boardY + boardHeight - 100;

            // Title
            titleFont.draw(batch, selectedQuest.getTitle(), detailX, detailY);

            // Description
            normalFont.draw(batch, selectedQuest.getDescription(),
                    detailX, detailY - 40, 350, -1, true);

            // Requirements
            normalFont.setColor(Color.YELLOW);
            normalFont.draw(batch, "Requirements:", detailX, detailY - 120);
            normalFont.setColor(Color.WHITE);

            int reqY = 0;
            for (Map.Entry<String, Integer> req : selectedQuest.getRequirements().entrySet()) {
                normalFont.draw(batch, "- " + req.getValue() + "x " + req.getKey(),
                        detailX + 10, detailY - 150 - (reqY * 20));
                reqY++;
            }

            // Rewards
            normalFont.setColor(Color.YELLOW);
            normalFont.draw(batch, "Rewards:", detailX, detailY - 180 - (reqY * 20));
            normalFont.setColor(Color.WHITE);

            QuestReward reward = selectedQuest.getReward();
            normalFont.draw(batch, "- XP: " + reward.getExperience(),
                    detailX + 10, detailY - 210 - (reqY * 20));
            normalFont.draw(batch, "- Gold: " + reward.getGold(),
                    detailX + 10, detailY - 230 - (reqY * 20));

            int rewardY = 0;
            for (Map.Entry<String, Integer> item : reward.getItems().entrySet()) {
                normalFont.draw(batch, "- " + item.getValue() + "x " + item.getKey(),
                        detailX + 10, detailY - 250 - (reqY * 20) - (rewardY * 20));
                rewardY++;
            }

            // Accept button
            float buttonX = detailX + 100;
            float buttonY = boardY + 50;
            batch.draw(buttonTexture, buttonX, buttonY, 150, 50);

            layout.setText(normalFont, "Accept Quest");
            normalFont.draw(batch, "Accept Quest",
                    buttonX + (150 - layout.width) / 2, buttonY + 30);
        }
    }

    private void updateQuestSlots() {
        List<Quest> availableQuests = controller.getAvailableQuests();
        questSlots.clear();

        float slotWidth = 350;
        float slotHeight = 80;
        float slotX = boardX + 20;
        float slotY = boardY + boardHeight - 100;

        for (int i = 0; i < availableQuests.size(); i++) {
            Quest quest = availableQuests.get(i);
            Rectangle bounds = new Rectangle(slotX, slotY - (i * (slotHeight + 10)), slotWidth, slotHeight);
            questSlots.add(new QuestSlot(quest, bounds));
        }
    }

    public boolean handleClick(float screenX, float screenY) {
        Vector2 pos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);

        // Check if a quest slot was clicked
        for (QuestSlot slot : questSlots) {
            if (slot.bounds.contains(pos)) {
                selectedQuest = slot.quest;
                return true;
            }
        }

        // Check if accept button was clicked
        if (selectedQuest != null) {
            float buttonX = boardX + 500;
            float buttonY = boardY + 50;
            Rectangle acceptButton = new Rectangle(buttonX, buttonY, 150, 50);

            if (acceptButton.contains(pos)) {
                controller.acceptQuest(selectedQuest.getId());
                selectedQuest = null;
                return true;
            }
        }

        return false;
    }

    public void dispose() {
        backgroundTexture.dispose();
        questSlotTexture.dispose();
        buttonTexture.dispose();
        titleFont.dispose();
        normalFont.dispose();
    }

    private static class QuestSlot {
        Quest quest;
        Rectangle bounds;

        public QuestSlot(Quest quest, Rectangle bounds) {
            this.quest = quest;
            this.bounds = bounds;
        }
    }
}