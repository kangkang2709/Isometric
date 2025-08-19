package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.controller.quest.BountyBoardController;
import ctu.game.isometric.model.quest.Quest;
import ctu.game.isometric.model.quest.QuestReward;
import ctu.game.isometric.animation.UIAnimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class BountyBoardView {
    private BountyBoardController controller;
    private Texture backgroundTexture;
    private Texture questCardTexture;
    private Texture buttonTexture;
    private Texture buttonHoverTexture;
    private Texture tabTexture;
    private Texture tabActiveTexture;
    private Texture panelGlowTexture;
    private Texture iconReward;
    private Texture iconRequirement;

    // Fonts
    private BitmapFont titleFont;
    private BitmapFont headerFont;
    private BitmapFont normalFont;
    private BitmapFont statusFont;
    private GlyphLayout layout;

    // UI Animation effects
    private ShaderProgram blurShader;
    private ShaderProgram glowShader;
    private float animationTime = 0f;
    private UIAnimator uiAnimator;
    private Map<String, Float> cardAnimations = new HashMap<>();

    // Status icon textures
    private TextureRegion statusIconAvailable;
    private TextureRegion statusIconActive;
    private TextureRegion statusIconCompleted;
    private TextureRegion statusIconLocked;
    private TextureRegion statusIconClaimed;

    // Scroll functionality
    private int maxVisibleQuests = 4;
    private int scrollOffset = 0;
    private float scrollVelocity = 0f;
    private float scrollSmoothing = 0f;
    private Texture scrollBarTexture;
    private Texture scrollHandleTexture;
    private Rectangle scrollBarArea;
    private Rectangle scrollHandle;
    private boolean isDraggingScroll = false;
    private List<Quest> currentQuestList = new ArrayList<>();

    // Layout properties
    private float boardX;
    private float boardY;
    private float boardWidth;
    private float boardHeight;
    private List<QuestSlot> questSlots;
    private Quest selectedQuest;
    private Quest previousSelectedQuest;
    private Matrix4 uiMatrix;
    private float detailsPanelAlpha = 0f;

    // Quest view mode
    private enum QuestTab {AVAILABLE, ACTIVE, COMPLETED, LOCKED}

    private QuestTab currentTab = QuestTab.AVAILABLE;
    private QuestTab previousTab = QuestTab.AVAILABLE;
    private float tabTransitionProgress = 1f;

    // UI Elements
    private Map<QuestTab, Rectangle> tabBounds = new HashMap<>();
    private Rectangle submitButton;
    private Rectangle acceptButton;
    private Map<Rectangle, Boolean> buttonHoverStates = new HashMap<>();

    // Colors - FF7R inspired palette
    private static final Color COLOR_PRIMARY = new Color(0.2f, 0.6f, 0.9f, 1f);
    private static final Color COLOR_SECONDARY = new Color(0.1f, 0.4f, 0.8f, 1f);
    private static final Color COLOR_ACCENT = new Color(0.0f, 0.8f, 0.8f, 1f);
    private static final Color COLOR_DARK = new Color(0.05f, 0.05f, 0.15f, 0.85f);
    private static final Color COLOR_LIGHT = new Color(0.9f, 0.9f, 1f, 1f);

    private static final Color COLOR_AVAILABLE = new Color(0.9f, 0.9f, 1f, 1f);
    private static final Color COLOR_IN_PROGRESS = new Color(0.2f, 0.7f, 1f, 1f);
    private static final Color COLOR_COMPLETED = new Color(0.3f, 0.9f, 0.5f, 1f);
    private static final Color COLOR_CLAIMED = new Color(0.6f, 0.6f, 0.6f, 0.7f);

    public BountyBoardView(BountyBoardController controller, BitmapFont titleFont, BitmapFont commonfont) {
        this.controller = controller;
        this.questSlots = new ArrayList<>();

        // Load textures with modern UI style
        backgroundTexture = new Texture(Gdx.files.internal("ui/panel-1.png"));
        questCardTexture = new Texture(Gdx.files.internal("ui/panel-header-2.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
        buttonHoverTexture = new Texture(Gdx.files.internal("ui/button_selected.png"));
        tabTexture = new Texture(Gdx.files.internal("ui/tab.png"));
        tabActiveTexture = new Texture(Gdx.files.internal("ui/tab_active.png"));
        panelGlowTexture = new Texture(Gdx.files.internal("ui/item-slot-1.png"));
        scrollBarTexture = new Texture(Gdx.files.internal("ui/button-white.png"));
        scrollHandleTexture = new Texture(Gdx.files.internal("ui/button-white.png"));
        iconReward = new Texture(Gdx.files.internal("ui/icon-checkbox-off.png"));
        iconRequirement = new Texture(Gdx.files.internal("ui/icon-checkbox-on.png"));

        // Load status icons from texture atlas
        Texture statusIcons = new Texture(Gdx.files.internal("ui/icon-checkbox-off.png"));
        int iconSize = 32;
        statusIconAvailable = new TextureRegion(statusIcons, 0, 0, iconSize, iconSize);
        statusIconActive = new TextureRegion(statusIcons, iconSize, 0, iconSize, iconSize);
        statusIconCompleted = new TextureRegion(statusIcons, iconSize * 2, 0, iconSize, iconSize);
        statusIconLocked = new TextureRegion(statusIcons, iconSize * 3, 0, iconSize, iconSize);
        statusIconClaimed = new TextureRegion(statusIcons, iconSize * 4, 0, iconSize, iconSize);

        // Initialize fonts with modern styling
        this.titleFont = titleFont;

        headerFont = titleFont;

        normalFont = commonfont;
        normalFont.setColor(COLOR_LIGHT);

        statusFont = commonfont;
        statusFont.getData().setScale(0.9f);
        normalFont.setColor(Color.YELLOW);

        layout = new GlyphLayout();

        // Initialize shaders for blur and glow effects
        blurShader = new ShaderProgram(
                Gdx.files.internal("shaders/default.vert"),
                Gdx.files.internal("shaders/blur.frag")
        );

        glowShader = new ShaderProgram(
                Gdx.files.internal("shaders/default.vert"),
                Gdx.files.internal("shaders/glow.frag")
        );


        uiAnimator = new UIAnimator();

        // Set board position and dimensions with modern proportions
        boardWidth = Gdx.graphics.getWidth() * 0.85f;
        boardHeight = Gdx.graphics.getHeight() * 0.85f;
        boardX = (Gdx.graphics.getWidth() - boardWidth) / 2;
        boardY = (Gdx.graphics.getHeight() - boardHeight) / 2;

        // Create tab areas with modern styling
        float tabWidth = 150;
        float tabHeight = 50;
        float tabY = boardY + boardHeight - 95;
        float tabSpacing = 10f;
        tabBounds.put(QuestTab.AVAILABLE, new Rectangle(boardX + 50, tabY, tabWidth, tabHeight));
        tabBounds.put(QuestTab.ACTIVE, new Rectangle(boardX + 50 + tabWidth + tabSpacing, tabY, tabWidth, tabHeight));
        tabBounds.put(QuestTab.COMPLETED, new Rectangle(boardX + 50 + (tabWidth + tabSpacing) * 2, tabY, tabWidth, tabHeight));
        tabBounds.put(QuestTab.LOCKED, new Rectangle(boardX + 50 + (tabWidth + tabSpacing) * 3, tabY, tabWidth, tabHeight));

        // Create modern buttons with hover states
        acceptButton = new Rectangle(boardX + boardWidth - 200, boardY + 110, 170, 60);
        submitButton = new Rectangle(boardX + boardWidth - 200, boardY + 110, 170, 60);
        buttonHoverStates.put(acceptButton, false);
        buttonHoverStates.put(submitButton, false);

        // Initialize modern scroll components
        float scrollBarWidth = 12;
        float scrollHandleHeight = 100;
        scrollBarArea = new Rectangle(
                boardX + 380,
                boardY + 120,
                scrollBarWidth,
                boardHeight - 240
        );
        scrollHandle = new Rectangle(
                scrollBarArea.x,
                scrollBarArea.y + scrollBarArea.height - scrollHandleHeight,
                scrollBarWidth,
                scrollHandleHeight
        );

        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize quests on startup
        controller.reset();
    }
    private float scrollInertia = 0f;
    private float scrollTargetOffset = 0f;
    private float scrollDamping = 0.92f; // Controls how quickly scrolling slows down
    private float scrollElasticity = 0.3f; // Controls bounce effect at boundaries
    private float scrollAcceleration = 15f; // How fast scrolling responds
    private boolean smoothScrollEnabled = true;

    public void update(float delta) {
        animationTime += delta;

        // Update card animations
        for (QuestSlot slot : questSlots) {
            float targetScale = (slot.quest == selectedQuest) ? 1.05f : 1.0f;
            float currentScale = cardAnimations.getOrDefault(slot.quest.getId(), 1.0f);
            float newScale = uiAnimator.lerpValue(currentScale, targetScale, 10f * delta);
            cardAnimations.put(slot.quest.getId(), newScale);
        }

        // Smooth scrolling
        if (scrollVelocity != 0) {
            scrollSmoothing += scrollVelocity * delta * 4;

            if (scrollSmoothing >= 1) {
                scrollOffset++;
                scrollSmoothing = 0;
                if (scrollOffset >= currentQuestList.size() - maxVisibleQuests) {
                    scrollOffset = Math.max(0, currentQuestList.size() - maxVisibleQuests);
                    scrollVelocity = 0;
                }
            } else if (scrollSmoothing <= -1) {
                scrollOffset--;
                scrollSmoothing = 0;
                if (scrollOffset <= 0) {
                    scrollOffset = 0;
                    scrollVelocity = 0;
                }
            }

            scrollVelocity *= 0.95f;
            if (Math.abs(scrollVelocity) < 0.1f) {
                scrollVelocity = 0;
            }

            updateQuestSlots();
        }

        if (smoothScrollEnabled) {
            // Apply velocity to inertia
            if (scrollVelocity != 0) {
                scrollInertia += scrollVelocity * delta * scrollAcceleration;
                scrollVelocity = 0; // Reset input velocity after applying it
            }

            // Calculate target scroll position with inertia
            scrollTargetOffset += scrollInertia;

            // Apply boundary constraints with elasticity
            float maxScroll = Math.max(0, currentQuestList.size() - maxVisibleQuests);
            if (scrollTargetOffset < 0) {
                scrollTargetOffset += (0 - scrollTargetOffset) * scrollElasticity;
                scrollInertia *= 0.7f; // Reduce inertia when hitting top boundary
            } else if (scrollTargetOffset > maxScroll) {
                scrollTargetOffset += (maxScroll - scrollTargetOffset) * scrollElasticity;
                scrollInertia *= 0.7f; // Reduce inertia when hitting bottom boundary
            }

            // Apply scrolling with integer precision to avoid visual jittering
            float previousScrollOffset = scrollOffset;
            scrollOffset = Math.round(scrollTargetOffset);

            // Update quest slots if scroll position changed
            if (previousScrollOffset != scrollOffset) {
                updateQuestSlots();
            }

            // Apply damping to gradually reduce inertia
            scrollInertia *= scrollDamping;

            // Stop tiny movements to avoid continuous small scrolling
            if (Math.abs(scrollInertia) < 0.01f) {
                scrollInertia = 0;
            }
        }

        // Update scroll handle position
        if (currentQuestList.size() > maxVisibleQuests) {
            float availableTrack = scrollBarArea.height - scrollHandle.height;
            float scrollRatio = Math.max(0, Math.min(1, scrollTargetOffset / (currentQuestList.size() - maxVisibleQuests)));
            scrollHandle.y = scrollBarArea.y + availableTrack * (1 - scrollRatio);
        }

        // Update details panel animation
        float targetAlpha = (selectedQuest != null) ? 1f : 0f;
        detailsPanelAlpha = uiAnimator.lerpValue(detailsPanelAlpha, targetAlpha, delta * 5);

        // Update tab transition
        if (tabTransitionProgress < 1f) {
            tabTransitionProgress += delta * 4;
            if (tabTransitionProgress >= 1f) {
                tabTransitionProgress = 1f;
                previousTab = currentTab;
            }
        }

    }

    public void render(SpriteBatch batch) {
        update(Gdx.graphics.getDeltaTime());

        // Update quest slots based on current tab
        updateQuestSlots();

        batch.setProjectionMatrix(uiMatrix);

        // Draw semi-transparent background with glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(
                panelGlowTexture,
                boardX - 30, boardY - 30,
                boardWidth + 60, boardHeight + 60
        );

        // Draw main panel background
        batch.setColor(0.1f, 0.1f, 0.2f, 0.85f);  // Semi-transparent dark blue
        batch.draw(
                backgroundTexture,
                boardX, boardY,
                boardWidth, boardHeight
        );
        batch.setColor(Color.WHITE);

        // Draw title with glow effect
        String title = "BOUNTY BOARD";
        layout.setText(titleFont, title);
        float titleX = boardX + (boardWidth - layout.width) / 2;
        float titleY = boardY + boardHeight - 8;

        // Draw title glow
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        titleFont.setColor(COLOR_ACCENT.r, COLOR_ACCENT.g, COLOR_ACCENT.b, 0.5f);
        titleFont.draw(batch, title, titleX + 1, titleY + 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw title
        titleFont.setColor(COLOR_ACCENT);
        titleFont.draw(batch, title, titleX, titleY);

        // Draw tabs with animated transitions
        for (Map.Entry<QuestTab, Rectangle> tab : tabBounds.entrySet()) {
            QuestTab tabType = tab.getKey();
            Rectangle bounds = tab.getValue();

            float alpha = 0.8f;
            float yOffset = 0;

            if (tabType == currentTab) {
                alpha = 1f;
                yOffset = 5 * (1f - Interpolation.swingOut.apply(tabTransitionProgress)) + 10;
            } else if (tabType == previousTab && tabTransitionProgress < 1f) {
                alpha = 0.8f + 0.2f * (1f - tabTransitionProgress);
                yOffset = 5 * tabTransitionProgress;
            }

            Texture tabTex = (tabType == currentTab) ? tabActiveTexture : tabTexture;
            batch.setColor(1, 1, 1, alpha);
            batch.draw(
                    tabTex,
                    bounds.x,
                    bounds.y + yOffset,
                    bounds.width,
                    bounds.height
            );

            String tabText = tabType.name();
            layout.setText(normalFont, tabText);
            float textX = bounds.x + (bounds.width - layout.width) / 2;
            float textY = bounds.y + bounds.height - 15 + yOffset;

            normalFont.setColor(tabType == currentTab ? COLOR_ACCENT : COLOR_DARK);
            normalFont.draw(batch, tabText, textX, textY);
        }
        batch.setColor(Color.WHITE);

        // Draw scroll bar if needed
        if (currentQuestList.size() > maxVisibleQuests) {
            batch.setColor(1, 1, 1, 0.3f);
            batch.draw(
                    scrollBarTexture,
                    scrollBarArea.x,
                    scrollBarArea.y,
                    scrollBarArea.width,
                    scrollBarArea.height
            );

            batch.setColor(1, 1, 1, 0.8f);
            batch.draw(
                    scrollHandleTexture,
                    scrollHandle.x,
                    scrollHandle.y,
                    scrollHandle.width,
                    scrollHandle.height
            );
        }
        batch.setColor(Color.WHITE);

        // Draw quest cards with animations
        for (QuestSlot slot : questSlots) {
            float scale = cardAnimations.getOrDefault(slot.quest.getId(), 1.0f);
            float width = slot.bounds.width * scale;
            float height = slot.bounds.height * scale;
            float x = slot.bounds.x - (width - slot.bounds.width) / 2;
            float y = slot.bounds.y - (height - slot.bounds.height) / 2;

            // Draw card background with glow effect if selected
            if (slot.quest == selectedQuest) {
                batch.setColor(COLOR_PRIMARY.r, COLOR_PRIMARY.g, COLOR_PRIMARY.b, 0.3f);
                batch.draw(panelGlowTexture, x - 5, y - 5, width + 10, height + 10);
                batch.setColor(Color.WHITE);
            }

            batch.draw(questCardTexture, x, y, width, height);

            // Get appropriate status icon
            TextureRegion statusIcon;
            switch (slot.quest.getStatus()) {
                case AVAILABLE:
                    normalFont.setColor(COLOR_AVAILABLE);
                    statusIcon = statusIconAvailable;
                    break;
                case IN_PROGRESS:
                    normalFont.setColor(COLOR_IN_PROGRESS);
                    statusIcon = statusIconActive;
                    break;
                case COMPLETED:
                    normalFont.setColor(COLOR_COMPLETED);
                    statusIcon = statusIconCompleted;
                    break;
                case LOCKED:
                    normalFont.setColor(Color.GRAY);
                    statusIcon = statusIconLocked;
                    break;
                case CLAIMED:
                    normalFont.setColor(COLOR_CLAIMED);
                    statusIcon = statusIconClaimed;
                    break;
                default:
                    statusIcon = statusIconAvailable;
            }

            // Draw status icon
            float iconSize = 24 * scale;
            batch.draw(statusIcon, x + 10, y + height - 30, iconSize, iconSize);

            // Draw quest title
            normalFont.draw(batch, slot.quest.getTitle(),
                    x + 40, y + height - 15);

            // Draw quest description (shortened)
            normalFont.setColor(COLOR_LIGHT);
            String description = slot.quest.getDescription();
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            normalFont.draw(batch, description,
                    x + 15, y + height - 45, width - 30, Align.left, true);

            batch.setColor(Color.WHITE);
        }

        // Draw selected quest details panel with animation
        if (selectedQuest != null && detailsPanelAlpha > 0.01f) {
            batch.setColor(1, 1, 1, detailsPanelAlpha);

            float detailPanelX = boardX + 400;
            float detailPanelWidth = boardWidth - 430;
            float detailPanelY = boardY + 100;
            float detailPanelHeight = boardHeight - 220;

            // Draw detail panel background with glow
            batch.setColor(COLOR_PRIMARY.r, COLOR_PRIMARY.g, COLOR_PRIMARY.b, 0.2f * detailsPanelAlpha);
            batch.draw(panelGlowTexture,
                    detailPanelX - 10, detailPanelY - 10,
                    detailPanelWidth + 20, detailPanelHeight + 20);

            // Draw detail panel
            batch.setColor(0.12f, 0.15f, 0.25f, 0.9f * detailsPanelAlpha);
            batch.draw(backgroundTexture,
                    detailPanelX, detailPanelY,
                    detailPanelWidth, detailPanelHeight);

            batch.setColor(1, 1, 1, detailsPanelAlpha);

            float detailX = detailPanelX + 20;
            float detailY = detailPanelY + detailPanelHeight - 30;

            // Draw title with appropriate status color
            Color statusColor;
            switch (selectedQuest.getStatus()) {
                case AVAILABLE:
                    statusColor = COLOR_AVAILABLE;
                    break;
                case IN_PROGRESS:
                    statusColor = COLOR_IN_PROGRESS;
                    break;
                case COMPLETED:
                    statusColor = COLOR_COMPLETED;
                    break;
                case CLAIMED:
                    statusColor = COLOR_CLAIMED;
                    break;
                default:
                    statusColor = COLOR_LIGHT;
            }

            headerFont.setColor(statusColor.r, statusColor.g, statusColor.b, detailsPanelAlpha);
            headerFont.draw(batch, selectedQuest.getTitle(), detailX, detailY);

            // Draw status badge
            statusFont.setColor(statusColor.r, statusColor.g, statusColor.b, detailsPanelAlpha);
            String statusText = selectedQuest.getStatus().name();
            statusFont.draw(batch, statusText, detailX, detailY - 35);

            // Draw description
            normalFont.setColor(COLOR_LIGHT.r, COLOR_LIGHT.g, COLOR_LIGHT.b, detailsPanelAlpha);
            normalFont.draw(batch, selectedQuest.getDescription(),
                    detailX, detailY - 70, detailPanelWidth - 40, -1, true);

            // Requirements section
            float requirementY = detailY - 150;

            // Draw requirements icon and header
            batch.draw(iconRequirement, detailX, requirementY - 15, 24, 24);
            headerFont.setColor(COLOR_ACCENT.r, COLOR_ACCENT.g, COLOR_ACCENT.b, detailsPanelAlpha);
            headerFont.draw(batch, "Requirements", detailX + 35, requirementY);

            // Draw separator line

            normalFont.setColor(COLOR_LIGHT.r, COLOR_LIGHT.g, COLOR_LIGHT.b, detailsPanelAlpha);
            int reqY = 0;
            for (Map.Entry<String, Integer> req : selectedQuest.getRequirements().entrySet()) {
                normalFont.draw(batch, "• " + req.getValue() + "× " + req.getKey(),
                        detailX + 10, requirementY - 30 - (reqY * 25));
                reqY++;
            }

            // Rewards section
            float rewardY = requirementY - 50 - (reqY * 25);

            // Draw rewards icon and header
            batch.draw(iconReward, detailX, rewardY - 15, 24, 24);
            headerFont.setColor(COLOR_ACCENT.r, COLOR_ACCENT.g, COLOR_ACCENT.b, detailsPanelAlpha);
            headerFont.draw(batch, "Rewards", detailX + 35, rewardY);

            // Draw separator line

            QuestReward reward = selectedQuest.getReward();
            normalFont.draw(batch, "• XP: " + reward.getExperience(),
                    detailX + 10, rewardY - 30);
            normalFont.draw(batch, "• Gold: " + reward.getGold(),
                    detailX + 10, rewardY - 55);

            int rewardItemY = 0;
            if (reward.getItems() != null && !reward.getItems().isEmpty()) {
                for (Map.Entry<String, Integer> item : reward.getItems().entrySet()) {
                    normalFont.draw(batch, "• " + item.getValue() + "× " + item.getKey(),
                            detailX + 10, rewardY - 80 - (rewardItemY * 25));
                    rewardItemY++;
                }
            }

            // Draw accept button if quest is available
            if (selectedQuest.getStatus() == Quest.QuestStatus.AVAILABLE ||
                    controller.checkCanAcceptQuest(selectedQuest.getId())) {
                boolean isHovered = buttonHoverStates.get(acceptButton);
                batch.draw(
                        isHovered ? buttonHoverTexture : buttonTexture,
                        acceptButton.x, acceptButton.y,
                        acceptButton.width, acceptButton.height
                );

                layout.setText(normalFont, "Accept Quest");
                normalFont.setColor(isHovered ? COLOR_ACCENT : COLOR_LIGHT);
                normalFont.draw(batch, "Accept Quest",
                        acceptButton.x + (acceptButton.width - layout.width) / 2,
                        acceptButton.y + 35);
            }

            // Draw complete button if quest requirements are met
            if (selectedQuest.getStatus() == Quest.QuestStatus.IN_PROGRESS &&
                    controller.checkQuestCompletion(selectedQuest.getId())) {
                boolean isHovered = buttonHoverStates.get(submitButton);
                batch.draw(
                        isHovered ? buttonHoverTexture : buttonTexture,
                        submitButton.x, submitButton.y,
                        submitButton.width, submitButton.height
                );

                layout.setText(normalFont, "Complete Quest");
                normalFont.setColor(isHovered ? COLOR_ACCENT : COLOR_LIGHT);
                normalFont.draw(batch, "Complete Quest",
                        submitButton.x + (submitButton.width - layout.width) / 2,
                        submitButton.y + 35);
            }

            // Draw submit button if quest is completed
            if (selectedQuest.getStatus() == Quest.QuestStatus.COMPLETED) {
                boolean isHovered = buttonHoverStates.get(submitButton);
                batch.draw(
                        isHovered ? buttonHoverTexture : buttonTexture,
                        submitButton.x, submitButton.y,
                        submitButton.width, submitButton.height
                );

                layout.setText(normalFont, "Claim Rewards");
                normalFont.setColor(isHovered ? COLOR_ACCENT : COLOR_LIGHT);
                normalFont.draw(batch, "Claim Rewards",
                        submitButton.x + (submitButton.width - layout.width) / 2,
                        submitButton.y + 35);
            }
        }


        // Reset color
        batch.setColor(Color.WHITE);
    }

    boolean needUpdate = true;

    private void updateQuestSlots() {
        // Always refresh quest lists when switching tabs or after quest state changes
        if (needUpdate || currentTab != previousTab) {
            controller.updateQuestStatusFromQuestTracker(
                    controller.getGameController().getCharacter().getQuestTracker()
            );
            needUpdate = false;
        }

        // Filter quests based on the current tab
        switch (currentTab) {
            case AVAILABLE:
                currentQuestList = controller.getAllQuests().values().stream()
                        .filter(quest -> quest.getStatus() == Quest.QuestStatus.AVAILABLE)
                        .toList();
                break;
            case ACTIVE:
                // Refresh active quests from controller
                currentQuestList = new ArrayList<>(controller.getActiveQuests());
                break;
            case COMPLETED:
                // Refresh completed quests from controller
                currentQuestList = new ArrayList<>(controller.getCompletedQuests());
                break;
            case LOCKED:
                // Refresh locked quests from controller
                currentQuestList = new ArrayList<>(controller.getLockedQuests());
                break;
            default:
                currentQuestList = new ArrayList<>();
        }

        // Ensure scrollOffset is valid
    // Ensure scrollOffset is valid
    scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, currentQuestList.size() - maxVisibleQuests)));

    // Clear and populate only visible quest slots
    questSlots.clear();
    float slotWidth = 350;
    float slotHeight = 95;
    float slotX = boardX + 20;
    float slotY = boardY + boardHeight - 200;
    float padding = 15;

    int end = Math.min(scrollOffset + maxVisibleQuests, currentQuestList.size());
    for (int i = scrollOffset; i < end; i++) {
        int displayIndex = i - scrollOffset;
        float yOffset = slotY - (displayIndex * (slotHeight + padding));
        questSlots.add(new QuestSlot(currentQuestList.get(i), new Rectangle(slotX, yOffset, slotWidth, slotHeight)));
    }
    }


    public boolean handleClick(float screenX, float screenY) {

        screenY = Gdx.graphics.getHeight() - screenY; // Invert Y coordinate for GDX
        // Check tab clicks
        for (Map.Entry<QuestTab, Rectangle> tab : tabBounds.entrySet()) {
            if (tab.getValue().contains(screenX, screenY)) {
                if (currentTab != tab.getKey()) {
                    previousTab = currentTab;
                    currentTab = tab.getKey();
                    tabTransitionProgress = 0f;
                    selectedQuest = null;
                    scrollOffset = 0; // Reset scroll position when changing tabs
                }
                return true;
            }
        }


        // Check if a quest slot was clicked
        for (QuestSlot slot : questSlots) {
            if (slot.bounds.contains(screenX, screenY)) {
                if (selectedQuest != slot.quest) {
                    previousSelectedQuest = selectedQuest;
                    selectedQuest = slot.quest;
                }
                return true;
            }
        }

        // Check if accept button was clicked
        if (selectedQuest != null && (selectedQuest.getStatus() == Quest.QuestStatus.AVAILABLE || controller.checkCanAcceptQuest(selectedQuest.getId()))) {
            if (acceptButton.contains(screenX, screenY)) {
                controller.acceptQuest(selectedQuest.getId());
                needUpdate = true; // This will now refresh all tabs
                // Optionally switch to ACTIVE tab to show the accepted quest
                if (currentTab == QuestTab.AVAILABLE) {
                    previousTab = currentTab;
                    currentTab = QuestTab.ACTIVE;
                    tabTransitionProgress = 0f;
                }
                return true;
            }
        }

        // Check if submit/complete button was clicked
        if (selectedQuest != null) {
            if (selectedQuest.getStatus() == Quest.QuestStatus.COMPLETED && submitButton.contains(screenX, screenY)) {
                controller.submitQuest(selectedQuest.getId());
                needUpdate = true; // This will now refresh all tabs
                selectedQuest = null;
                // Optionally switch to COMPLETED tab to show the claimed quest
                if (currentTab == QuestTab.ACTIVE) {
                    previousTab = currentTab;
                    currentTab = QuestTab.COMPLETED;
                    tabTransitionProgress = 0f;
                }
                return true;
            } else if (selectedQuest.getStatus() == Quest.QuestStatus.IN_PROGRESS &&
                    controller.checkQuestCompletion(selectedQuest.getId()) &&
                    submitButton.contains(screenX, screenY)) {
                controller.submitQuest(selectedQuest.getId());
                needUpdate = true;
                return true;
            }
        }

        return false;
    }

    public boolean handleDrag(float screenX, float screenY) {
        Vector2 pos = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);

        if (isDraggingScroll) {
            float availableTrack = scrollBarArea.height - scrollHandle.height;
            float relativeY = pos.y - scrollBarArea.y;
            float scrollRatio = 1 - Math.max(0, Math.min(1, relativeY / availableTrack));

            int newOffset = (int) (scrollRatio * (currentQuestList.size() - maxVisibleQuests));
            if (newOffset != scrollOffset) {
                scrollOffset = Math.max(0, Math.min(currentQuestList.size() - maxVisibleQuests, newOffset));
                updateQuestSlots();
            }
            return true;
        }

        return false;
    }

    public boolean handleScroll(float amountY) {
        if (currentQuestList.size() > maxVisibleQuests) {
            scrollVelocity += amountY * 0.5f; // Adjust multiplier for faster/slower scrolling
            return true;
        }
        return false;
    }

    public boolean scrollUp() {
        if (currentQuestList.size() > maxVisibleQuests) {
            scrollVelocity = -3f; // Adjust this value for faster/slower scrolling
            return true;
        }
        return false;
    }

    public boolean scrollDown() {
        if (currentQuestList.size() > maxVisibleQuests) {
            scrollVelocity = 3f; // Adjust this value for faster/slower scrolling
            return true;
        }
        return false;
    }

    public void dispose() {
        backgroundTexture.dispose();
        questCardTexture.dispose();
        buttonTexture.dispose();
        buttonHoverTexture.dispose();
        tabTexture.dispose();
        tabActiveTexture.dispose();
        panelGlowTexture.dispose();
        scrollBarTexture.dispose();
        scrollHandleTexture.dispose();
        iconReward.dispose();
        iconRequirement.dispose();
        titleFont.dispose();
        headerFont.dispose();
        normalFont.dispose();
        statusFont.dispose();
        blurShader.dispose();
        glowShader.dispose();

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