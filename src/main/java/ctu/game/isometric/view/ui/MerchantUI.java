package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerchantUI {
    private GameController gameController;
    private BitmapFont font;
    private BitmapFont smallFont;
    private boolean visible = false;

    private ShapeRenderer shapeRenderer;

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

    private static final int SLOTS_PER_ROW = 5;
    private static final int MAX_SLOTS = 15;
    private static final int SLOT_SIZE = 64;
    private static final int PADDING = 10;

    private Map<String, Texture> itemTextures = new HashMap<>();
    private Matrix4 uiMatrix;

    // UI colors
    private final Color bgColor = new Color(0.2f, 0.2f, 0.3f, 0.9f);
    private final Color slotColor = new Color(0.3f, 0.3f, 0.3f, 1f);
    private final Color selectedColor = new Color(0.5f, 0.5f, 0.8f, 1f);
    private final Color buttonColor = new Color(0.4f, 0.4f, 0.4f, 1f);
    private final Color activeTabColor = new Color(0.5f, 0.5f, 0.8f, 1f);
    private final Color inactiveTabColor = new Color(0.3f, 0.3f, 0.4f, 1f);
    private final Color closeButtonColor = new Color(0.7f, 0.3f, 0.3f, 1f);
    private final Color priceColor = new Color(0.9f, 0.9f, 0.2f, 1f);
    private final Color affordableColor = new Color(0.2f, 0.8f, 0.2f, 1f);
    private final Color unaffordableColor = new Color(0.8f, 0.2f, 0.2f, 1f);

    public MerchantUI(GameController gameController) {
        this.gameController = gameController;
        this.font = new BitmapFont();
        this.smallFont = new BitmapFont();
        smallFont.getData().setScale(0.8f);
        this.shapeRenderer = new ShapeRenderer();

        // Create projection matrix
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Calculate merchant UI bounds
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float merchantWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW + 1) * PADDING) + 200;
        float merchantHeight = ((MAX_SLOTS / SLOTS_PER_ROW) * SLOT_SIZE) + (((MAX_SLOTS / SLOTS_PER_ROW) + 1) * PADDING) + 100;

        merchantBounds = new Rectangle(
                (screenWidth - merchantWidth) / 2,
                (screenHeight - merchantHeight) / 2,
                merchantWidth,
                merchantHeight
        );

        // Create item slots
        itemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            itemSlots[i] = new Rectangle(
                    merchantBounds.x + PADDING + (col * (SLOT_SIZE + PADDING)),
                    merchantBounds.y + merchantBounds.height - PADDING - 60 - SLOT_SIZE - (row * (SLOT_SIZE + PADDING)),
                    SLOT_SIZE,
                    SLOT_SIZE
            );
        }

        // Create player item slots (same positions, just swapped when viewing inventory)
        playerItemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            playerItemSlots[i] = new Rectangle(itemSlots[i].x, itemSlots[i].y, SLOT_SIZE, SLOT_SIZE);
        }

        // Create buttons
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonX = merchantBounds.x + merchantBounds.width - buttonWidth - PADDING;
        float buttonY = merchantBounds.y + PADDING * 3;

        // Create tabs
        float tabWidth = 120;
        float tabHeight = 30;
        tabMerchantButton = new Rectangle(
                merchantBounds.x + PADDING,
                merchantBounds.y + merchantBounds.height - 30,
                tabWidth,
                tabHeight
        );
        tabPlayerButton = new Rectangle(
                merchantBounds.x + tabWidth + PADDING * 2,
                merchantBounds.y + merchantBounds.height - 30,
                tabWidth,
                tabHeight
        );

        buyButton = new Rectangle(buttonX, buttonY + buttonHeight + PADDING, buttonWidth, buttonHeight);
        sellButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        closeButton = new Rectangle(
                merchantBounds.x + merchantBounds.width - 30 - PADDING,
                merchantBounds.y + merchantBounds.height - 30 - PADDING,
                30,
                30
        );

        // Load merchant items
        loadMerchantItems();
    }

    private void loadMerchantItems() {
        merchantItems = ItemLoader.getAllItems();

        // Debug: Log how many items were loaded
        Gdx.app.log("MerchantUI", "Loaded " + merchantItems.size() + " merchant items");

        // Ensure we have items, otherwise load some default items for testing
        if (merchantItems.isEmpty()) {
            Gdx.app.error("MerchantUI", "No items loaded from ItemLoader! Check ItemLoader implementation.");
            // You could add fallback items here if needed
        }

        preloadItemTextures();
    }

    private void updatePlayerItems() {
        Character character = gameController.getCharacter();
        playerItems.clear();
        displayItems.clear();

        // Convert character's item map to our display format
        Map<String, Integer> characterItems = character.getItems();
        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            Items item = ItemLoader.getItemByName(entry.getKey());
            if (item != null) {
                playerItems.put(item, entry.getValue());
                displayItems.add(item);
            }
        }
    }

    private void preloadItemTextures() {
        for (Items item : merchantItems) {
            if (item != null && item.getTexturePath() != null) {
                getItemTexture(item.getTexturePath());
            }
        }
    }

    private Texture getItemTexture(String texturePath) {
        if (!itemTextures.containsKey(texturePath)) {
            itemTextures.put(texturePath, new Texture(Gdx.files.internal(texturePath)));
        }
        return itemTextures.get(texturePath);
    }

    public void render(SpriteBatch batch) {
        if (!visible) return;

        // Update player items each render to ensure it's current
        updatePlayerItems();

        // Store current batch state
        Matrix4 prevMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Draw background and UI elements
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw main background
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(merchantBounds.x, merchantBounds.y, merchantBounds.width, merchantBounds.height);

        // Draw tabs
        shapeRenderer.setColor(showingMerchantItems ? activeTabColor : inactiveTabColor);
        shapeRenderer.rect(tabMerchantButton.x, tabMerchantButton.y, tabMerchantButton.width, tabMerchantButton.height);

        shapeRenderer.setColor(showingMerchantItems ? inactiveTabColor : activeTabColor);
        shapeRenderer.rect(tabPlayerButton.x, tabPlayerButton.y, tabPlayerButton.width, tabPlayerButton.height);

        // Draw item slots
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i == selectedItemIndex) {
                shapeRenderer.setColor(selectedColor);
            } else {
                shapeRenderer.setColor(slotColor);
            }

            if (i < currentItems.size()) {
                shapeRenderer.rect(currentSlots[i].x, currentSlots[i].y, currentSlots[i].width, currentSlots[i].height);
            }
        }

        // Draw buttons
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            shapeRenderer.setColor(buttonColor);
            if (showingMerchantItems) {
                shapeRenderer.rect(buyButton.x, buyButton.y, buyButton.width, buyButton.height);
            } else {
                shapeRenderer.rect(sellButton.x, sellButton.y, sellButton.width, sellButton.height);
            }
        }

        shapeRenderer.setColor(closeButtonColor);
        shapeRenderer.rect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);

        shapeRenderer.end();

        // Configure batch for UI rendering
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        // Draw tabs text
        font.setColor(Color.WHITE);
        font.draw(batch, "Merchant", tabMerchantButton.x + 30, tabMerchantButton.y + 20);
        font.draw(batch, "Your Items", tabPlayerButton.x + 25, tabPlayerButton.y + 20);

        // Draw character score
        Character character = gameController.getCharacter();
        font.setColor(priceColor);
        font.draw(batch, "Your Score: " + character.getScore(),
                merchantBounds.x + merchantBounds.width - 200, merchantBounds.y + merchantBounds.height - 20);

        // Draw item icons and prices
        if (showingMerchantItems) {
            // Draw merchant items
            for (int i = 0; i < Math.min(merchantItems.size(), MAX_SLOTS); i++) {
                Items item = merchantItems.get(i);
                if (item != null && item.getTexturePath() != null) {
                    // Draw the item icon
                    Texture itemTexture = getItemTexture(item.getTexturePath());
                    batch.draw(itemTexture,
                            itemSlots[i].x + 8,
                            itemSlots[i].y + 8,
                            SLOT_SIZE - 16,
                            SLOT_SIZE - 16);

                    // Draw price with color based on affordability
                    try {
                        int price = item.getItemPrice();
                        smallFont.setColor(character.getScore() >= price ? affordableColor : unaffordableColor);
                        smallFont.draw(batch, price + "", itemSlots[i].x + 5, itemSlots[i].y + 15);
                    } catch (NumberFormatException e) {
                        // If price isn't valid, don't show it
                    }

                    // Draw item name
                    smallFont.setColor(Color.WHITE);
                    String itemName = item.getItemName();
                    if (itemName.length() > 10) {
                        itemName = itemName.substring(0, 7) + "...";
                    }
                    smallFont.draw(batch, itemName, itemSlots[i].x, itemSlots[i].y - 5);
                }
            }
        } else {
            // Draw player items with quantities
            for (int i = 0; i < Math.min(displayItems.size(), MAX_SLOTS); i++) {
                Items item = displayItems.get(i);
                int quantity = playerItems.get(item);

                if (item != null && item.getTexturePath() != null) {
                    // Draw the item icon
                    Texture itemTexture = getItemTexture(item.getTexturePath());
                    batch.draw(itemTexture,
                            playerItemSlots[i].x + 8,
                            playerItemSlots[i].y + 8,
                            SLOT_SIZE - 16,
                            SLOT_SIZE - 16);

                    // Draw quantity
                    smallFont.setColor(Color.WHITE);
                    smallFont.draw(batch, "x" + quantity, playerItemSlots[i].x + SLOT_SIZE - 20,
                            playerItemSlots[i].y + SLOT_SIZE - 10);

                    // Draw sell price
                    try {
                        int price = item.getItemPrice() / 2;
                        smallFont.setColor(priceColor);
                        smallFont.draw(batch, price + "", playerItemSlots[i].x + 5, playerItemSlots[i].y + 15);
                    } catch (NumberFormatException e) {
                        // If price isn't valid, don't show it
                    }

                    // Draw item name
                    smallFont.setColor(Color.WHITE);
                    String itemName = item.getItemName();
                    if (itemName.length() > 10) {
                        itemName = itemName.substring(0, 7) + "...";
                    }
                    smallFont.draw(batch, itemName, playerItemSlots[i].x, playerItemSlots[i].y - 5);
                }
            }
        }

        // Draw item details if an item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < currentItems.size()) {
            Items item = currentItems.get(selectedItemIndex);

            float detailsX = merchantBounds.x + PADDING;
            float detailsY = merchantBounds.y + 60;
            float detailsWidth = merchantBounds.width - PADDING * 2;

            font.setColor(Color.WHITE);
            font.draw(batch, item.getItemName(), detailsX, detailsY);
            font.draw(batch, "Effect: " + item.getItemEffect(), detailsX, detailsY - 25);

            int price = 0;
            try {
                price = item.getItemPrice();
            } catch (NumberFormatException e) {
                // Use default 0
            }

            if (showingMerchantItems) {
                font.setColor(character.getScore() >= price ? affordableColor : unaffordableColor);
                font.draw(batch, "Buy Price: " + price, detailsX, detailsY - 50);

                // Draw buy button text
                font.setColor(Color.WHITE);
                font.draw(batch, "BUY", buyButton.x + 45, buyButton.y + 25);
            } else {
                font.setColor(priceColor);
                font.draw(batch, "Sell Price: " + (price/2), detailsX, detailsY - 50);

                // Draw sell button text
                font.setColor(Color.WHITE);
                font.draw(batch, "SELL", sellButton.x + 45, sellButton.y + 25);
            }

            font.setColor(Color.LIGHT_GRAY);
            smallFont.draw(batch, item.getItemDescription(), detailsX, detailsY - 75,
                    detailsWidth * 0.8f, -1, true);
        }

        // Draw close button text
        font.setColor(Color.WHITE);
        font.draw(batch, "X", closeButton.x + 10, closeButton.y + 20);

        batch.end();

        // Restore original batch state
        if (batchWasDrawing) {
            batch.setProjectionMatrix(prevMatrix);
            batch.begin();
        }
    }

    public boolean handleClick(int screenX, int screenY) {
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

        return true; // Click was inside merchant UI
    }

    private void buySelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= merchantItems.size()) return;

        Items item = merchantItems.get(selectedItemIndex);
        Character character = gameController.getCharacter();

        try {
            int price = item.getItemPrice();

            // Check if character has enough score
            if (character.getScore() >= price) {
                character.addScore(-price);
                character.addItem(item, 1); // Add 1 quantity of the item
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
            // Give half price when selling
            int sellPrice = price / 2;

            // Add score and remove item
            character.addScore(sellPrice);
            character.removeItem(item.getItemName(), 1);

            // Update player items list and reset selection to avoid out-of-bounds
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
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void onWindowResize() {
        // Update projection matrix
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Recalculate UI positions
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float merchantWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW + 1) * PADDING) + 200;
        float merchantHeight = ((MAX_SLOTS / SLOTS_PER_ROW) * SLOT_SIZE) + (((MAX_SLOTS / SLOTS_PER_ROW) + 1) * PADDING) + 100;

        merchantBounds.set(
                (screenWidth - merchantWidth) / 2,
                (screenHeight - merchantHeight) / 2,
                merchantWidth,
                merchantHeight
        );

        // Update all UI components based on new positions
    }

    public void dispose() {
        // Dispose all cached item textures
        for (Texture texture : itemTextures.values()) {
            if (texture != null) texture.dispose();
        }
        itemTextures.clear();

        if (font != null) font.dispose();
        if (smallFont != null) smallFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}