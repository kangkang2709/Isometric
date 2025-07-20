package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class MerchantUI {
    private GameController gameController;
    private BitmapFont titleFont;
    private BitmapFont font;
    private BitmapFont smallFont;
    private GlyphLayout layout;
    private boolean visible = false;

    private ShapeRenderer shapeRenderer;
    private Texture backgroundTexture;
    private Texture buttonTexture;
    private Texture slotTexture;
    private Texture highlightTexture;

    private int selectedItemIndex = -1;
    private List<Items> merchantItems = new ArrayList<>();
    private Map<Items, Integer> playerItems = new HashMap<>();
    private List<Items> displayItems = new ArrayList<>();

    private Rectangle merchantBounds;
    private Rectangle[] itemSlots;
    private Rectangle[] playerItemSlots;
    private Rectangle buyButton;
    private Rectangle sellButton;
    private Rectangle closeButton;
    private Rectangle tabMerchantButton;
    private Rectangle tabPlayerButton;

    private boolean showingMerchantItems = true;
    private float animTime = 0;
    private float hoverAlpha = 0;

    private static final int SLOTS_PER_ROW = 5;
    private static final int MAX_SLOTS = 15;
    private static final int SLOT_SIZE = 64;
    private static final int PADDING = 12;
    private static final float CORNER_RADIUS = 10f;

    private Map<String, Texture> itemTextures = new HashMap<>();
    private Matrix4 uiMatrix;

    // Enhanced UI colors
    private final Color bgColor = new Color(0.12f, 0.12f, 0.18f, 0.95f);
    private final Color buttonColor = new Color(0.25f, 0.25f, 0.35f, 1f);
    private final Color buttonHoverColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private final Color activeTabColor = new Color(0.3f, 0.4f, 0.8f, 1f);
    private final Color inactiveTabColor = new Color(0.22f, 0.22f, 0.28f, 1f);
    private final Color closeButtonColor = new Color(0.7f, 0.3f, 0.3f, 1f);
    private final Color closeButtonHoverColor = new Color(0.8f, 0.4f, 0.4f, 1f);
    private final Color affordableColor = new Color(0.4f, 0.9f, 0.4f, 1f);

    private final Color unaffordableColor = new Color(0.9f, 0.4f, 0.4f, 1f);
    private final Color priceColor = new Color(1f, 0.85f, 0.4f, 1f);

    private final Color titleColor = new Color(0.9f, 0.9f, 1f, 1f);
    private final Color textColor = new Color(0.85f, 0.85f, 0.9f, 1f);
    private final Color dividerColor = new Color(0.3f, 0.3f, 0.4f, 0.8f);

    public MerchantUI(GameController gameController) {
        this.gameController = gameController;
        this.layout = new GlyphLayout();
        this.shapeRenderer = new ShapeRenderer();

        // Generate better fonts using FreeType
        initFonts();

        // Load textures
        try {
            backgroundTexture = new Texture(Gdx.files.internal("ui/merchant.png"));
            buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
            slotTexture = new Texture(Gdx.files.internal("ui/slot.png"));
            highlightTexture = new Texture(Gdx.files.internal("ui/slot_highlight.png"));
        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Failed to load UI textures, using fallback rendering", e);
        }

        // Create projection matrix
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Calculate merchant UI bounds with enhanced proportions
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float merchantWidth = 800;
        float merchantHeight = 600;

        merchantBounds = new Rectangle(
                (screenWidth - merchantWidth) / 2,
                (screenHeight - merchantHeight) / 2,
                merchantWidth,
                merchantHeight
        );

        // Create item slots with even spacing
        createSlots();

        // Create tabs with better positioning
        float tabWidth = 130;
        float tabHeight = 36;
        tabMerchantButton = new Rectangle(
                merchantBounds.x + PADDING + 60,
                merchantBounds.y + merchantBounds.height - 36,
                tabWidth,
                tabHeight
        );
        tabPlayerButton = new Rectangle(
                merchantBounds.x + tabWidth + PADDING * 2 + 60,
                merchantBounds.y + merchantBounds.height - 36,
                tabWidth,
                tabHeight
        );

        // Create buttons with better positioning
        float buttonWidth = 140;
        float buttonHeight = 48;
        float buttonX = merchantBounds.x + merchantBounds.width - buttonWidth - PADDING * 2;
        float buttonY = merchantBounds.y + PADDING * 3 + 120;

        buyButton = new Rectangle(buttonX, buttonY + buttonHeight + PADDING, buttonWidth, buttonHeight);
        sellButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        closeButton = new Rectangle(
                merchantBounds.x + merchantBounds.width - 36 - PADDING,
                merchantBounds.y + merchantBounds.height - 36 - PADDING,
                36,
                36
        );

        // Load merchant items
        loadMerchantItems();
    }

    private void initFonts() {
        try {
            this.font = generateVietNameseFont("Roboto-Black.ttf", 16);
            this.titleFont = generateVietNameseFont("Roboto-Black.ttf", 16);
            this.smallFont = generateVietNameseFont("Roboto-Black.ttf", 12);
        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Failed to load custom fonts, using default", e);
            titleFont = new BitmapFont();
            titleFont.getData().setScale(1.5f);
            font = new BitmapFont();
            smallFont = new BitmapFont();
            smallFont.getData().setScale(0.8f);
        }
    }

    private void createSlots() {
        itemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            itemSlots[i] = new Rectangle(
                    merchantBounds.x + PADDING * 2 + (col * (SLOT_SIZE + PADDING)) + 50,
                    merchantBounds.y + merchantBounds.height - PADDING * 2 - 30 - SLOT_SIZE - (row * (SLOT_SIZE + PADDING + 16)),
                    SLOT_SIZE,
                    SLOT_SIZE
            );
        }

        playerItemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            playerItemSlots[i] = new Rectangle(itemSlots[i].x, itemSlots[i].y, SLOT_SIZE, SLOT_SIZE);
        }
    }

    public void render(SpriteBatch batch) {
        if (!visible) return;

        // Animation time update
        animTime += Gdx.graphics.getDeltaTime();
        float pulse = (float) Math.sin(animTime * 3) * 0.1f + 0.9f;

        // Update player items each render to ensure it's current
        updatePlayerItems();

        Matrix4 prevMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Draw background using ShapeRenderer with rounded corners
        shapeRenderer.setProjectionMatrix(uiMatrix);

        // Draw main panel
        drawPanel(batch, merchantBounds, bgColor);

        // Draw tabs
        drawTab(tabMerchantButton, showingMerchantItems);
        drawTab(tabPlayerButton, !showingMerchantItems);

        // Draw item slots with subtle highlighting
        drawItemSlots(batch);

        // Draw buttons
        drawButtons(batch);

        // Draw dividing lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(dividerColor);

        // Horizontal divider below items
        float dividerY = merchantBounds.y + merchantBounds.height - 70 - (MAX_SLOTS / SLOTS_PER_ROW) * (SLOT_SIZE + PADDING) - PADDING * 2;
        shapeRenderer.rectLine(
                merchantBounds.x + PADDING,
                dividerY,
                merchantBounds.x + merchantBounds.width - PADDING,
                dividerY,
                2
        );
        shapeRenderer.end();

        // Configure batch for UI rendering
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        // Draw title and UI text with better positioning
        drawUIText(batch);

        // Draw item icons and details
        drawItems(batch, pulse);

        // Draw item details section with animations
        drawItemDetails(batch);

        batch.end();

        // Restore original batch state
        if (batchWasDrawing) {
            batch.setProjectionMatrix(prevMatrix);
            batch.begin();
        }
    }

    private void drawPanel(Batch batch, Rectangle bounds, Color color) {
        if (backgroundTexture != null) {
            batch.begin();
            batch.setProjectionMatrix(uiMatrix);
            batch.draw(backgroundTexture, bounds.x - 50, bounds.y + 30, bounds.width + 50, bounds.height);
            batch.setColor(Color.WHITE); // Reset color
            batch.end();
        } else {
            if (shapeRenderer != null) {
                Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(color);
                drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CORNER_RADIUS);
                shapeRenderer.end();
                Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
            }
        }
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius) {
        // Center points
        shapeRenderer.rect(x + radius, y, width - 2 * radius, height);
        shapeRenderer.rect(x, y + radius, width, height - 2 * radius);

        // Four corners
        shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f);
        shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f);
        shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f);
        shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f);
    }

    private void drawTab(Rectangle tabBounds, boolean active) {
        boolean isHovered = (tabBounds == tabMerchantButton && hoverMerchantTab) ||
                (tabBounds == tabPlayerButton && hoverPlayerTab);

        Color tabColor;
        if (active) {
            tabColor = activeTabColor;
        } else {
            tabColor = isHovered ? buttonHoverColor : inactiveTabColor;
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(tabColor);

        // Draw tab with rounded top corners
        shapeRenderer.rect(tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height - CORNER_RADIUS);
        shapeRenderer.arc(tabBounds.x + CORNER_RADIUS, tabBounds.y + tabBounds.height - CORNER_RADIUS,
                CORNER_RADIUS, 90, 90);
        shapeRenderer.arc(tabBounds.x + tabBounds.width - CORNER_RADIUS, tabBounds.y + tabBounds.height - CORNER_RADIUS,
                CORNER_RADIUS, 0, 90);
        shapeRenderer.rect(tabBounds.x + CORNER_RADIUS, tabBounds.y + tabBounds.height - CORNER_RADIUS,
                tabBounds.width - 2 * CORNER_RADIUS, CORNER_RADIUS);

        shapeRenderer.end();
    }

    public boolean handleMouseMove(int screenX, int screenY) {
        if (!visible) return false;

        // Convert to UI coordinates (origin at bottom-left)
        float uiY = Gdx.graphics.getHeight() - screenY;

        // Reset all hover states
        hoverBuyButton = false;
        hoverSellButton = false;
        hoverCloseButton = false;
        hoverMerchantTab = false;
        hoverPlayerTab = false;

        // Check if mouse is inside merchant bounds
        if (!merchantBounds.contains(screenX, uiY)) {
            return false;
        }

        // Check tabs
        hoverMerchantTab = tabMerchantButton.contains(screenX, uiY);
        hoverPlayerTab = tabPlayerButton.contains(screenX, uiY);

        // Check close button
        hoverCloseButton = closeButton.contains(screenX, uiY);

        // Check action buttons when item is selected
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            hoverBuyButton = showingMerchantItems && buyButton.contains(screenX, uiY);
            hoverSellButton = !showingMerchantItems && sellButton.contains(screenX, uiY);
        }

        return true;
    }

    private void drawItemSlots(SpriteBatch batch) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;

        batch.begin();
        batch.setProjectionMatrix(uiMatrix);
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < currentItems.size()) {
                Items item = currentItems.get(i);
                Rectangle slot = currentSlots[i];

                // Draw slot texture
                if (slotTexture != null) {
                    batch.draw(slotTexture, slot.x, slot.y, slot.width, slot.height);
                }

                // Highlight selected slot
                if (i == selectedItemIndex && highlightTexture != null) {
                    batch.draw(highlightTexture, slot.x, slot.y, slot.width, slot.height);
                }

                // Draw item texture if available
//                if (item != null && item.getTexturePath() != null) {
//                    Texture itemTexture = getItemTexture(item.getTexturePath());
//                    float itemSize = 40;
//                    float centerX = slot.x + SLOT_SIZE / 2f - itemSize / 2f;
//                    float centerY = slot.y + SLOT_SIZE / 2f - itemSize / 2f;
//                    batch.draw(itemTexture, centerX, centerY, itemSize, 34);
//                }
            }
        }

        batch.end();
    }


    private Rectangle hoveredButton = null;
    private boolean hoverBuyButton = false;
    private boolean hoverSellButton = false;
    private boolean hoverCloseButton = false;
    private boolean hoverMerchantTab = false;
    private boolean hoverPlayerTab = false;


    private void drawButtons(Batch batch) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;

        boolean batchWasDrawing = batch.isDrawing();
        if (!batchWasDrawing) {
            batch.begin();
        }

        // Draw action button if item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            Rectangle actionButton = showingMerchantItems ? buyButton : sellButton;
            boolean isHovered = showingMerchantItems ? hoverBuyButton : hoverSellButton;

            if (buttonTexture != null) {
                // Apply hover effect
                Color buttonColorToUse = isHovered ? buttonHoverColor : buttonColor;
                batch.setColor(buttonColorToUse);
                batch.draw(buttonTexture,
                        actionButton.x, actionButton.y,
                        actionButton.width, actionButton.height);
                batch.setColor(Color.WHITE); // Reset color

                // Draw button text
                String buttonText = showingMerchantItems ? "BUY" : "SELL";
                layout.setText(font, buttonText);
                font.draw(batch, buttonText,
                        actionButton.x + (actionButton.width - layout.width) / 2,
                        actionButton.y + (actionButton.height + layout.height) / 2);
            } else {
                if (shapeRenderer != null) {
                    if (batchWasDrawing) batch.end();

                    Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeRenderer.setColor(isHovered ? buttonHoverColor : buttonColor);
                    drawRoundedRect(actionButton.x, actionButton.y,
                            actionButton.width, actionButton.height,
                            CORNER_RADIUS);
                    shapeRenderer.end();
                    Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

                    if (batchWasDrawing) batch.begin();
                }
            }
        }

        // Draw close button with hover effect
        if (buttonTexture != null) {
            batch.setColor(hoverCloseButton ? closeButtonHoverColor : closeButtonColor);
            batch.draw(buttonTexture,
                    closeButton.x, closeButton.y,
                    closeButton.width, closeButton.height);
            batch.setColor(Color.WHITE);

            // Draw X on top of texture
            font.draw(batch, "X",
                    closeButton.x + closeButton.width / 2 - 6,
                    closeButton.y + closeButton.height / 2 + 8);
        } else {
            if (shapeRenderer != null) {
                if (batchWasDrawing) batch.end();

                Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(hoverCloseButton ? closeButtonHoverColor : closeButtonColor);
                drawRoundedRect(closeButton.x, closeButton.y,
                        closeButton.width, closeButton.height,
                        CORNER_RADIUS);
                shapeRenderer.end();
                Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

                if (batchWasDrawing) batch.begin();
            }
        }

        // Only end batch if we started it
        if (!batchWasDrawing) {
            batch.end();
        }
    }

    private void drawUIText(SpriteBatch batch) {
        // Draw tabs text with better positioning
        titleFont.setColor(Color.WHITE);
        layout.setText(titleFont, "Merchant");
        titleFont.draw(batch, "Merchant",
                tabMerchantButton.x + (tabMerchantButton.width - layout.width) / 2,
                tabMerchantButton.y + tabMerchantButton.height - (tabMerchantButton.height - layout.height) / 2);

        layout.setText(titleFont, "Your Items");
        titleFont.draw(batch, "Your Items",
                tabPlayerButton.x + (tabPlayerButton.width - layout.width) / 2,
                tabPlayerButton.y + tabPlayerButton.height - (tabPlayerButton.height - layout.height) / 2);

        // Draw character score
        Character character = gameController.getCharacter();
        titleFont.setColor(priceColor);
        String scoreText = "Your Score: " + (int) character.getScore();
        layout.setText(titleFont, scoreText);
        titleFont.draw(batch, scoreText,
                merchantBounds.x + merchantBounds.width - layout.width - 340,
                merchantBounds.y + merchantBounds.height - PADDING * 1.5f);

        // Draw close button text
        font.setColor(Color.WHITE);
        font.draw(batch, "X", closeButton.x + (closeButton.width - 12) / 2, closeButton.y + (closeButton.height + 12) / 2);
    }

    private void drawItems(SpriteBatch batch, float pulse) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;
        Character character = gameController.getCharacter();

        // Draw items
        for (int i = 0; i < Math.min(currentItems.size(), MAX_SLOTS); i++) {
            Items item = currentItems.get(i);
            if (item != null && item.getTexturePath() != null) {
                // Draw the item icon
                Texture itemTexture = getItemTexture(item.getTexturePath());

                // Apply subtle pulse animation to selected item
                float scale = (i == selectedItemIndex) ? pulse : 1.0f;
                float itemSize = 37;
                float centerX = currentSlots[i].x + SLOT_SIZE / 2f - itemSize * scale / 2f;
                float centerY = currentSlots[i].y + SLOT_SIZE / 2f - itemSize * scale / 2f;

                batch.draw(itemTexture, centerX, centerY, itemSize * scale, itemSize);

                // Draw price/quantity
                smallFont.setColor(Color.WHITE);
                if (showingMerchantItems) {
                    try {
                        int price = item.getItemPrice();
                        smallFont.setColor(character.getScore() >= price ? affordableColor : unaffordableColor);
                        smallFont.draw(batch, price + "", currentSlots[i].x + 5, currentSlots[i].y + 15);
                    } catch (NumberFormatException e) {
                        // If price isn't valid, don't show it
                    }
                } else {
                    // Draw quantity
                    int quantity = playerItems.get(item);
                    smallFont.draw(batch, "x" + quantity,
                            currentSlots[i].x + SLOT_SIZE - 20,
                            currentSlots[i].y + SLOT_SIZE - 10);
                }

                // Draw item name below slot
                smallFont.setColor(textColor);
                String itemName = item.getItemName();
                if (itemName.length() > 10) {
                    itemName = itemName.substring(0, 7) + "...";
                }
                layout.setText(smallFont, itemName);
                smallFont.draw(batch, itemName,
                        currentSlots[i].x + (SLOT_SIZE - layout.width) / 2,
                        currentSlots[i].y - 5);
            }
        }
    }

    private void drawItemDetails(SpriteBatch batch) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Character character = gameController.getCharacter();

        // Draw selected item details
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            Items item = currentItems.get(selectedItemIndex);

            float detailsX = merchantBounds.x + PADDING * 2 + 40;
            float detailsY = merchantBounds.y + 250;

            // Draw item name
            titleFont.setColor(titleColor);
            titleFont.draw(batch, item.getItemName(), detailsX, detailsY);

            // Draw item effect
            font.setColor(textColor);
            font.draw(batch, "Effect: " + item.getItemEffect(), detailsX, detailsY - 25);

            // Draw price
            int price = 0;
            try {
                price = item.getItemPrice();
            } catch (NumberFormatException e) {
                // Use default 0
            }

            if (showingMerchantItems) {
                font.setColor(character.getScore() >= price ? affordableColor : unaffordableColor);
                font.draw(batch, "Buy Price: " + price, detailsX, detailsY - 50);

                // Draw buy button
                font.setColor(Color.WHITE);
                layout.setText(font, "BUY");
                font.draw(batch, "BUY",
                        buyButton.x + (buyButton.width - layout.width) / 2,
                        buyButton.y + (buyButton.height + layout.height) / 2);
            } else {
                font.setColor(priceColor);
                font.draw(batch, "Sell Price: " + (price * 0.7), detailsX, detailsY - 50);

                // Draw sell button
                font.setColor(Color.WHITE);
                layout.setText(font, "SELL");
                font.draw(batch, "SELL",
                        sellButton.x + (sellButton.width - layout.width) / 2,
                        sellButton.y + (sellButton.height + layout.height) / 2);
            }

            // Draw item description with word wrapping
            font.setColor(textColor);
            font.draw(batch, item.getItemDescription(),
                    detailsX, detailsY - 75,
                    merchantBounds.width - PADDING * 4, Align.left, true);
        }
    }

    // Keep existing methods for functionality
    private void loadMerchantItems() {
        merchantItems = ItemLoader.getAllItemsWithoutNA();

        Gdx.app.log("MerchantUI", "Loaded " + merchantItems.size() + " merchant items");

        if (merchantItems.isEmpty()) {
            Gdx.app.error("MerchantUI", "No items loaded from ItemLoader! Check ItemLoader implementation.");
        }

        preloadItemTextures();
    }

    private void updatePlayerItems() {
        Character character = gameController.getCharacter();


        playerItems.clear();
        displayItems.clear();

        Map<String, Integer> characterItems = character.getItems();
        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            Items item = ItemLoader.getItemByName(entry.getKey());
            if (item.getItemEffect().equals("N/A")) {
                continue; // Skip items with "N/A" effect
            }
            if (item != null) {
                playerItems.put(item, entry.getValue());
                displayItems.add(item);
            }
        }
    }

    // The rest of the required functionality methods remain unchanged
    // (preloadItemTextures, getItemTexture, handleClick, buySelectedItem, sellSelectedItem, etc.)

    private void preloadItemTextures() {
        for (Items item : merchantItems) {
            if (item != null && item.getTexturePath() != null) {
                getItemTexture(item.getTexturePath());
            }
        }
    }

    private Texture getItemTexture(String texturePath) {
        if (!itemTextures.containsKey(texturePath)) {
            Texture texture = new Texture(Gdx.files.internal(texturePath));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            itemTextures.put(texturePath, texture);
        }
        return itemTextures.get(texturePath);
    }

    public boolean handleClick(int screenX, int screenY) {
        // Existing implementation...
        if (!visible) return false;

        // Convert to UI coordinates (origin at bottom-left)
        float uiY = Gdx.graphics.getHeight() - screenY;

        // Check if click is inside merchant bounds
        if (!merchantBounds.contains(screenX, uiY)) {
            return false;
        }

        // Check tabs first
        if (tabMerchantButton.contains(screenX, uiY)) {
            showingMerchantItems = true;
            selectedItemIndex = -1;
            return true;
        }

        if (tabPlayerButton.contains(screenX, uiY)) {
            showingMerchantItems = false;
            selectedItemIndex = -1;
            return true;
        }

        // Check close button
        if (closeButton.contains(screenX, uiY)) {
            visible = false;
            return true;
        }

        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;

        // Check item slots
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < currentItems.size() && currentSlots[i].contains(screenX, uiY)) {
                selectedItemIndex = i;
                return true;
            }
        }

        // Check action buttons when item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            if (showingMerchantItems && buyButton.contains(screenX, uiY)) {
                buySelectedItem();
                return true;
            } else if (!showingMerchantItems && sellButton.contains(screenX, uiY)) {
                sellSelectedItem();
                return true;
            }
        }

        return true;
    }

    private void buySelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= merchantItems.size()) return;

        Items item = merchantItems.get(selectedItemIndex);
        Character character = gameController.getCharacter();

        try {
            int price = item.getItemPrice();

            if (character.getScore() >= price) {
                character.addScore(-price);
                character.addItem(item, 1);
                Gdx.app.log("MerchantUI", "Item purchased: " + item.getItemName());
            } else {
                Gdx.app.log("MerchantUI", "Not enough score to buy this item");
            }
        } catch (NumberFormatException e) {
            Gdx.app.log("MerchantUI", "Invalid item price format");
        }
    }

    private void sellSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= displayItems.size()) return;

        Items item = displayItems.get(selectedItemIndex);
        Character character = gameController.getCharacter();

        try {
            int price = item.getItemPrice();
            float sellPrice = price *0.7f; // 70% of the buy price

            character.addScore((int)sellPrice);
            character.removeItem(item.getItemName(), 1);

            updatePlayerItems();
            if (selectedItemIndex >= displayItems.size()) {
                selectedItemIndex = displayItems.size() - 1;
                if (selectedItemIndex < 0) selectedItemIndex = -1;
            }

            Gdx.app.log("MerchantUI", "Item sold: " + item.getItemName() + " for " + sellPrice);
        } catch (NumberFormatException e) {
            Gdx.app.log("MerchantUI", "Invalid item price format");
        }
    }

    public void show() {
        visible = true;
        selectedItemIndex = -1;
        showingMerchantItems = true;
        updatePlayerItems();
        animTime = 0;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void onWindowResize() {
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float merchantWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW + 1) * PADDING) + 220;
        float merchantHeight = ((MAX_SLOTS / SLOTS_PER_ROW) * SLOT_SIZE) + (((MAX_SLOTS / SLOTS_PER_ROW) + 1) * PADDING) + 140;

        merchantBounds.set(
                (screenWidth - merchantWidth) / 2,
                (screenHeight - merchantHeight) / 2,
                merchantWidth,
                merchantHeight
        );

        createSlots();

        // Update tab and button positions
        tabMerchantButton.set(
                merchantBounds.x + PADDING,
                merchantBounds.y + merchantBounds.height - 36,
                tabMerchantButton.width,
                tabMerchantButton.height
        );

        tabPlayerButton.set(
                merchantBounds.x + tabMerchantButton.width + PADDING * 2,
                merchantBounds.y + merchantBounds.height - 36,
                tabPlayerButton.width,
                tabPlayerButton.height
        );

        closeButton.set(
                merchantBounds.x + merchantBounds.width - 36 - PADDING,
                merchantBounds.y + merchantBounds.height - 36 - PADDING,
                36,
                36
        );

        float buttonWidth = 140;
        float buttonHeight = 48;
        float buttonX = merchantBounds.x + merchantBounds.width - buttonWidth - PADDING * 2;
        float buttonY = merchantBounds.y + PADDING * 3;

        buyButton.set(buttonX, buttonY + buttonHeight + PADDING, buttonWidth, buttonHeight);
        sellButton.set(buttonX, buttonY, buttonWidth, buttonHeight);
    }

    public void dispose() {
        for (Texture texture : itemTextures.values()) {
            if (texture != null) texture.dispose();
        }
        itemTextures.clear();

        if (backgroundTexture != null) backgroundTexture.dispose();
        if (buttonTexture != null) buttonTexture.dispose();
        if (slotTexture != null) slotTexture.dispose();
        if (highlightTexture != null) highlightTexture.dispose();

        if (titleFont != null) titleFont.dispose();
        if (font != null) font.dispose();
        if (smallFont != null) smallFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}