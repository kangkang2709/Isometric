package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class InventoryUI {
    private GameController gameController;
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private boolean visible = false;

    private Texture backgroundTexture;
    private Texture panelTexture;
    private Texture itemSlotTexture;
    private Texture itemSlotSelected;
    private Texture itemSlotHover;
    private Texture buttonTexture;
    private Texture buttonHoverTexture;
    private Texture iconTexture;
    private ShapeRenderer shapeRenderer;
    private ShaderProgram blurShader;

    // Animation parameters
    private float animationState = 0;
    private boolean isOpening = false;
    private boolean isClosing = false;
    private final float ANIMATION_DURATION = 0.3f;
    private long lastPulseTime = 0;

    private int selectedItemIndex = -1;
    private int hoveredSlotIndex = -1;
    private List<String> itemList = new ArrayList<>();
    private Rectangle inventoryBounds;
    private Rectangle[] itemSlots;
    private Rectangle useButton;
    private Rectangle discardButton;
    private Rectangle closeButton;
    private Rectangle craftButton;

    private static final int SLOTS_PER_ROW = 3;
    private static final int MAX_SLOTS = 21;
    private static final int SLOT_SIZE = 60;
    private static final int PADDING = 12;

    private Map<String, Texture> itemTextures = new HashMap<>();
    private boolean inventoryDirty = true;
    private Matrix4 uiMatrix;

    // Cached positions for item details
    private float detailsX;
    private float detailsY;
    private float detailsWidth;

    private final Color closeButtonColor = new Color(0.7f, 0.3f, 0.3f, 0.9f);
    private final Color closeButtonHoverColor = new Color(0.9f, 0.4f, 0.4f, 1f);
    private final Color titleColor = new Color(0.9f, 0.9f, 1f, 1f);
    private final Color headingColor = new Color(0.7f, 0.8f, 1f, 1f);
    private final Color textColor = new Color(0.85f, 0.85f, 0.9f, 1f);
    private final Color accentColor = new Color(0.5f, 0.8f, 1f, 1f);
    private final Color errorColor = new Color(1f, 0.5f, 0.5f, 1f);

    // Button hover states
    private boolean hoverUseButton = false;
    private boolean hoverDiscardButton = false;
    private boolean hoverCloseButton = false;
    private boolean hoverCraftButton = false;

    private static final Map<String, CraftingRecipe> CRAFTING_RECIPES = new HashMap<>();
    String errorMessage = "";

    private static class CraftingRecipe {
        String ingredient;
        int ingredientCount;
        String result;

        CraftingRecipe(String ingredient, int ingredientCount, String result) {
            this.ingredient = ingredient;
            this.ingredientCount = ingredientCount;
            this.result = result;
        }
    }

    static {
        CRAFTING_RECIPES.put("Healing Herb", new CraftingRecipe("Healing Herb", 2, "Elixir"));
        CRAFTING_RECIPES.put("Mana Blossom", new CraftingRecipe("Mana Blossom", 2, "Arcane Essence"));
    }

    public InventoryUI(GameController gameController) {
        this.gameController = gameController;
        this.font = gameController.getCommonFont();
        this.titleFont = gameController.getBigCommonFont();

        this.buttonFont = font;
        this.buttonFont.getData().setScale(0.9f);

        this.shapeRenderer = new ShapeRenderer();

        // Create projection matrix once
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Load textures - add new textures
        loadTextures();

        // Calculate inventory bounds
        calculateLayout();

        // Initialize blur shader
        initializeShaders();

        // Preload item textures
        preloadCommonTextures();
    }

    private void loadTextures() {
        // Load enhanced textures with FF7 Remake inspiration
        backgroundTexture = new Texture(Gdx.files.internal("ui/panel-1.png"));
        panelTexture = new Texture(Gdx.files.internal("ui/panel-1.png"));
        itemSlotTexture = new Texture(Gdx.files.internal("ui/item-slot-1.png"));
        itemSlotSelected = new Texture(Gdx.files.internal("ui/item-slot-2.png"));
        itemSlotHover = new Texture(Gdx.files.internal("ui/item-slot-3.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
        buttonHoverTexture = new Texture(Gdx.files.internal("ui/button_selected.png"));
        iconTexture = new Texture(Gdx.files.internal("ui/icon2-chest.png"));

        // Set texture filtering for smooth scaling
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        panelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        itemSlotTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        itemSlotSelected.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        itemSlotHover.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        buttonHoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void calculateLayout() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // More elegant proportions for inventory window
        float inventoryWidth = Math.min(screenWidth * 0.8f, 900);
        float inventoryHeight = Math.min(screenHeight * 0.8f, 600);

        inventoryBounds = new Rectangle(
                (screenWidth - inventoryWidth) / 2,
                (screenHeight - inventoryHeight) / 2,
                inventoryWidth,
                inventoryHeight
        );

        // Create item slots with improved spacing
        itemSlots = new Rectangle[MAX_SLOTS];
        float slotAreaWidth = (SLOTS_PER_ROW * SLOT_SIZE) + ((SLOTS_PER_ROW - 1) * PADDING);
        float startX = inventoryBounds.x + (inventoryBounds.width * 0.3f - slotAreaWidth) / 2;

        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            itemSlots[i] = new Rectangle(
                    startX + (col * (SLOT_SIZE + PADDING)),
                    inventoryBounds.y + inventoryHeight - SLOT_SIZE * 2 - 8 - (row * (SLOT_SIZE + PADDING)),
                    SLOT_SIZE,
                    SLOT_SIZE
            );
        }

        // Create buttons with consistent sizing and placement
        float buttonWidth = 140;
        float buttonHeight = 45;
        float buttonX = inventoryBounds.x + inventoryBounds.width - buttonWidth - PADDING * 2;
        float buttonStartY = inventoryBounds.y + PADDING * 4;
        float buttonSpacing = buttonHeight + PADDING;

        discardButton = new Rectangle(buttonX, buttonStartY, buttonWidth, buttonHeight);
        useButton = new Rectangle(buttonX, buttonStartY + buttonSpacing, buttonWidth, buttonHeight);
        craftButton = new Rectangle(buttonX, buttonStartY + buttonSpacing * 2, buttonWidth, buttonHeight);

        // Modern close button (X in corner)
        closeButton = new Rectangle(
                inventoryBounds.x + inventoryBounds.width - 36 - PADDING,
                inventoryBounds.y + inventoryBounds.height - 36 - PADDING,
                36,
                36
        );

        // Precalculate item details position - more spacious layout
        detailsX = inventoryBounds.x + inventoryBounds.width * 0.32f;
        detailsY = inventoryBounds.y + inventoryBounds.height - 80;
        detailsWidth = inventoryBounds.width * 0.65f;
    }

    private void initializeShaders() {
        // Simple blur shader for modern UI feel
        String vertexShader = "attribute vec4 a_position;\n"
                + "attribute vec2 a_texCoord0;\n"
                + "varying vec2 v_texCoord;\n"
                + "void main() {\n"
                + "   gl_Position = a_position;\n"
                + "   v_texCoord = a_texCoord0;\n"
                + "}";

        String fragmentShader = "#ifdef GL_ES\n"
                + "precision mediump float;\n"
                + "#endif\n"
                + "varying vec2 v_texCoord;\n"
                + "uniform sampler2D u_texture;\n"
                + "uniform float u_blur_radius;\n"
                + "void main() {\n"
                + "   vec4 color = vec4(0.0);\n"
                + "   float blurSize = 1.0/512.0 * u_blur_radius;\n"
                + "   for(float x = -4.0; x <= 4.0; x++) {\n"
                + "       for(float y = -4.0; y <= 4.0; y++) {\n"
                + "           color += texture2D(u_texture, v_texCoord + vec2(x, y) * blurSize) / 81.0;\n"
                + "       }\n"
                + "   }\n"
                + "   gl_FragColor = color;\n"
                + "}";

        try {
            blurShader = new ShaderProgram(vertexShader, fragmentShader);
            if (!blurShader.isCompiled()) {
                System.err.println("Shader compilation failed: " + blurShader.getLog());
            }
        } catch (Exception e) {
            System.err.println("Could not load shader: " + e.getMessage());
            blurShader = null;
        }
    }

    public void render(SpriteBatch batch) {
        if (!visible && !isClosing) return;

        // Update animation state
        updateAnimation();

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

        // Apply animation scaling/alpha
        float scale = isOpening || isClosing ? animationState : 1.0f;
        float alpha = isOpening || isClosing ? animationState : 1.0f;

        // Setup rendering
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        // Calculate pulsing effect for selected items
        float pulseAmount = 0;
        if (selectedItemIndex >= 0) {
            long time = TimeUtils.millis();
            pulseAmount = (float) Math.sin((time - lastPulseTime) / 400.0) * 0.1f + 0.1f;
        }

        // Draw background panel with blur effect
        if (backgroundTexture != null) {
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, alpha * 0.95f);

            // Draw with scale animation
            float width = inventoryBounds.width * scale;
            float height = inventoryBounds.height * scale;
            float x = inventoryBounds.x + (inventoryBounds.width - width) / 2;
            float y = inventoryBounds.y + (inventoryBounds.height - height) / 2;

            batch.draw(backgroundTexture, x, y, width, height);

            // Draw panel dividers
            batch.draw(panelTexture,
                    x + inventoryBounds.width * 0.3f * scale, y,
                    2, height);

            // Draw title area
            batch.draw(panelTexture,
                    x, y + height - 60 * scale,
                    width, 2);

            batch.setColor(c);
        }

        // Draw inventory title
        titleFont.setColor(titleColor.r, titleColor.g, titleColor.b, alpha);
        titleFont.draw(batch, "INVENTORY",
                inventoryBounds.x + 80,
                inventoryBounds.y + inventoryBounds.height - 20);

        // Draw inventory icon
        batch.draw(iconTexture,
                inventoryBounds.x + 20,
                inventoryBounds.y + inventoryBounds.height - 50,
                40, 40);

        // Draw item slots with hover and selection effects
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i == selectedItemIndex) {
                // Draw highlighted slot with pulse effect
                float extraSize = pulseAmount * SLOT_SIZE;
                batch.setColor(1, 1, 1, alpha);
                batch.draw(itemSlotSelected,
                        itemSlots[i].x - extraSize / 2, itemSlots[i].y - extraSize / 2,
                        itemSlots[i].width + extraSize, itemSlots[i].height + extraSize);
            } else if (i == hoveredSlotIndex) {
                // Draw hover effect
                batch.setColor(1, 1, 1, alpha);
                batch.draw(itemSlotHover,
                        itemSlots[i].x, itemSlots[i].y,
                        itemSlots[i].width, itemSlots[i].height);
            } else {
                // Draw normal slot
                batch.setColor(1, 1, 1, alpha);
                batch.draw(itemSlotTexture,
                        itemSlots[i].x, itemSlots[i].y,
                        itemSlots[i].width, itemSlots[i].height);
            }
        }

        // Draw buttons with hover effects
        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            Items item = ItemLoader.getItemByName(itemList.get(selectedItemIndex));
            if (item != null) {
                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest") && !item.getItemEffect().equals("debuff")) {
                    // Use button
                    drawButton(batch, useButton, "USE", hoverUseButton, alpha);
                }

                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest")) {
                    // Discard button
                    drawButton(batch, discardButton, "DISCARD", hoverDiscardButton, alpha);
                }

                if (item.getItemEffect().equals("craft")) {
                    // Craft button
                    drawButton(batch, craftButton, "CRAFT", hoverCraftButton, alpha);
                }
            }
        }

        // Draw close button
        batch.setColor(hoverCloseButton ? closeButtonHoverColor : closeButtonColor);
        batch.draw(buttonTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);

        buttonFont.setColor(Color.WHITE);
        buttonFont.draw(batch, "X",
                closeButton.x + closeButton.width / 2 - 8,
                closeButton.y + closeButton.height / 2 + 8);

        // Draw item icons and quantities
        Character character = gameController.getCharacter();
        Map<String, Integer> items = character.getItems();
        int index = 0;

        font.setColor(textColor.r, textColor.g, textColor.b, alpha);
        for (String itemName : itemList) {
            if (index >= MAX_SLOTS) break;

            // Draw item icon
            Items item = ItemLoader.getItemByName(itemName);
            if (item != null && item.getTexturePath() != null) {
                Texture itemTexture = getItemTexture(item.getItemName(), item.getTexturePath());
                float itemSize = SLOT_SIZE - 16;
                float centerX = itemSlots[index].x + SLOT_SIZE / 2f - itemSize / 2f;
                float centerY = itemSlots[index].y + SLOT_SIZE / 2f - itemSize / 2f;

                batch.setColor(1, 1, 1, alpha);
                batch.draw(itemTexture, centerX, centerY, itemSize, itemSize);

                // Draw quantity with improved visibility
                Integer quantityObj = items.get(itemName);
                if (quantityObj != null) {
                    int quantity = quantityObj.intValue();
                    if (quantity > 1) {
                        // Draw small background circle for quantity
                        shapeRenderer.setProjectionMatrix(uiMatrix);
                        batch.end();
                        Gdx.gl.glEnable(GL20.GL_BLEND);
                        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.8f * alpha);
                        shapeRenderer.circle(itemSlots[index].x + SLOT_SIZE - 16, itemSlots[index].y + 16, 12);
                        shapeRenderer.end();
                        batch.begin();

                        // Draw quantity text
                        font.setColor(1, 1, 1, alpha);
                        font.draw(batch, String.valueOf(quantity),
                                itemSlots[index].x + SLOT_SIZE - 22,
                                itemSlots[index].y + 22);
                    }
                }
            }
            index++;
        }

        // Draw item details if an item is selected
        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null) {
                // Draw item texture larger in details panel
                Texture itemTexture = getItemTexture(item.getItemName(), item.getTexturePath());
                batch.setColor(1, 1, 1, alpha);
                batch.draw(itemTexture,
                        detailsX,
                        detailsY - 80,
                        80, 80);

                // Draw item name and details with proper styling
                titleFont.setColor(headingColor.r, headingColor.g, headingColor.b, alpha);
                titleFont.draw(batch, item.getItemName(), detailsX + 90, detailsY);

                font.setColor(textColor.r, textColor.g, textColor.b, alpha);

                // Draw item stats with icons or visual styling
                String itemType = !item.getItemEffect().equals("N/A") ? item.getItemEffect() : "KEY ITEM";
                drawItemStat(batch, "Effect:", itemType, detailsX + 90, detailsY - 40);
                drawItemStat(batch, "Value:", String.valueOf(item.getValue()), detailsX + 90, detailsY - 70);
                drawItemStat(batch, "Mana:", String.valueOf(item.getManaCost()), detailsX + 90, detailsY - 100);

                // Draw description with word wrapping
                font.draw(batch, item.getItemDescription(),
                        detailsX + 40, detailsY - 140,
                        detailsWidth - 40, -1, true);

                // Draw error message with animation if present
                if (!errorMessage.isEmpty()) {
                    float errorPulse = (float) Math.sin(TimeUtils.millis() / 200.0) * 0.2f + 0.8f;
                    font.setColor(errorColor.r, errorColor.g, errorColor.b, errorPulse * alpha);
                    font.draw(batch, errorMessage,
                            detailsX + 120,
                            inventoryBounds.y + 90);
                }

                // Draw usage instructions based on item type
                font.setColor(accentColor.r, accentColor.g, accentColor.b, alpha * 0.9f);
                switch (item.getItemEffect()) {
                    case "heal":
                        font.draw(batch, "*Used to restore health or mana.",
                                detailsX, inventoryBounds.y + 40);
                        break;
                    case "buff":
                        font.draw(batch, "*Used to enhance character stats.",
                                detailsX, inventoryBounds.y + 40);
                        break;
                    case "debuff":
                        font.draw(batch, "*Applies negative effects to enemies\n Only usable in battle.",
                                detailsX, inventoryBounds.y + 50);
                        break;
                    case "quest":
                        font.draw(batch, "*Quest item.\n Usually cannot be used directly.",
                                detailsX, inventoryBounds.y + 50);
                        break;
                    case "N/A":
                        font.draw(batch, "*Story-related item.\n Has no direct effect in battle.",
                                detailsX, inventoryBounds.y + 50);
                        break;
                    case "craft":
                        font.draw(batch, "*Material used to craft other items.",
                                detailsX, inventoryBounds.y + 40);
                        CraftingRecipe recipe = CRAFTING_RECIPES.get(item.getItemName());
                        if (recipe != null) {
                            font.draw(batch, "Crafting: " + recipe.ingredientCount + " " + recipe.ingredient + " → 1 " + recipe.result,
                                    detailsX, inventoryBounds.y + 20);
                        }
                        break;
                }
            }
        }

        batch.end();

        // Restore original batch state
        if (batchWasDrawing) {
            batch.setProjectionMatrix(prevMatrix);
            batch.begin();
        }
    }

    private final GlyphLayout layout = new GlyphLayout(); // Declare once, reuse to avoid GC

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text, boolean hovered, float alpha) {
        Texture buttonTex = hovered ? buttonHoverTexture : buttonTexture;
        batch.setColor(1, 1, 1, alpha);
        batch.draw(buttonTex, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        buttonFont.setColor(1, 1, 1, alpha);
        layout.setText(buttonFont, text);
        float textX = buttonRect.x + (buttonRect.width - layout.width) / 2f;
        float textY = buttonRect.y + (buttonRect.height + layout.height) / 2f;

        buttonFont.draw(batch, layout, textX, textY);
    }


    private void drawItemStat(SpriteBatch batch, String label, String value, float x, float y) {
        font.setColor(accentColor.r, accentColor.g, accentColor.b, batch.getColor().a);
        font.draw(batch, label, x, y);
        font.setColor(textColor.r, textColor.g, textColor.b, batch.getColor().a);
        font.draw(batch, value, x + 100, y);
    }

    private void updateAnimation() {
        if (isOpening) {
            animationState += Gdx.graphics.getDeltaTime() / ANIMATION_DURATION;
            if (animationState >= 1.0f) {
                animationState = 1.0f;
                isOpening = false;
            }
        } else if (isClosing) {
            animationState -= Gdx.graphics.getDeltaTime() / ANIMATION_DURATION;
            if (animationState <= 0.0f) {
                animationState = 0.0f;
                isClosing = false;
                visible = false;
            }
        }
    }

    public void show() {
        visible = true;
        selectedItemIndex = -1;
        errorMessage = "";
        inventoryDirty = true;
        isOpening = true;
        isClosing = false;
        animationState = 0.0f;
        lastPulseTime = TimeUtils.millis();
    }

    public void hide() {
        isClosing = true;
        isOpening = false;
    }

    // Remaining methods (preloadCommonTextures, getItemTexture, etc.) remain the same
    // Only including critical methods with UI improvements

    private void preloadCommonTextures() {
        List<Items> commonItems = ItemLoader.getAllItems();
        Map<String, Texture> textureMap = gameController.getAssetManager().loadAllItems(commonItems);
        this.itemTextures.putAll(textureMap);
    }

    private Texture getItemTexture(String name, String texturePath) {
        if (!itemTextures.containsKey(name)) {
            itemTextures.put(name, new Texture(Gdx.files.internal(texturePath)));
        }
        return itemTextures.get(name);
    }

    private void updateItemList() {
        itemList.clear();
        Character character = gameController.getCharacter();
        if (character.getItems() != null) {
            for (Map.Entry<String, Integer> entry : character.getItems().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    itemList.add(entry.getKey());
                }
            }
        }
    }

    public boolean handleClick(int screenX, int screenY) {
        if (!visible || isClosing) return false;

        float uiY = Gdx.graphics.getHeight() - screenY;

        if (!inventoryBounds.contains(screenX, uiY)) {
            return false;
        }

        if (closeButton.contains(screenX, uiY)) {
            hide();
            return true;
        }

        int itemCount = Math.min(MAX_SLOTS, itemList.size());
        for (int i = 0; i < itemCount; i++) {
            if (itemSlots[i].contains(screenX, uiY)) {
                selectedItemIndex = i;
                lastPulseTime = TimeUtils.millis();
                return true;
            }
        }

        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null) {
                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest") && !item.getItemEffect().equals("debuff")) {
                    if (useButton.contains(screenX, uiY)) {
                        useSelectedItem();
                        return true;
                    }
                }

                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest")) {
                    if (discardButton.contains(screenX, uiY)) {
                        discardSelectedItem();
                        return true;
                    }
                }

                if (item.getItemEffect().equals("craft")) {
                    if (craftButton.contains(screenX, uiY)) {
                        craftSelectedItem();
                        return true;
                    }
                }
            }
        }

        return true;
    }

    public boolean handleMouseMove(int screenX, int screenY) {
        if (!visible) return false;

        float uiY = Gdx.graphics.getHeight() - screenY;

        hoverUseButton = false;
        hoverDiscardButton = false;
        hoverCloseButton = false;
        hoverCraftButton = false;
        hoveredSlotIndex = -1;

        if (!inventoryBounds.contains(screenX, uiY)) {
            return false;
        }

        hoverCloseButton = closeButton.contains(screenX, uiY);

        // Check for hovering over item slots
        for (int i = 0; i < Math.min(MAX_SLOTS, itemList.size()); i++) {
            if (itemSlots[i].contains(screenX, uiY)) {
                hoveredSlotIndex = i;
                break;
            }
        }

        if (selectedItemIndex >= 0 && selectedItemIndex < itemList.size()) {
            String itemName = itemList.get(selectedItemIndex);
            Items item = ItemLoader.getItemByName(itemName);

            if (item != null) {
                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest") && !item.getItemEffect().equals("debuff")) {
                    hoverUseButton = useButton.contains(screenX, uiY);
                }

                if (!item.getItemEffect().equals("N/A") && !item.getItemEffect().equals("quest")) {
                    hoverDiscardButton = discardButton.contains(screenX, uiY);
                }

                if (item.getItemEffect().equals("craft")) {
                    hoverCraftButton = craftButton.contains(screenX, uiY);
                }
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
                if (!gameController.getCharacter().hasItem(itemName)) {
                    selectedItemIndex = -1;
                }
                inventoryDirty = true;
                errorMessage = "";
            } catch (IllegalStateException | IllegalArgumentException e) {
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
            inventoryDirty = true;
            errorMessage = "";
        }
    }

    private void craftSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= itemList.size()) return;

        String itemName = itemList.get(selectedItemIndex);
        Items item = ItemLoader.getItemByName(itemName);

        if (item != null && item.getItemEffect().equals("craft")) {
            CraftingRecipe recipe = CRAFTING_RECIPES.get(itemName);
            if (recipe != null) {
                Character character = gameController.getCharacter();

                if (character.getItemCount(recipe.ingredient) >= recipe.ingredientCount) {
                    try {
                        Items ingredientItem = ItemLoader.getItemByName(recipe.ingredient);

                        if (ingredientItem != null) {
                            Items craftedItem = ItemLoader.getItemByName(recipe.result);
                            if (craftedItem != null) {
                                character.addItem(craftedItem, 1);
                                character.descreaseItemAmount(ingredientItem.getItemName(), recipe.ingredientCount);

                                errorMessage = "Crafted " + recipe.result + "!";
                            }
                        }
                        inventoryDirty = true;
                    } catch (Exception e) {
                        errorMessage = "Error: " + e.getMessage();
                    }
                } else {
                    errorMessage = "Cần " + recipe.ingredientCount + " " + recipe.ingredient + " để chế tạo!";
                }
            }
        }
    }

    public boolean isVisible() {
        return visible || isOpening || isClosing;
    }

    public void notifyItemsChanged() {
        inventoryDirty = true;
    }

    public void onWindowResize() {
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        calculateLayout();
    }

    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (panelTexture != null) panelTexture.dispose();
        if (itemSlotTexture != null) itemSlotTexture.dispose();
        if (itemSlotSelected != null) itemSlotSelected.dispose();
        if (itemSlotHover != null) itemSlotHover.dispose();
        if (buttonTexture != null) buttonTexture.dispose();
        if (buttonHoverTexture != null) buttonHoverTexture.dispose();
        if (iconTexture != null) iconTexture.dispose();

        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (blurShader != null) blurShader.dispose();
    }
}