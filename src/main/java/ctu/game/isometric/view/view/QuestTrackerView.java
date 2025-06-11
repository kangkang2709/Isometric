package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.quest.Quest;

import java.util.List;
import java.util.Map;

public class QuestTrackerView {
    private GameController gameController;
    private Texture backgroundTexture;
    private BitmapFont font;
    private GlyphLayout layout;
    private Rectangle closeButtonBounds;
    private boolean isVisible;
    private Quest selectedQuest;
    private Matrix4 uiMatrix;
    public QuestTrackerView(GameController gameController) {
        this.gameController = gameController;
        this.backgroundTexture = new Texture(Gdx.files.internal("ui/quest_tracker.png"));
        this.font = new BitmapFont();
        this.font.setColor(Color.WHITE);
        this.layout = new GlyphLayout();
        this.closeButtonBounds = new Rectangle(750, 550, 40, 40); // Example close button
        this.isVisible = false;
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void render(SpriteBatch batch) {
        if (!isVisible) return;

        // Draw background
        batch.setProjectionMatrix(uiMatrix);
        batch.draw(backgroundTexture, 100, 100, 800, 600);

        // Draw title
        font.getData().setScale(1.5f);
        font.setColor(Color.GOLD);
        layout.setText(font, "Quest Tracker");
        font.draw(batch, layout, 500 - layout.width / 2, 650);

        // Draw active quests
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        List<Quest> activeQuests = gameController.getCharacter().getQuestTracker().getActiveQuests();
        drawQuestList(batch, "Active Quests", activeQuests, 120, 550);

        // Draw completed quests
        List<Quest> completedQuests = gameController.getCharacter().getQuestTracker().getCompletedQuests();
        drawQuestList(batch, "Completed Quests", completedQuests, 120, 350);

//        // Draw available quests
//        List<Quest> availableQuests = gameController.getCharacter().getQuestTracker().getAvailableQuests();
//        drawQuestList(batch, "Available Quests", availableQuests, 120, 150);

        // Draw selected quest details
        if (selectedQuest != null) {
            drawQuestDetails(batch, selectedQuest, 500, 550);
        }

        // Draw close button
        font.setColor(Color.RED);
        font.draw(batch, "X", closeButtonBounds.x + 10, closeButtonBounds.y + 30);
    }

    private void drawQuestDetails(SpriteBatch batch, Quest quest, float x, float y) {
        font.setColor(Color.GOLD);
        font.draw(batch, "Quest Details", x, y);

        font.setColor(Color.YELLOW);
        font.draw(batch, "Title: " + quest.getTitle(), x, y - 30);
        font.setColor(Color.WHITE);
        font.draw(batch, "Description: " + quest.getDescription(), x, y - 60, 300, -1, true);

        font.setColor(Color.YELLOW);
        font.draw(batch, "Requirements:", x, y - 120);
        font.setColor(Color.WHITE);
        int offset = 0;
        for (Map.Entry<String, Integer> req : quest.getRequirements().entrySet()) {
            font.draw(batch, "- " + req.getValue() + "x " + req.getKey(), x + 20, y - 150 - offset);
            offset += 20;
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "Rewards:", x, y - 150 - offset);
        font.setColor(Color.WHITE);
        offset += 30;
        font.draw(batch, "- XP: " + quest.getReward().getExperience(), x + 20, y - 150 - offset);
        font.draw(batch, "- Gold: " + quest.getReward().getGold(), x + 20, y - 170 - offset);
    }
    private void drawQuestList(SpriteBatch batch, String title, List<Quest> quests, float x, float y) {
        font.setColor(Color.YELLOW);
        font.draw(batch, title, x, y);

        font.setColor(Color.WHITE);
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            font.draw(batch, "- " + quest.getTitle(), x + 20, y - (i + 1) * 20);
        }
    }

    public boolean handleClick(float screenX, float screenY) {
        if (!isVisible) return false;

        // Check if close button is clicked
        if (closeButtonBounds.contains(screenX, Gdx.graphics.getHeight() - screenY)) {
            gameController.returnToPreviousState();
            isVisible = false;
            return true;
        }

        // Check if a quest is clicked
        List<Quest> allQuests = gameController.getCharacter().getQuestTracker().getActiveQuests();
        allQuests.addAll(gameController.getCharacter().getQuestTracker().getCompletedQuests());

        float questY = 550; // Starting Y position for quests
        for (Quest quest : allQuests) {
            Rectangle questBounds = new Rectangle(120, questY - 20, 300, 20);
            if (questBounds.contains(screenX, Gdx.graphics.getHeight() - screenY)) {
                selectedQuest = quest;
                return true;
            }
            questY -= 30; // Adjust for next quest
        }

        return false;
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
    }

    public void dispose() {
        backgroundTexture.dispose();
        font.dispose();
    }
}