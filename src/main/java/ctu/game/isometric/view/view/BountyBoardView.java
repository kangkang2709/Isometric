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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BountyBoardView {
    private BountyBoardController controller;
    private Texture backgroundTexture;
    private Texture questSlotTexture;
    private Texture buttonTexture;
    private Texture tabTexture;
    private Texture tabActiveTexture;
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

    // Quest view mode
    private enum QuestTab { AVAILABLE, ACTIVE, COMPLETED }
    private QuestTab currentTab = QuestTab.AVAILABLE;

    // UI Elements
    private Map<QuestTab, Rectangle> tabBounds = new HashMap<>();
    private Rectangle submitButton;
    private Rectangle acceptButton;

    private static final Color COLOR_AVAILABLE = Color.WHITE;
    private static final Color COLOR_IN_PROGRESS = new Color(0.2f, 0.6f, 1f, 1f);
    private static final Color COLOR_COMPLETED = new Color(0.2f, 0.8f, 0.2f, 1f);
    private static final Color COLOR_CLAIMED = new Color(0.5f, 0.5f, 0.5f, 1f);

    public BountyBoardView(BountyBoardController controller) {
        this.controller = controller;
        this.questSlots = new ArrayList<>();

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("ui/bounty_board.png"));
        questSlotTexture = new Texture(Gdx.files.internal("ui/quest_slot.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
        tabTexture = new Texture(Gdx.files.internal("ui/tab.png"));
        tabActiveTexture = new Texture(Gdx.files.internal("ui/tab_active.png"));

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

        // Create tab areas
        float tabWidth = 120;
        float tabHeight = 40;
        float tabY = boardY + boardHeight;
        tabBounds.put(QuestTab.AVAILABLE, new Rectangle(boardX + 20, tabY, tabWidth, tabHeight));
        tabBounds.put(QuestTab.ACTIVE, new Rectangle(boardX + 20 + tabWidth + 10, tabY, tabWidth, tabHeight));
        tabBounds.put(QuestTab.COMPLETED, new Rectangle(boardX + 20 + (tabWidth + 10) * 2, tabY, tabWidth, tabHeight));

        // Create buttons
        acceptButton = new Rectangle(boardX + 500, boardY + 50, 150, 50);
        submitButton = new Rectangle(boardX + 500, boardY + 110, 150, 50);

        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize quests on startup
        controller.reset();
    }

    public void render(SpriteBatch batch) {
        // Update quest slots based on current tab
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

        // Draw tabs
        for (Map.Entry<QuestTab, Rectangle> tab : tabBounds.entrySet()) {
            Texture tabTex = (tab.getKey() == currentTab) ? tabActiveTexture : tabTexture;
            Rectangle bounds = tab.getValue();
            batch.draw(tabTex, bounds.x, bounds.y, bounds.width, bounds.height);

            String tabText = tab.getKey().name();
            layout.setText(normalFont, tabText);
            float textX = bounds.x + (bounds.width - layout.width) / 2;
            float textY = bounds.y + bounds.height - 15;
            normalFont.draw(batch, tabText, textX, textY);
        }

        // Draw quest slots
        for (QuestSlot slot : questSlots) {
            batch.draw(questSlotTexture, slot.bounds.x, slot.bounds.y,
                    slot.bounds.width, slot.bounds.height);

            // Set color based on quest status
            switch (slot.quest.getStatus()) {
                case AVAILABLE:
                    normalFont.setColor(COLOR_AVAILABLE);
                    break;
                case IN_PROGRESS:
                    normalFont.setColor(COLOR_IN_PROGRESS);
                    break;
                case COMPLETED:
                    normalFont.setColor(COLOR_COMPLETED);
                    break;
                case CLAIMED:
                    normalFont.setColor(COLOR_CLAIMED);
                    break;
            }

            // Draw quest title with status indicator
            String statusText = " [" + slot.quest.getStatus().name() + "]";
            normalFont.draw(batch, slot.quest.getTitle() + statusText,
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

            // Title with status
            String titleWithStatus = selectedQuest.getTitle() + " [" + selectedQuest.getStatus() + "]";
            titleFont.draw(batch, titleWithStatus, detailX, detailY);

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

            // Draw accept button if quest is available
            if (selectedQuest.getStatus() == Quest.QuestStatus.AVAILABLE) {
                batch.draw(buttonTexture, acceptButton.x, acceptButton.y, acceptButton.width, acceptButton.height);
                layout.setText(normalFont, "Accept Quest");
                normalFont.draw(batch, "Accept Quest",
                        acceptButton.x + (acceptButton.width - layout.width) / 2,
                        acceptButton.y + 30);
            }


            if (selectedQuest.getStatus() == Quest.QuestStatus.IN_PROGRESS &&
                    controller.checkQuestCompletion(selectedQuest.getId())) {
                batch.draw(buttonTexture, submitButton.x, submitButton.y, submitButton.width, submitButton.height);
                layout.setText(normalFont, "Complete Quest");
                normalFont.draw(batch, "Complete Quest",
                        submitButton.x + (submitButton.width - layout.width) / 2,
                        submitButton.y + 30);
            }
//             Draw submit button if quest is completed
            if (selectedQuest.getStatus() == Quest.QuestStatus.COMPLETED) {
                batch.draw(buttonTexture, submitButton.x, submitButton.y, submitButton.width, submitButton.height);
                layout.setText(normalFont, "Submit Quest");
                normalFont.draw(batch, "Submit Quest",
                        submitButton.x + (submitButton.width - layout.width) / 2,
                        submitButton.y + 30);
            }
        }
    }

    private void updateQuestSlots() {
        List<Quest> quests;

        // Filter quests based on the current tab
        switch (currentTab) {
            case AVAILABLE:
                controller.updateQuestStatusFromQuestTracker(controller.getGameController().getCharacter().getQuestTracker());
                quests = controller.getAllQuests().values().stream()
                        .filter(quest -> quest.getStatus() == Quest.QuestStatus.AVAILABLE)
                        .toList();
                break;
            case ACTIVE:
                quests = controller.getActiveQuests();
                break;
            case COMPLETED:
                quests = controller.getCompletedQuests();
                break;
            default:
                quests = new ArrayList<>();
        }

        // Clear and populate quest slots
        questSlots.clear();
        float slotWidth = 350;
        float slotHeight = 80;
        float slotX = boardX + 20;
        float slotY = boardY + boardHeight - 100;

        for (int i = 0; i < quests.size(); i++) {
            float yOffset = slotY - (i * (slotHeight + 10));
            questSlots.add(new QuestSlot(quests.get(i), new Rectangle(slotX, yOffset, slotWidth, slotHeight)));
        }
    }

    public boolean handleClick(float screenX, float screenY) {
        Vector2 pos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);

        // Check tab clicks
        for (Map.Entry<QuestTab, Rectangle> tab : tabBounds.entrySet()) {
            if (tab.getValue().contains(pos)) {
                currentTab = tab.getKey();
                selectedQuest = null;
                return true;
            }
        }

        // Check if a quest slot was clicked
        for (QuestSlot slot : questSlots) {
            if (slot.bounds.contains(pos)) {
                selectedQuest = slot.quest;
                return true;
            }
        }

        // Check if accept button was clicked
        if (selectedQuest != null && selectedQuest.getStatus() == Quest.QuestStatus.AVAILABLE) {
            if (acceptButton.contains(pos)) {
                controller.acceptQuest(selectedQuest.getId());
                selectedQuest = null;
                return true;
            }
        }

        // Check if submit button was clicked
        if (selectedQuest != null && selectedQuest.getStatus() == Quest.QuestStatus.COMPLETED) {
            if (submitButton.contains(pos)) {
                controller.submitQuest(selectedQuest.getId());
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
        tabTexture.dispose();
        tabActiveTexture.dispose();
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