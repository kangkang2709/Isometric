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

public class InventoryUI {
    private GameController gameController;
    private BitmapFont font;
    private boolean visible = false;

    private Texture buttonTexture;
    private Texture backgroundTexture;
    private Texture itemSlotTexture;
    private Texture itemSlotSelected;
    private ShapeRenderer shapeRenderer;

    private int selectedItemIndex = -1;
    private List<String> itemList = new ArrayList<>();
    private Rectangle inventoryBounds;
    private Rectangle[] itemSlots;
    private Rectangle useButton;
    private Rectangle discardButton;
    private Rectangle closeButton;

    private static final int SLOTS_PER_ROW = 5;
    private static final int MAX_SLOTS = 20;
    private static final int SLOT_SIZE = 64;
    private static final int PADDING = 10;

    private Map<String, Texture> itemTextures = new HashMap<>();
    private boolean inventoryDirty = true;
    private Matrix4 uiMatrix;

    // Cached positions for item details
    private float detailsX;
    private float detailsY;
    private float detailsWidth;

    // UI colors
    private final Color bgColor = new Color(0.2f, 0.2f, 0.2f, 0.9f);
    private final Color slotColor = new Color(0.3f, 0.3f, 0.3f, 1f);
    private final Color selectedColor = new Color(0.5f, 0.5f, 0.8f, 1f);
    private final Color buttonColor = new Color(0.4f, 0.4f, 0.4f, 1f);
    private final Color closeButtonColor = new Color(0.7f, 0.3f, 0.3f, 1f);

    public InventoryUI(GameController gameController) {
        this.gameController = gameController;
        this.font = new BitmapFont();
        this.shapeRenderer = new ShapeRenderer();

        // Create projection matrix once
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("ui/inventory_bg.png"));
        itemSlotTexture = new Texture(Gdx.files.internal("ui/slot.png"));
        itemSlotSelected = new Texture(Gdx.files.internal("ui/slot_highlight.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));

        // Calculate inventory bounds
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float inventoryWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW + 1) * PADDING) + 200;
        float inventoryHeight = ((MAX_SLOTS / SLOTS_PER_ROW) * SLOT_SIZE) + (((MAX_SLOTS / SLOTS_PER_ROW) + 1) * PADDING) + 100;

        inventoryBounds = new Rectangle(
                (screenWidth - inventoryWidth) / 2,
                (screenHeight - inventoryHeight) / 2,
                inventoryWidth,
                inventoryHeight
        );

        // Create item slots
        itemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            itemSlots[i] = new Rectangle(
                    inventoryBounds.x + PADDING + (col * (SLOT_SIZE + PADDING)),
                    inventoryBounds.y + inventoryBounds.height - PADDING - SLOT_SIZE - (row * (SLOT_SIZE + PADDING)),
                    SLOT_SIZE,
                    SLOT_SIZE
            );
        }

        // Create buttons
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonX = inventoryBounds.x + inventoryBounds.width - buttonWidth - PADDING;
        float buttonY = inventoryBounds.y + PADDING * 3;

        useButton = new Rectangle(buttonX, buttonY + buttonHeight + PADDING, buttonWidth, buttonHeight);
        discardButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        closeButton = new Rectangle(
                inventoryBounds.x + inventoryBounds.width - 30 - PADDING,
                inventoryBounds.y + inventoryBounds.height - 30 - PADDING,
                30,
                30
        );

        // Precalculate item details position
        detailsX = inventoryBounds.x + (SLOTS_PER_ROW * (SLOT_SIZE + PADDING)) + PADDING * 2;
        detailsY = inventoryBounds.y + inventoryBounds.height - PADDING * 3;
        detailsWidth = inventoryBounds.width - detailsX + inventoryBounds.x - PADDING * 2;

        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Preload common textures
        preloadCommonTextures();
    }
    /**
     * Renders the inventory UI.
     *
     * @param batch The SpriteBatch used for rendering.
     */




    String errorMessage = "";
    public void render(SpriteBatch batch) {
        if (!visible) return;

        // Only update when inventory has changed
        if (inventoryDirty) {
            updateItemList();
            inventoryDirty = false;
        }

        // Store current batch state
        Matrix4 prevMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Draw background using textures
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        // Draw main background texture
        if (backgroundTexture != null) {
            batch.draw(backgroundTexture,
                    inventoryBounds.x, inventoryBounds.y,
                    inventoryBounds.width, inventoryBounds.height);
        } else {
            // Fallback to ShapeRenderer if texture isn't available
            batch.end();
            shapeRenderer.setProjectionMatrix(uiMatrix);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(bgColor);
            shapeRenderer.rect(inventoryBounds.x, inventoryBounds.y,
                    inventoryBounds.width, inventoryBounds.height);
            shapeRenderer.end();
            batch.begin();
        }

        // Draw item slots using textures
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i == selectedItemIndex) {
                // Draw highlighted slot for selected item
                if (itemSlotSelected != null) {
                    batch.draw(itemSlotSelected,
                            itemSlots[i].x, itemSlots[i].y,
                            itemSlots[i].width, itemSlots[i].height);
                }
            } else {
                // Draw normal slot
                if (itemSlotTexture != null) {
                    batch.draw(itemSlotTexture,
                            itemSlots[i].x, itemSlots[i].y,
                            itemSlots[i].width, itemSlots[i].height);
                }
            }
        }

        // Draw buttons with textures
        // Draw buttons with textures
        if (buttonTexture != null) {
            // Use button texture for all buttons with hover effects
            if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
                Items item = ItemLoader.getItemByName(itemList.get(selectedItemIndex));
                if (item != null && !item.getItemEffect().equals("N/A")) {
                    // Use button
                    batch.setColor(hoverUseButton ? buttonHoverColor : buttonColor);
                    batch.draw(buttonTexture, useButton.x, useButton.y, useButton.width, useButton.height);

                    // Discard button
                    batch.setColor(hoverDiscardButton ? buttonHoverColor : buttonColor);
                    batch.draw(buttonTexture, discardButton.x, discardButton.y, discardButton.width, discardButton.height);
                }
            }

            // Use a red tint for the close button
            batch.setColor(hoverCloseButton ? closeButtonHoverColor : closeButtonColor);
            batch.draw(buttonTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);
            batch.setColor(Color.WHITE); // Reset color
        }

        // Draw item icons and quantities
        Character character = gameController.getCharacter();
        Map<String, Integer> items = character.getItems();
        int index = 0;

        font.setColor(Color.WHITE);
        for (String itemName : itemList) {
            if (index >= MAX_SLOTS) break;

            // Draw item icon
            Items item = ItemLoader.getItemByName(itemName);
            if (item != null && item.getTexturePath() != null) {
                Texture itemTexture = getItemTexture(item.getTexturePath());
                float itemSize = SLOT_SIZE - 16;
                float centerX = itemSlots[index].x + SLOT_SIZE/2f - itemSize/2f;
                float centerY = itemSlots[index].y + SLOT_SIZE/2f - itemSize/2f;

                batch.draw(itemTexture, centerX, centerY, itemSize, itemSize);

                // Draw quantity
                int quantity = items.get(itemName);
                if (quantity > 1) {
                    font.draw(batch, String.valueOf(quantity),
                            itemSlots[index].x + SLOT_SIZE - 20,
                            itemSlots[index].y + 20);
                }
            }
            index++;
        }

        // Draw item details if an item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null) {
                font.draw(batch, item.getItemName(), detailsX, detailsY);
                font.draw(batch, "Effect: " + item.getItemEffect(), detailsX, detailsY - 30);
                font.draw(batch, "Value: " + item.getValue(), detailsX, detailsY - 60);
                font.draw(batch, item.getItemDescription(), detailsX, detailsY - 90,
                        detailsWidth, -1, true);
                font.draw(batch, "Mana Cost: " + item.getManaCost(), detailsX, detailsY - 150);
                font.draw(batch, errorMessage, detailsX, detailsY - 180);

                // Only show action buttons if the item has an effect
                if (!item.getItemEffect().equals("N/A")) {
                    font.draw(batch, "USE", useButton.x + useButton.width/2 - 15,
                            useButton.y + useButton.height/2 + 7);
                    font.draw(batch, "DISCARD", discardButton.x + discardButton.width/2 - 35,
                            discardButton.y + discardButton.height/2 + 7);
                }
            }
        }

        // Draw close button text centered
        font.draw(batch, "X", closeButton.x + closeButton.width/2 - 5,
                closeButton.y + closeButton.height/2 + 7);

        batch.end();

        // Restore original batch state
        if (batchWasDrawing) {
            batch.setProjectionMatrix(prevMatrix);
            batch.begin();
        }
    }

    private void preloadCommonTextures() {
        // Preload commonly used item textures
        List<Items> commonItems = ItemLoader.getAllItems();
        if (commonItems != null) {
            for (Items item : commonItems) {
                if (item != null && item.getTexturePath() != null) {
                    getItemTexture(item.getTexturePath());
                }
            }
        }
    }

    private Texture getItemTexture(String texturePath) {
        if (!itemTextures.containsKey(texturePath)) {
            itemTextures.put(texturePath, new Texture(Gdx.files.internal(texturePath)));
        }
        return itemTextures.get(texturePath);
    }

    private void updateItemList() {
        itemList.clear();
        Character character = gameController.getCharacter();
        if (character.getItems() != null) {
            itemList.addAll(character.getItems().keySet());
        }
    }

    public boolean handleClick(int screenX, int screenY) {
        if (!visible) return false;

        // Convert to UI coordinates (origin at bottom-left)
        float uiY = Gdx.graphics.getHeight() - screenY;

        // Check if click is inside inventory bounds
        if (!inventoryBounds.contains(screenX, uiY)) {
            return false;
        }

        // Check close button
        if (closeButton.contains(screenX, uiY)) {
            visible = false;
            return true;
        }

        // Check item slots - only iterate through actual items
        int itemCount = Math.min(MAX_SLOTS, itemList.size());
        for (int i = 0; i < itemCount; i++) {
            if (itemSlots[i].contains(screenX, uiY)) {
                selectedItemIndex = i;
                return true;
            }
        }

        // Check action buttons when item is selected and has valid effect
        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            // Retrieve the item only once instead of potentially twice
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null && !item.getItemEffect().equals("N/A")) {
                if (useButton.contains(screenX, uiY)) {
                    useSelectedItem();
                    return true;
                } else if (discardButton.contains(screenX, uiY)) {
                    discardSelectedItem();
                    return true;
                }
            }
        }

        return true;
    }

    // Button hover states
    private boolean hoverUseButton = false;
    private boolean hoverDiscardButton = false;
    private boolean hoverCloseButton = false;

    // Hover colors
    private final Color buttonHoverColor = new Color(0.5f, 0.5f, 0.5f, 1f);
    private final Color closeButtonHoverColor = new Color(0.8f, 0.4f, 0.4f, 1f);
    public boolean handleMouseMove(int screenX, int screenY) {
        if (!visible) return false;

        // Convert to UI coordinates (origin at bottom-left)
        float uiY = Gdx.graphics.getHeight() - screenY;

        // Reset hover states
        hoverUseButton = false;
        hoverDiscardButton = false;
        hoverCloseButton = false;

        // Check if mouse is inside inventory bounds
        if (!inventoryBounds.contains(screenX, uiY)) {
            return false;
        }

        // Check close button
        hoverCloseButton = closeButton.contains(screenX, uiY);

        // Check action buttons when an item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null && !item.getItemEffect().equals("N/A")) {
                hoverUseButton = useButton.contains(screenX, uiY);
                hoverDiscardButton = discardButton.contains(screenX, uiY);
            }
        }

        return true;
    }

    private void useSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= itemList.size()) return;

        String itemName = itemList.get(selectedItemIndex);
        Items item = ItemLoader.getItemByName(itemName);

        if (item != null) {
            try {
                gameController.getCharacter().useItem(item);
                // If item was fully consumed, reset selection
                if (!gameController.getCharacter().hasItem(itemName)) {
                    selectedItemIndex = -1;
                }
                // Mark inventory as dirty since items changed
                inventoryDirty = true;
            } catch (IllegalStateException e) {
                errorMessage =e.getMessage();
            }
            catch (IllegalArgumentException e) {
               errorMessage = e.getMessage();
            }
        }
    }

    private void discardSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= itemList.size()) return;

        String itemName = itemList.get(selectedItemIndex);
        Items item = ItemLoader.getItemByName(itemName);

        if (item != null) {
            gameController.getCharacter().deleteItem(item);
            selectedItemIndex = -1;
            // Mark inventory as dirty since items changed
            inventoryDirty = true;
        }
    }

    public void show() {
        visible = true;
        selectedItemIndex = -1;
        errorMessage="";
        inventoryDirty = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void notifyItemsChanged() {
        inventoryDirty = true;
    }

    public void onWindowResize() {
        // Update projection matrix
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Recalculate UI positions
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float inventoryWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW + 1) * PADDING) + 200;
        float inventoryHeight = ((MAX_SLOTS / SLOTS_PER_ROW) * SLOT_SIZE) + (((MAX_SLOTS / SLOTS_PER_ROW) + 1) * PADDING) + 100;

        inventoryBounds.set(
                (screenWidth - inventoryWidth) / 2,
                (screenHeight - inventoryHeight) / 2,
                inventoryWidth,
                inventoryHeight
        );

        // Update cached positions
        detailsX = inventoryBounds.x + (SLOTS_PER_ROW * (SLOT_SIZE + PADDING)) + PADDING * 2;
        detailsY = inventoryBounds.y + inventoryBounds.height - PADDING * 3;
        detailsWidth = inventoryBounds.width - detailsX + inventoryBounds.x - PADDING * 2;
    }

    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (itemSlotTexture != null) itemSlotTexture.dispose();
        if (itemSlotSelected != null) itemSlotSelected.dispose();

        // Dispose all cached item textures
        for (Texture texture : itemTextures.values()) {
            if (texture != null) texture.dispose();
        }
        itemTextures.clear();

        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}