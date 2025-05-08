package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    private int maxHealth = 100;
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
    private Texture questBoxTexture;

    // UI visibility control
    private boolean uiVisible = true;

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
        questBoxTexture = new Texture(Gdx.files.internal("ui/quest_box.png"));
    }

    private void createSkin() {
        skin = new Skin();

        // Load custom font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Tektur-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = 16;
        params.color = Color.WHITE;
        params.borderWidth = 1;
        params.borderColor = Color.BLACK;
        BitmapFont customFont = generator.generateFont(params);

        // Create another font for different purposes if needed
        FreeTypeFontGenerator.FreeTypeFontParameter titleParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParams.size = 20;
        titleParams.color = Color.WHITE;
        titleParams.borderWidth = 1.5f;
        titleParams.borderColor = Color.BLACK;
        BitmapFont titleFont = generator.generateFont(titleParams);

        generator.dispose();

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

        // Add the tables to the root table
        rootTable.top().pad(10);
        rootTable.add(topLeftTable).left().expandX();
        rootTable.add(topRightTable).right();

        stage.addActor(rootTable);
    }

    private void setupTopLeft() {
        topLeftTable = new Table();

        // Time frame with time label
        timeFrameImage = new Image(new TextureRegionDrawable(timeFrameTexture));
        timeLabel = new Label("12:00", skin, "time");

        float healthBarWidth = healthBarTexture.getWidth();
        float healthBarHeight = healthBarTexture.getHeight();

        // Stack time label on top of the frame
        Stack timeStack = new Stack();
        timeStack.add(timeFrameImage);
        Table timeLabelTable = new Table();
        timeLabelTable.add(timeLabel).center().padBottom(5);
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
        healthIndicatorTable.add(healthIndicator).width((healthBarWidth - 12) * (character.getHealth() / 100f)).height(11).padLeft(0).padTop(0).left();
        healthStack.add(healthIndicatorTable);

        // Add the bar image on top (with transparent areas)
        healthStack.add(healthBarImage);

        // Add to top left table
        topLeftTable.add(timeStack).padRight(10);
        Table playerInfoTable = new Table();
        playerInfoTable.add(playerNameLabel).left().row();
        playerInfoTable.add(healthStack).left().padTop(5);
        topLeftTable.add(playerInfoTable).left().top();
    }

    private void setupTopRight() {
        topRightTable = new Table();

        // Quest box with quest text
        questBoxImage = new Image(new TextureRegionDrawable(questBoxTexture));
        questLabel = new Label("Current Quest", skin, "quest");

        // Stack quest label on quest box
        Stack questStack = new Stack();
        questStack.add(questBoxImage);
        Table questLabelTable = new Table();
        questLabelTable.add(questLabel).center();
        questStack.add(questLabelTable);

        topRightTable.add(questStack).right().top();
    }

    public void update() {
        if (character != null && uiVisible) {
            // Update time
            LocalTime now = LocalTime.now();
            timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));

            // Update player info
            playerNameLabel.setText(character.getName());

            // Update health
            float health = character.getHealth();

            // Update health bar color based on health percentage
            float healthPercent = health / (float)maxHealth;
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
                ((TextureRegionDrawable)healthIndicator.getDrawable()).getRegion().getTexture().dispose();
            }

            // This is just changing the texture but not the layout
            healthIndicator.setDrawable(new TextureRegionDrawable(new Texture(healthPixmap)));

            // We should rebuild the entire health section instead of just setting width
            reinitializeHealthBar(health, healthColor);

            healthPixmap.dispose();
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

    private void reinitializeHealthBar(float health, Color healthColor) {
        Stack healthStack = findHealthStack();
        if (healthStack != null) {
            healthStack.clear();

            Table healthIndicatorTable = new Table();
            healthIndicatorTable.left().top(); // Align the table itself
            healthIndicatorTable.add(healthIndicator)
                    .width((healthBarTexture.getWidth() - 12) * (health / 100f))
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
     * @param visible true to show UI, false to hide
     */
    public void setUIVisible(boolean visible) {
        uiVisible = visible;
    }

    /**
     * Returns current UI visibility
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
        questBoxTexture.dispose();
    }
}