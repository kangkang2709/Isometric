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
import org.w3c.dom.ls.LSException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class QuestTrackerView {
    private GameController gameController;
    private Texture backgroundTexture;
    private Texture arrowTexture;
    private BitmapFont font;
    private BitmapFont smallFont;
    private GlyphLayout layout;
    private Rectangle closeButtonBounds;
    private boolean isVisible;
    private Quest selectedQuest;
    private Matrix4 uiMatrix;

    // Constants for positioning and scroll behavior
// Constants for positioning and scroll behavior
    private final float QUEST_LIST_Y = 510;
    private final float QUEST_ITEM_HEIGHT = 65f;
    private final float QUEST_ITEM_WIDTH = 350f;
    private float scrollOffset = 0;
    private final float SCROLL_AMOUNT = 40;
    private final int MAX_VISIBLE_QUESTS = 5;

    // Scroll button rectangles
    private Rectangle scrollUpButton;
    private Rectangle scrollDownButton;


    public QuestTrackerView(GameController gameController) {
        this.gameController = gameController;
        this.backgroundTexture = new Texture(Gdx.files.internal("ui/quest_tracker.png"));
        this.arrowTexture = new Texture(Gdx.files.internal("ui/arrow.png"));

        this.font = gameController.getCommonFont();
        this.smallFont = generateVietNameseFont("IMFellEnglishSC-Regular.ttf", 16);

        this.font.setColor(Color.WHITE);
        this.layout = new GlyphLayout();
        this.closeButtonBounds = new Rectangle(1050, 580, 40, 40);
        this.scrollUpButton = new Rectangle(300, QUEST_LIST_Y - 10, 30, 30);
        this.scrollDownButton = new Rectangle(300, QUEST_LIST_Y - 300, 30, 30);
        this.isVisible = false;
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    List<Quest> activeQuests;

    public void render(SpriteBatch batch) {
        if (!isVisible) return;

        // Draw background
        batch.setProjectionMatrix(uiMatrix);
        batch.draw(backgroundTexture, 270, 50, 800, 600);

        // Draw title
//        font.getData().setScale(1.5f);
//        font.setColor(Color.GOLD);
//        layout.setText(font, "Quest Tracker");
//        font.draw(batch, layout, 500 - layout.width / 2, 650);

        // Draw active quests only
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);

        drawQuestList(batch, "Active Quests", activeQuests, 300, QUEST_LIST_Y + 20);

        // Draw selected quest details
        if (selectedQuest != null) {
            drawQuestDetails(batch, selectedQuest, 720, QUEST_LIST_Y);
        }

        // Draw close button
        font.setColor(Color.RED);
        font.draw(batch, "X", closeButtonBounds.x + 10, closeButtonBounds.y + 30);
    }

    private void drawQuestDetails(SpriteBatch batch, Quest quest, float x, float y) {
        font.setColor(Color.GOLD);
        font.draw(batch, "Quest Details", x, y + 20);

        font.setColor(Color.YELLOW);
        font.draw(batch, "Title: " + quest.getTitle(), x, y - 30);
        font.setColor(Color.WHITE);

        // Cache processed description to avoid repeated processing
        String processedDescription = getProcessedDescription(quest);
        font.draw(batch, "Description: " + processedDescription, x, y - 60, 300, -1, true);

        font.setColor(Color.YELLOW);
        font.draw(batch, "Requirements:", x, y - 140);
        font.setColor(Color.WHITE);
        int offset = 20;

        // Add null check for requirements
        if (quest.getRequirements() != null) {
            for (Map.Entry<String, Integer> req : quest.getRequirements().entrySet()) {
                font.draw(batch, "- " + req.getValue() + "x " + req.getKey(), x + 40, y - 150 - offset);
                offset += 20;
            }
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "Rewards:", x, y - 160 - offset);
        font.setColor(Color.WHITE);
        offset += 50;

        // Add null check for reward
        if (quest.getReward() != null) {
            font.draw(batch, "- XP: " + quest.getReward().getExperience(), x + 20, y - 150 - offset);
            font.draw(batch, "- Gold: " + quest.getReward().getGold(), x + 20, y - 180 - offset);
            if (quest.getReward().getItems() != null && !quest.getReward().getItems().isEmpty()) {
                for (Map.Entry<String, Integer> item : quest.getReward().getItems().entrySet()) {
                    font.draw(batch, "- " + item.getValue() + "x " + item.getKey(), x + 20, y - 190 - offset);
                    offset += 20;
                }
            }
        }
    }

    // Add this helper method to cache processed descriptions
    private String getProcessedDescription(Quest quest) {
        // Simple caching mechanism - you could implement a more sophisticated cache
        if (quest.getDescription() == null) {
            return "";
        }

        // This could be enhanced with a proper cache (Map<Quest, String>) if needed
        return Arrays.stream(quest.getDescription().split("\n"))
                .filter(line -> !line.startsWith("Condition:"))
                .collect(Collectors.joining("\n"));
    }

    private void drawQuestList(SpriteBatch batch, String title, List<Quest> quests, float x, float y) {
        // Draw section title with underline
        font.setColor(Color.GOLD);
        layout.setText(font, title);

        font.draw(batch, title, x + 20, y + 20);

        // Draw underline
        batch.setColor(Color.GOLD);
        batch.draw(backgroundTexture, x, y - 5, layout.width, 2);
        batch.setColor(Color.WHITE);

        // Reset font for quest items
        font.getData().setScale(1.0f);

        if (quests.isEmpty()) {
            font.setColor(Color.GRAY);
            font.draw(batch, "No quests available", x + 20, y - 30);
            return;
        }

        float questItemHeight = 65;
        float questItemWidth = 350;

        // Calculate scroll indicators
        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = quests.size() > MAX_VISIBLE_QUESTS &&
                scrollOffset < (quests.size() - MAX_VISIBLE_QUESTS) * questItemHeight;

        // Draw scroll buttons if needed
        if (canScrollUp) {
            batch.setColor(Color.WHITE);
            batch.draw(arrowTexture, scrollUpButton.x, scrollUpButton.y, scrollUpButton.width, scrollUpButton.height,
                    0, 0, 16, 16, false, false);
        }

        if (canScrollDown) {
            batch.setColor(Color.WHITE);
            batch.draw(arrowTexture, scrollDownButton.x, scrollDownButton.y, scrollDownButton.width, scrollDownButton.height,
                    0, 0, 16, 16, false, true);
        }

        // Calculate visible indices based on scroll position
        int startIndex = (int) (scrollOffset / questItemHeight);
        int endIndex = Math.min(quests.size(), startIndex + MAX_VISIBLE_QUESTS);

        float visibleAreaTop = y - 20;
        float visibleAreaBottom = y - 20 - (MAX_VISIBLE_QUESTS * questItemHeight);

        // Only render the visible quests
        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = quests.get(i);
            float itemY = visibleAreaTop - ((i - startIndex) + 1) * questItemHeight + 40;

            // Draw quest background (highlight if selected)
            if (quest == selectedQuest) {
                batch.setColor(0.3f, 0.3f, 0.4f, 0.7f);
            } else {
                batch.setColor(0.2f, 0.2f, 0.3f, 0.5f);
            }
            batch.draw(backgroundTexture, x + 10, itemY - 10, questItemWidth, questItemHeight - 5);
            batch.setColor(Color.WHITE);

            // Draw quest title with appropriate color based on status
            switch (quest.getStatus()) {
                case IN_PROGRESS:
                    font.setColor(Color.CYAN);
                    break;
                case COMPLETED:
                    font.setColor(Color.LIME);
                    break;
                case CLAIMED:
                    font.setColor(Color.LIGHT_GRAY);
                    break;
                default:
                    font.setColor(Color.WHITE);
            }

            font.draw(batch, quest.getTitle(), x + 40, itemY + 35);

            // Show progress for active quests
            if (quest.getStatus() == Quest.QuestStatus.IN_PROGRESS) {
                drawQuestProgress(batch, quest, x + 35, itemY, questItemWidth - 30);
            }
        }

        // Reset font color
        font.setColor(Color.WHITE);
    }

    private void drawQuestProgress(SpriteBatch batch, Quest quest, float x, float y, float width) {
        Map<String, Integer> requirements = quest.getRequirements();
        Map<String, Integer> inventory = gameController.getCharacter().getItems();

        // Choose the first requirement to display (simplified)
        if (!requirements.isEmpty()) {
            String item = requirements.keySet().iterator().next();
            int required = requirements.get(item);
            int collected = inventory != null ? inventory.getOrDefault(item, 0) : 0;

            // Draw text
            smallFont.setColor(Color.LIGHT_GRAY);
            smallFont.draw(batch, item + ": " + collected + "/" + required, x + 5, y + 12);

            // Draw progress bar
            float progress = Math.min(1.0f, (float) collected / required);
            batch.setColor(0.2f, 0.2f, 0.2f, 1);
            batch.draw(backgroundTexture, x, y - 10, width, 5);
            batch.setColor(0.2f, 0.7f, 1.0f, 1);
            batch.draw(backgroundTexture, x, y - 10, width * progress, 5);
            font.getData().setScale(1.0f);
        }

        batch.setColor(Color.WHITE);
    }

    public boolean handleClick(float screenX, float screenY) {
        if (!isVisible) return false;

        screenY = Gdx.graphics.getHeight() - screenY; // Convert to OpenGL coordinates

        // Check if close button is clicked
        if (closeButtonBounds.contains(screenX, screenY)) {
            gameController.returnToPreviousState();
            isVisible = false;
            return true;
        }

        // Check if scroll buttons are clicked
        List<Quest> activeQuests = gameController.getCharacter().getQuestTracker().getActiveQuests();
        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = activeQuests.size() > MAX_VISIBLE_QUESTS &&
                scrollOffset < (activeQuests.size() - MAX_VISIBLE_QUESTS) * QUEST_ITEM_HEIGHT;

        if (canScrollUp && scrollUpButton.contains(screenX, screenY)) {
            scrollUp();
            return true;
        }

        if (canScrollDown && scrollDownButton.contains(screenX, screenY)) {
            scrollDown();
            return true;
        }

        // Check if a quest is clicked
        int startIndex = (int) (scrollOffset / QUEST_ITEM_HEIGHT);
        int endIndex = Math.min(activeQuests.size(), startIndex + MAX_VISIBLE_QUESTS);

        float visibleAreaTop = QUEST_LIST_Y - 20;

        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = activeQuests.get(i);
            float itemY = visibleAreaTop - ((i - startIndex) + 1) * QUEST_ITEM_HEIGHT + 40;

            Rectangle questBounds = new Rectangle(310, itemY - 10, QUEST_ITEM_WIDTH, QUEST_ITEM_HEIGHT - 10);
            if (questBounds.contains(screenX, screenY)) {
                selectedQuest = quest;
                return true;
            }
        }

        return false;
    }

    public void toggleVisibility() {
        selectedQuest = null; // Reset selected quest when toggling visibility
        isVisible = !isVisible;

        activeQuests = gameController.getCharacter().getQuestTracker().getActiveQuests();
        if (activeQuests == null) {
            activeQuests = List.of(); // Use an empty list if null
        }

        for (Quest quest : activeQuests) {
            gameController.getBountyBoardController().checkQuestCompletion(quest.getId());
        }
    }

    public void dispose() {
        backgroundTexture.dispose();
        arrowTexture.dispose();
        font.dispose();
    }

    public void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - SCROLL_AMOUNT);
    }

    public void scrollDown() {
        List<Quest> activeQuests = gameController.getCharacter().getQuestTracker().getActiveQuests();
        if (activeQuests.size() <= MAX_VISIBLE_QUESTS) return;

        float maxScroll = (activeQuests.size() - MAX_VISIBLE_QUESTS) * QUEST_ITEM_HEIGHT;
        scrollOffset = Math.min(maxScroll, scrollOffset + SCROLL_AMOUNT);
    }
}