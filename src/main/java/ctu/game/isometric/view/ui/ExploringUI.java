package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class ExploringUI {
    private Stage stage;
    private Skin skin;
    private GameController gameController;
    private Character character;

    // UI Elements
    private Label timeLabel;
    private Label playerNameLabel;
    private Label questLabel;
    private ProgressBar healthBar;
    private Label healthLabel;
    private Image healthIndicator;
    private float maxHealth = 20;

    // Mana bar elements
    private Image manaBarImage;
    private Image manaIndicator;
    private float maxMana = 20;

    // Experience bar elements
    private Image expBarImage;
    private Image expIndicator;
    private int maxExp = 100;

    // Images
    private Image timeFrameImage;
    private Image healthBarImage;
    private Image questBoxImage;

    // Tables for organization
    private Table rootTable;
    private Table topLeftTable;
    private Table topRightTable;

    // Textures
    private Texture timeFrameTexture;
    private Texture healthBarTexture;
    private Texture manaBarTexture;
    private Texture questBoxTexture;

    // UI visibility control
    private boolean uiVisible = true;

    private Label runLabel;
    private Table topCenterTable;

    public ExploringUI(GameController gameController) {
        this.gameController = gameController;
        this.character = gameController.getCharacter();
        this.stage = new Stage(new ScreenViewport());
        gameController.setExploringUI(this);
        loadTextures();
        createSkin();
        setupUI();
    }

    private void loadTextures() {
        timeFrameTexture = new Texture(Gdx.files.internal("ui/time_frame.png"));
        healthBarTexture = new Texture(Gdx.files.internal("ui/health_bar.png"));
        manaBarTexture = new Texture(Gdx.files.internal("ui/health_bar.png")); // Reuse health bar texture or use dedicated texture
        questBoxTexture = new Texture(Gdx.files.internal("ui/panel-dialogue-4.png"));
    }


    private void createSkin() {
        skin = new Skin();
        // Load custom font

        BitmapFont customFont = generateVietNameseFont("Roboto-Italic.ttf", 16);
        BitmapFont titleFont = generateVietNameseFont("Roboto-Black.ttf", 16);


        skin.add("default-font", customFont);
        skin.add("title-font", titleFont);

        // Create label styles
        Label.LabelStyle defaultStyle = new Label.LabelStyle();
        defaultStyle.font = customFont;
        defaultStyle.fontColor = Color.WHITE;
        skin.add("default", defaultStyle);

        Label.LabelStyle timeStyle = new Label.LabelStyle(defaultStyle);
        timeStyle.fontColor = Color.YELLOW;
        skin.add("time", timeStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;
        skin.add("title", titleStyle);

        Label.LabelStyle questStyle = new Label.LabelStyle(defaultStyle);
        questStyle.fontColor = Color.WHITE;
        skin.add("quest", questStyle);

        // Create progress bar style
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();

        // Create colored pixmaps for health bar
        Pixmap backgroundPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        backgroundPixmap.setColor(new Color(0, 0, 0, 0)); // Transparent background
        backgroundPixmap.fill();

        Pixmap knobPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(new Color(0, 0, 0, 0)); // Transparent knob
        knobPixmap.fill();

        Pixmap knobBeforePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        knobBeforePixmap.setColor(new Color(0.2f, 0.8f, 0.2f, 1f)); // Slightly adjusted green for better visibility
        knobBeforePixmap.fill();

        // Create drawables from pixmaps
        progressBarStyle.background = new TextureRegionDrawable(new Texture(backgroundPixmap));
        progressBarStyle.knob = new TextureRegionDrawable(new Texture(knobPixmap));
        progressBarStyle.knobBefore = new TextureRegionDrawable(new Texture(knobBeforePixmap));

        // Dispose pixmaps after creating textures
        backgroundPixmap.dispose();
        knobPixmap.dispose();
        knobBeforePixmap.dispose();

        progressBarStyle.background.setLeftWidth(0);
        progressBarStyle.background.setRightWidth(0);
        progressBarStyle.background.setTopHeight(0);
        progressBarStyle.background.setBottomHeight(0);

        skin.add("default-horizontal", progressBarStyle);
    }

    private void setupUI() {
        // Create main table that fills the screen
        rootTable = new Table();
        rootTable.setFillParent(true);

        // Top left UI elements
        setupTopLeft();

        // Top right UI elements
        setupTopRight();

        // Create top center table for run display
        setupTopCenter();

        // Add the tables to the root table
        rootTable.top().pad(10);
        rootTable.add(topLeftTable).left().expandX();
        rootTable.add(topCenterTable).center().expand(0, 0).padRight(400); // Centered with padding
        rootTable.add(topRightTable).right();

        stage.addActor(rootTable);
    }

    private void setupTopCenter() {
//        topCenterTable = new Table();
//        runLabel = new Label("Floor: " + character.getRun(), skin, "title");
//        topCenterTable.add(runLabel).center().top().padTop(5);
    }

    private void setupTopLeft() {
        topLeftTable = new Table();
        // Time frame with time label
        timeFrameImage = new Image(new TextureRegionDrawable(timeFrameTexture));
        timeLabel = new Label("Lv " + character.getLevel(), skin, "default");
        healthLabel = new Label("HP " + (int) character.getHealth() + "/" + (int) maxHealth, skin, "default");

        float healthBarWidth = healthBarTexture.getWidth();
        float healthBarHeight = healthBarTexture.getHeight();

        // Stack time label on top of the frame
        Stack timeStack = new Stack();
        timeStack.add(timeFrameImage);
        Table timeLabelTable = new Table();
        timeLabelTable.add(timeLabel).center();
        timeStack.add(timeLabelTable);

        // Player name and health
        playerNameLabel = new Label(character.getName(), skin, "title");
        healthBarImage = new Image(new TextureRegionDrawable(healthBarTexture));

        // Create initial Pixmap for health display
        Pixmap healthPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
        Color healthColor = Color.GREEN;
        healthPixmap.setColor(healthColor);
        healthPixmap.fill();
        Texture healthTexture = new Texture(healthPixmap);
        healthIndicator = new Image(new TextureRegionDrawable(healthTexture));
        healthPixmap.dispose();

        // Create health stack with components
        Stack healthStack = new Stack();

        // Add the colored health indicator (bottom layer)
        Table healthIndicatorTable = new Table();
        healthIndicatorTable.add(healthIndicator).width((healthBarWidth - 12) * (character.getHealth() / maxHealth)).height(11).padLeft(0).padTop(0).left();
        healthStack.add(healthIndicatorTable);

        // Add the bar image on top (with transparent areas)
        healthStack.add(healthBarImage);

        // Create mana bar components
        manaBarImage = new Image(new TextureRegionDrawable(manaBarTexture));

        // Create initial Pixmap for mana display
        Pixmap manaPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
        Color manaColor = Color.BLUE;
        manaPixmap.setColor(manaColor);
        manaPixmap.fill();
        Texture manaTexture = new Texture(manaPixmap);
        manaIndicator = new Image(new TextureRegionDrawable(manaTexture));
        manaPixmap.dispose();

        // Create mana stack with components
        Stack manaStack = new Stack();

        // Add the colored mana indicator (bottom layer)
        Table manaIndicatorTable = new Table();
        manaIndicatorTable.add(manaIndicator).width((healthBarWidth - 12) * (character.getMana() / maxMana)).height(11).padLeft(0).padTop(0).left();
        manaStack.add(manaIndicatorTable);

        // Add the bar image on top
        manaStack.add(manaBarImage);

        // Create experience bar components
//        expLabel = new Label("EXP: 0/100", skin, "default");

        // Create initial Pixmap for exp display
        Pixmap expPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
        Color expColor = Color.YELLOW;
        expPixmap.setColor(expColor);
        expPixmap.fill();
        Texture expTexture = new Texture(expPixmap);
        expIndicator = new Image(new TextureRegionDrawable(expTexture));
        expPixmap.dispose();
//
//        // Create exp stack with components
//        Stack expStack = new Stack();
//
//        // Add the colored exp indicator (bottom layer)
//        Table expIndicatorTable = new Table();
//        expIndicatorTable.add(expIndicator).width((healthBarWidth - 12) * (character.getExp() / (float)maxExp)).height(11).padLeft(0).padTop(0).left();
//        expStack.add(expIndicatorTable);

        // Add the bar image on top

        // Add to top left table
        topLeftTable.add(timeStack).padRight(10);
        Table playerInfoTable = new Table();
        playerInfoTable.add(playerNameLabel).left().row();
        playerInfoTable.add(healthStack).left().padTop(5).row();
        playerInfoTable.add(manaStack).left().padTop(3).row();
//        playerInfoTable.add(expStack).left().padTop(3).row();
//        playerInfoTable.add(expLabel).left().padTop(2);
        topLeftTable.add(playerInfoTable).left().top();
    }

    private void setupTopRight() {
        topRightTable = new Table();

        // Quest box with quest text
        questBoxImage = new Image(new TextureRegionDrawable(questBoxTexture));
        questLabel = new Label(character.getCurrentObject(), skin, "quest");

        // Stack quest label on quest box
        Stack questStack = new Stack();
        questStack.add(questBoxImage);
        Table questLabelTable = new Table();
        questLabelTable.add(questLabel).center().padLeft(-80);
        questStack.add(questLabelTable);

        topRightTable.add(questStack).right().top();
    }

    public void update() {
        if (character != null && uiVisible) {
            // Update time
            int charLevel = character.getLevel();
            timeLabel.setText(charLevel);


            playerNameLabel.setText(character.getName().replaceAll("_", " "));

//            else {
//                runLabel.setText((character.getGameMap().getMapName().toUpperCase().equalsIgnoreCase("main") ? "Village Forest" : character.getGameMap().getMapName().toUpperCase()));
//                playerNameLabel.setText(character.getName().replaceAll("_", " ") + " (Lv " + charLevel + ")");
//            }
            // Update health and max health
            float health = character.getHealth();
            maxHealth = character.getMaxHealth(); // Get dynamic max health
            healthLabel.setText("HP: " + (int) health + "/" + (int) maxHealth);
            // Update mana and max mana
            float mana = character.getMana();
            maxMana = character.getMaxMana(); // Get dynamic max mana

            // Update exp
            float exp = character.getExp();
            maxExp = charLevel * 50; // Assuming level * 50 for max exp needed
//            expLabel.setText("EXP: " + (int)exp + "/" + maxExp);

            // Update health bar color based on health percentage
            float healthPercent = health / (float) maxHealth;
            Color healthColor = new Color(
                    1f - healthPercent,  // More red as health decreases
                    healthPercent,       // More green as health increases
                    0f,                  // No blue
                    1f                   // Fully opaque
            );

            // Update health indicator with Pixmap
            Pixmap healthPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
            healthPixmap.setColor(healthColor);
            healthPixmap.fill();

            // Dispose old texture before setting new one
            if (healthIndicator.getDrawable() != null) {
                ((TextureRegionDrawable) healthIndicator.getDrawable()).getRegion().getTexture().dispose();
            }

            // Set new health texture
            healthIndicator.setDrawable(new TextureRegionDrawable(new Texture(healthPixmap)));

            // Update mana indicator
            float manaPercent = mana / (float) maxMana;
            Color manaColor = new Color(0.2f, 0.2f, 1f, 1f); // Blue color for mana

            Pixmap manaPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
            manaPixmap.setColor(manaColor);
            manaPixmap.fill();

            // Dispose old texture before setting new one
            if (manaIndicator.getDrawable() != null) {
                ((TextureRegionDrawable) manaIndicator.getDrawable()).getRegion().getTexture().dispose();
            }

            // Set new mana texture
            manaIndicator.setDrawable(new TextureRegionDrawable(new Texture(manaPixmap)));

            // Update exp indicator
            float expPercent = exp / (float) maxExp;
            Color expColor = new Color(1f, 0.8f, 0.2f, 1f); // Gold color for exp

            Pixmap expPixmap = new Pixmap(1, 10, Pixmap.Format.RGBA8888);
            expPixmap.setColor(expColor);
            expPixmap.fill();

            // Dispose old texture before setting new one
            if (expIndicator.getDrawable() != null) {
                ((TextureRegionDrawable) expIndicator.getDrawable()).getRegion().getTexture().dispose();
            }

            // Set new exp texture
            expIndicator.setDrawable(new TextureRegionDrawable(new Texture(expPixmap)));

            // We should rebuild the entire health section instead of just setting width
            reinitializeHealthBar(health, healthColor);
            reinitializeManaBar(mana, manaColor);
            reinitializeExpBar(exp, expColor);

            healthPixmap.dispose();
            manaPixmap.dispose();
            expPixmap.dispose();
        }
    }

    public void updateQuest() {
        if (questLabel != null) {
            questLabel.setText(character.getCurrentObject());
        }
    }

    private Stack findHealthStack() {
        // Navigate through the UI hierarchy to find the health stack
        if (topLeftTable != null) {
            Cell<?> cell = topLeftTable.getCells().get(1); // Assuming the second cell in the table
            if (cell != null && cell.getActor() instanceof Table) {
                Table playerInfoTable = (Table) cell.getActor();
                Cell<?> healthCell = playerInfoTable.getCells().get(1); // Assuming the second cell in the player info table
                if (healthCell != null && healthCell.getActor() instanceof Stack) {
                    return (Stack) healthCell.getActor();
                }
            }
        }
        return null;
    }

    private Stack findManaStack() {
        // Navigate through the UI hierarchy to find the mana stack
        if (topLeftTable != null) {
            Cell<?> cell = topLeftTable.getCells().get(1); // Assuming the second cell in the table
            if (cell != null && cell.getActor() instanceof Table) {
                Table playerInfoTable = (Table) cell.getActor();
                Cell<?> manaCell = playerInfoTable.getCells().get(2); // Assuming the third cell in the player info table
                if (manaCell != null && manaCell.getActor() instanceof Stack) {
                    return (Stack) manaCell.getActor();
                }
            }
        }
        return null;
    }


    private void reinitializeHealthBar(float health, Color healthColor) {
        Stack healthStack = findHealthStack();
        if (healthStack != null) {
            healthStack.clear();

            Table healthIndicatorTable = new Table();
            healthIndicatorTable.left().top(); // Align the table itself
            healthIndicatorTable.add(healthIndicator)
                    .width((healthBarTexture.getWidth() - 12) * (health / maxHealth))
                    .height(11)
                    .padLeft(6) // Add correct padding to match initial setup
                    .padTop(3)  // Add correct padding to match initial setup
                    .left();

            healthStack.add(healthIndicatorTable);
            healthStack.add(healthBarImage);

            // Force layout update
            healthStack.invalidate();
            healthStack.validate();
        }
    }

    private void reinitializeManaBar(float mana, Color manaColor) {
        Stack manaStack = findManaStack();
        if (manaStack != null) {
            manaStack.clear();

            Table manaIndicatorTable = new Table();
            manaIndicatorTable.left().top(); // Align the table itself
            manaIndicatorTable.add(manaIndicator)
                    .width((manaBarTexture.getWidth() - 12) * (mana / maxMana))
                    .height(11)
                    .padLeft(6) // Add correct padding to match initial setup
                    .padTop(3)  // Add correct padding to match initial setup
                    .left();

            manaStack.add(manaIndicatorTable);
            manaStack.add(manaBarImage);

            // Force layout update
            manaStack.invalidate();
            manaStack.validate();
        }
    }

    private Pixmap cachedBackgroundPixmap;
    private Texture cachedBackgroundTexture;

    private void initializeExpBarBackground() {
        int size = timeFrameTexture.getWidth();
        cachedBackgroundPixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Draw the timeFrameTexture as background
        Pixmap timeFramePixmap = new Pixmap(Gdx.files.internal("ui/time_frame.png"));
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (x < timeFramePixmap.getWidth() && y < timeFramePixmap.getHeight()) {
                    cachedBackgroundPixmap.drawPixel(x, y, timeFramePixmap.getPixel(x, y));
                }
            }
        }
        timeFramePixmap.dispose();

        // Add semi-transparent overlay for better contrast
        cachedBackgroundPixmap.setColor(0, 0, 0, 0.3f);
        cachedBackgroundPixmap.fillCircle(size / 2, size / 2, size / 2 - size / 20);

        cachedBackgroundTexture = new Texture(cachedBackgroundPixmap);
    }

    private void reinitializeExpBar(float exp, Color expColor) {
        if (cachedBackgroundPixmap == null || cachedBackgroundTexture == null) {
            initializeExpBarBackground();
        }

        int size = cachedBackgroundPixmap.getWidth();
        Pixmap progressPixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        progressPixmap.drawPixmap(cachedBackgroundPixmap, 0, 0);

        // Draw progress arc with gradient
        float expPercent = exp / (float) maxExp;
        int centerX = size / 2;
        int centerY = size / 2;
        int radius = (int) ((size / 2 - size / 10) * 1.1f); // 80% of the radius
        int thickness = size / 2;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int dx = x - centerX;
                int dy = y - centerY;
                double distanceSquared = dx * dx + dy * dy;

                if (distanceSquared <= radius * radius && distanceSquared >= (radius - thickness) * (radius - thickness)) {
                    double angle = Math.atan2(dy, dx) + Math.PI;
                    double normalizedAngle = angle / (2 * Math.PI);

                    if (normalizedAngle <= expPercent) {
                        float gradientFactor = (float) (distanceSquared - (radius - thickness) * (radius - thickness)) / (thickness * thickness);
                        Color gradientColor = new Color(
                                expColor.r * gradientFactor,
                                expColor.g * gradientFactor,
                                expColor.b * gradientFactor,
                                expColor.a
                        );
                        progressPixmap.setColor(gradientColor);
                        progressPixmap.drawPixel(x + 1, y);
                    }
                }
            }
        }

        // Draw border
        progressPixmap.setColor(Color.BLACK);
        progressPixmap.drawCircle(centerX, centerY, radius);
        progressPixmap.drawCircle(centerX, centerY, radius - thickness);

        // Dispose old texture and set new one
        if (timeFrameImage.getDrawable() != null) {
            ((TextureRegionDrawable) timeFrameImage.getDrawable()).getRegion().getTexture().dispose();
        }
        timeFrameImage.setDrawable(new TextureRegionDrawable(new Texture(progressPixmap)));


        progressPixmap.dispose();
    }

    public void render() {
        if (uiVisible) {
            update();
            stage.act(Gdx.graphics.getDeltaTime());
            stage.draw();
        }
    }

    /**
     * Toggles UI visibility
     */
    public void toggleUI() {
        uiVisible = !uiVisible;
    }

    /**
     * Explicitly sets UI visibility
     *
     * @param visible true to show UI, false to hide
     */
    public void setUIVisible(boolean visible) {
        uiVisible = visible;
    }

    /**
     * Returns current UI visibility
     *
     * @return true if UI is visible, false otherwise
     */
    public boolean isUIVisible() {
        return uiVisible;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public Stage getStage() {
        return stage;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
        timeFrameTexture.dispose();
        healthBarTexture.dispose();
        manaBarTexture.dispose();
        questBoxTexture.dispose();
    }
}