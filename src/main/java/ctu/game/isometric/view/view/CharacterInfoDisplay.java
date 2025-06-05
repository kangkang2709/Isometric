package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;

import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class CharacterInfoDisplay {
    private Character character;
    private BitmapFont font;
    private GlyphLayout layout;
    private Texture backgroundTexture;
    private Texture maleAvatar;
    private Texture femaleAvatar;
    private Texture currentAvatar;
    private Rectangle leftPanel;
    private Rectangle rightPanel;
    private boolean initialized = false;

    public CharacterInfoDisplay(Character character) {
        this.character = character;
        initialize();
    }

    private void initialize() {
        if (!initialized) {
            this.font = generateVietNameseFont("ModernAntiqua-Regular.ttf", 20);
            layout = new GlyphLayout();

            // Load avatars
            maleAvatar = new Texture(Gdx.files.internal("characters/male_avatar.png"));
            femaleAvatar = new Texture(Gdx.files.internal("characters/female_avatar.png"));

            // Set current avatar based on character gender
            currentAvatar = character.getGender().name().equals("MALE") ? maleAvatar : femaleAvatar;
            backgroundTexture = new Texture(Gdx.files.internal("ui/player_info.png"));
            // Define panel regions
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            leftPanel = new Rectangle(50, 100, screenWidth / 2 - 100, screenHeight - 200);
            rightPanel = new Rectangle(screenWidth / 2 + 50, 100, screenWidth / 2 - 100, screenHeight - 200);

            initialized = true;
        }
    }

    public void render(SpriteBatch batch) {
        // Save original projection matrix and batch state
        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }



        // Start batch for drawing text and images
        batch.begin();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        // Draw background first
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Draw title
        float titleY = Gdx.graphics.getHeight() - 50;
        font.setColor(Color.WHITE);
        layout.setText(font, "Character Information");
        font.draw(batch, "Character Information", 110, titleY +34);

        // Left panel content
        float leftX = leftPanel.x + 120;
        float leftY = leftPanel.y + leftPanel.height - 30;
        float lineHeight = 40;

        // Draw character details on left panel
        font.setColor(Color.YELLOW);
        font.draw(batch, character.getName(), leftX + 90, leftY+55);
        leftY = leftY - lineHeight ;
        font.draw(batch, "Level: " + character.getLevel(), leftX, leftY+5);
        leftY -= lineHeight;
        font.draw(batch, "Exp: " +  character.getExp() + "/" + (character.getLevel() * 50), leftX, leftY);
        leftY = leftY - lineHeight - 40 ;

        font.setColor(Color.RED);
        font.draw(batch, "Health: " + character.getHealth() + "/" + character.getMaxHealth(), leftX, leftY);
        leftY -= lineHeight;

        font.setColor(Color.BLUE);
        font.draw(batch, "Mana: " + character.getMana() + "/" + character.getMaxMana(), leftX, leftY);
        leftY = leftY - lineHeight - 45 ;

        font.setColor(Color.ORANGE);
        font.draw(batch, "Damage: " + character.getDamage(), leftX, leftY);
        leftY = leftY - lineHeight ;
        font.draw(batch,  "Defense: " + character.getDefend(), leftX, leftY);
        leftY = leftY - lineHeight - 45 ;

        // Status effects
        font.setColor(Color.WHITE);
        font.draw(batch, "Status Effects:", leftX, leftY+5);
        leftY -= lineHeight / 1.5f;

        Map<String, java.util.List<String>> status = character.getStatus();
        if (status != null) {
            // Display buffs
            if (status.containsKey("buffs") && !status.get("buffs").isEmpty()) {
                font.setColor(Color.GREEN);
                for (String buff : status.get("buffs")) {
                    font.draw(batch, "- " + buff, leftX + 10, leftY);
                    leftY -= lineHeight / 1.5f;
                }
            } else {
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, "- No active buffs", leftX + 10, leftY);
                leftY -= lineHeight / 1.5f;
            }

            // Display debuffs
            if (status.containsKey("debuffs") && !status.get("debuffs").isEmpty()) {
                font.setColor(Color.SALMON);
                for (String debuff : status.get("debuffs")) {
                    font.draw(batch, "- " + debuff, leftX + 10, leftY);
                    leftY -= lineHeight / 1.5f;
                }
            } else {
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, "- No active debuffs", leftX + 10, leftY);
            }
        }

        // Right panel content
        float rightX = rightPanel.x + 160;
        float rightY = rightPanel.y + rightPanel.height - 30;

        // Draw character avatar
        if (currentAvatar != null) {
            float avatarX = rightPanel.x + (rightPanel.width / 2) - 75;
            float avatarY = rightY - 180;
            batch.draw(currentAvatar, avatarX, avatarY, 150, 200);
        }

        // Draw attempt flags and score
        rightY -= 250; // Position below avatar
        font.setColor(Color.WHITE);
        font.draw(batch, "Character Statistics:", rightX, rightY);
        rightY -= lineHeight;

        Map<String, Integer> attemptFlags = character.getEttempFlags();
        if (attemptFlags != null) {
            font.setColor(Color.LIGHT_GRAY);

            if (attemptFlags.containsKey("quizAttempts")) {
                font.draw(batch, "Quiz Attempts Today: " + attemptFlags.get("quizAttempts")+ "/3", rightX, rightY);
                rightY -= lineHeight;
            }

            if (attemptFlags.containsKey("mulQuizAttempts")) {
                font.draw(batch, "Multiple Choice Attempts Today: " + attemptFlags.get("mulQuizAttempts") + "/3", rightX, rightY);
                rightY -= lineHeight;
            }

            if (attemptFlags.containsKey("fallen")) {
                font.draw(batch, "Times Fallen: " + attemptFlags.get("fallen"), rightX, rightY);
                rightY -= lineHeight;
            }

            if (attemptFlags.containsKey("wrongWord")) {
                font.draw(batch, "Wrong Words: " + attemptFlags.get("wrongWord"), rightX, rightY);
                rightY -= lineHeight;
            }
        }

        // Draw score
        font.setColor(Color.GOLD);
        font.draw(batch, "Total Score: " + character.getScore(), rightX, rightY);

        // Restore batch state
        if (!batchWasDrawing) {
            batch.end();
        }
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (maleAvatar != null) maleAvatar.dispose();
        if (femaleAvatar != null) femaleAvatar.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}