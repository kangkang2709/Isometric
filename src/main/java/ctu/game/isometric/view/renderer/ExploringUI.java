package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;

public class ExploringUI {
    private Stage stage;
    private Skin skin;
    private GameController gameController;

    // UI Elements
    private Label locationLabel;
    private ProgressBar healthBar;
    private Label healthLabel;

    // Core tables for organization
    private Table mainTable;
    private Table statusTable;
    private Character character;
    public ExploringUI(GameController gameController) {
        this.gameController = gameController;
        this.stage = new Stage(new ScreenViewport());
        this.character= gameController.getCharacter();
        createSkin();
        setupUI();
    }

    private void createSkin() {
        skin = new Skin();

        // Add a default font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Create simple label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Create progress bar style with basic colors
        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();

        // Create background texture
        Pixmap bgPixmap = new Pixmap(100, 20, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(Color.DARK_GRAY);
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);

        // Create foreground texture
        Pixmap knobPixmap = new Pixmap(1, 20, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.RED);
        knobPixmap.fill();
        Texture knobTexture = new Texture(knobPixmap);

        progressBarStyle.background = new TextureRegionDrawable(bgTexture);
        progressBarStyle.knob = new TextureRegionDrawable(knobTexture);
        progressBarStyle.knobBefore = progressBarStyle.knob;

        skin.add("default-horizontal", progressBarStyle);

        bgPixmap.dispose();
        knobPixmap.dispose();
    }

    private void setupUI() {
        // Create main table that fills the screen
        mainTable = new Table();
        mainTable.setFillParent(true);

        // Create location display
        locationLabel = new Label("Location: 0,0", skin);

        // Create health display
        statusTable = new Table();
        healthBar = new ProgressBar(0, 100, 1, false, skin, "default-horizontal");
        healthBar.setValue(100);
        healthLabel = new Label("HP: 100/100", skin);

        statusTable.add(healthLabel).padRight(5);
        statusTable.add(healthBar).width(150);

        // Add elements to main table
        mainTable.top().left().pad(10);
        mainTable.add(locationLabel).left().padBottom(10);
        mainTable.row();
        mainTable.add(statusTable).left();

        // Add the main table to the stage
        stage.addActor(mainTable);
    }

    public void update() {
        if (character != null) {
            // Update location text
            int x = (int)character.getGridX();
            int y = (int)character.getGridY();
            locationLabel.setText("Location: " + x + "," + y);

            // Update health display
            int health = character.getHealth();
            int maxHealth = 100; // You might want to get this from the character
            healthBar.setValue(health);
            healthLabel.setText("HP: " + health + "/" + maxHealth);
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

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}