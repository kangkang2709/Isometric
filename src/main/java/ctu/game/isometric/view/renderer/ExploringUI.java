package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
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

    public ExploringUI(GameController gameController) {
        this.gameController = gameController;
        this.character = gameController.getCharacter();
        this.stage = new Stage(new ScreenViewport());

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

        // Add default font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Create label styles
        Label.LabelStyle defaultStyle = new Label.LabelStyle();
        defaultStyle.font = font;
        defaultStyle.fontColor = Color.WHITE;
        skin.add("default", defaultStyle);

        Label.LabelStyle timeStyle = new Label.LabelStyle(defaultStyle);
        timeStyle.fontColor = Color.YELLOW;
        skin.add("time", timeStyle);

        Label.LabelStyle questStyle = new Label.LabelStyle(defaultStyle);
        questStyle.fontColor = Color.WHITE;
        skin.add("quest", questStyle);

        // Create progress bar style
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
        playerNameLabel = new Label(character.getName(), skin);
        healthBarImage = new Image(new TextureRegionDrawable(healthBarTexture));

        // Health bar setup - fully match the health_bar.png dimensions
        healthBar = new ProgressBar(0, 100, 1, false, skin, "default-horizontal");
        healthBar.setValue(character.getHealth());

        // Create health stack with bar UNDER the image
        Stack healthStack = new Stack();

        // First add the health bar (will be underneath)
        Table healthBarTable = new Table();
        healthBarTable.add(healthBar).width(healthBarWidth - 6).height(healthBarHeight - 8).padLeft(3).padTop(0);
        healthStack.add(healthBarTable);

        // Then add the image on top (with transparent areas to see the bar)
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
        if (character != null) {
            // Update time
            LocalTime now = LocalTime.now();
            timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));

            // Update player info
            playerNameLabel.setText(character.getName());

            // Update health
            int health = character.getHealth();
            int maxHealth = 100; // Could get this from the character
            healthBar.setValue(health);

            // Update health bar color based on health percentage
            float healthPercent = health / (float)maxHealth;
            Color healthColor = new Color(
                    1f - healthPercent,  // More red as health decreases
                    healthPercent,       // More green as health increases
                    0f,                  // No blue
                    1f                   // Fully opaque
            );

            ((TextureRegionDrawable)((ProgressBar.ProgressBarStyle)healthBar.getStyle()).knobBefore).getRegion().getTexture().dispose();

            Pixmap healthPixmap = new Pixmap(5, 8, Pixmap.Format.RGBA8888);
            healthPixmap.setColor(healthColor);
            healthPixmap.fill();

            ((ProgressBar.ProgressBarStyle)healthBar.getStyle()).knobBefore =
                    new TextureRegionDrawable(new Texture(healthPixmap));

            healthPixmap.dispose();
        }
    }

    public void render() {
        update();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
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