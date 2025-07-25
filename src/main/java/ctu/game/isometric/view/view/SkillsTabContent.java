package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import ctu.game.isometric.model.entity.Character;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;


import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

class SkillsTabContent {
    private Character character;
    private BitmapFont font;
    private BitmapFont titleFont;
    private GlyphLayout layout = new GlyphLayout();
    private Texture[] skillButtonTextures;
    private Texture highlightTexture;
    private Texture panelTexture;
    private Texture selectedBorderTexture;

    private static final Color HEADER_COLOR = new Color(0.12f, 0.65f, 0.89f, 1f);
    private static final Color DESC_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color INACTIVE_COLOR = new Color(0.5f, 0.5f, 0.5f, 0.7f);

    private String[] skillNames = {
            "Basic Attack",
            "Dictionary Word",
            "Manual Input",
            "Healing",
            "Defense"
    };

    private String[] skillDescriptions = {
            "Basic physical damage based on your attack power",
            "Magical damage based on dictionary word point value",
            "Damage based on manually typed word, invalid words damage you",
            "Restores 20% of your max HP, costs 10 MP",
            "Increases 3 defense, recovers 5 MP"
    };

    private int selectedSkill = -1;
    private int hoveredSkill = -1;

    public SkillsTabContent(Character character, BitmapFont font) {
        this.character = character;
        this.font = font;
        this.titleFont = font;

        // Load skill icons and textures
        loadSkillTextures();
        createHighlightTexture();
        createSelectedBorderTexture();
        createPanelTexture();
    }

    private void loadSkillTextures() {
        skillButtonTextures = new Texture[5];
        try {
            skillButtonTextures[0] = new Texture(Gdx.files.internal("dungeon/skill_attack.png"));
            skillButtonTextures[1] = new Texture(Gdx.files.internal("dungeon/skill_flame.png"));
            skillButtonTextures[2] = new Texture(Gdx.files.internal("dungeon/skill_lightning.png"));
            skillButtonTextures[3] = new Texture(Gdx.files.internal("dungeon/skill_heal.png"));
            skillButtonTextures[4] = new Texture(Gdx.files.internal("dungeon/skill_defend.png"));
        } catch (Exception e) {
            // Create fallback textures if files are missing
            Gdx.app.error("SkillsTabContent", "Error loading skill textures: " + e.getMessage());
            for (int i = 0; i < skillButtonTextures.length; i++) {
                if (skillButtonTextures[i] == null) {
                    Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                    pixmap.setColor(Color.WHITE);
                    pixmap.fill();
                    skillButtonTextures[i] = new Texture(pixmap);
                    pixmap.dispose();
                }
            }
        }
    }

    private void createHighlightTexture() {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 0.3f);
        pixmap.fillRectangle(0, 0, 64, 64);
        pixmap.setColor(0.2f, 0.7f, 1.0f, 0.8f);
        pixmap.drawRectangle(0, 0, 63, 63);
        highlightTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createSelectedBorderTexture() {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.9f, 1.0f, 1f);
        pixmap.drawRectangle(0, 0, 63, 63);
        pixmap.drawRectangle(1, 1, 61, 61);
        pixmap.drawRectangle(2, 2, 59, 59);
        selectedBorderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createPanelTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.1f, 0.12f, 0.16f, 0.7f);
        pixmap.fill();
        panelTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void updateCharacter(Character character) {
        this.character = character;
    }


    GlyphLayout gyphLayout = new GlyphLayout();

    public void render(SpriteBatch batch, Rectangle bounds) {
        // Draw section title
        titleFont.setColor(HEADER_COLOR);
        titleFont.draw(batch, "SKILLS", bounds.x + 30, bounds.y + bounds.height - 10);

        float contentStartY = bounds.y + bounds.height - 40;
        float padding = 15;

        // Smaller icon size to fit better
        float iconSize = 55;
        float rowHeight = iconSize + padding;

        // Draw background panel for all skills
        batch.setColor(Color.WHITE);
        batch.draw(panelTexture,
                bounds.x + padding,
                contentStartY - (rowHeight * 5) - padding,
                bounds.width - padding * 2,
                rowHeight * 5 + padding * 2);

        // Draw each skill in its own row
        for (int i = 0; i < skillButtonTextures.length; i++) {
            float rowY = contentStartY - (i + 1) * rowHeight;
            float iconX = bounds.x + padding * 2;
            float textX = iconX + iconSize + padding;

            // Draw skill icon
            batch.setColor(Color.WHITE);
            batch.draw(skillButtonTextures[i], iconX, rowY, iconSize, iconSize);

            // Draw highlight/selection indicators
            if (i == hoveredSkill) {
                batch.setColor(Color.WHITE);
                batch.draw(highlightTexture, iconX - 2, rowY - 2, iconSize + 4, iconSize + 4);
            }

            if (i == selectedSkill) {
                batch.setColor(Color.WHITE);
                batch.draw(selectedBorderTexture, iconX - 2, rowY - 2, iconSize + 4, iconSize + 4);
            }

            // Draw skill name
            font.setColor(HEADER_COLOR);

            gyphLayout.setText(font, skillNames[i]);
            font.draw(batch, skillNames[i], textX, rowY + iconSize - 5);

            // Draw skill description (shorter version)
            font.setColor(DESC_COLOR);
            String description = skillDescriptions[i];
            if (description.contains("\n")) {
                description = description.substring(0, description.indexOf('\n'));
            }
            font.draw(batch, description, textX, rowY + iconSize - 25);

            // Draw skill cost/effect info
            font.setColor(Color.LIGHT_GRAY);
            String costInfo = getSkillCostInfo(i);
            font.draw(batch, "(" + costInfo + ")", textX + gyphLayout.width + 10, rowY + iconSize - 5);
            gyphLayout.reset();
        }

        // Draw detailed info for selected skill at bottom if there is one
        if (selectedSkill >= 0) {
            drawDetailedSkillInfo(batch, bounds, selectedSkill);
        }
    }

    private String getSkillCostInfo(int skillIndex) {
        switch (skillIndex) {
            case 0:
                return "Cost: None";
            case 1:
                return "Cost: 5 MP";
            case 2:
                return "Cost: None";
            case 3:
                return "Cost: 10 MP";
            case 4:
                return "Cost: None";
            default:
                return "";
        }
    }

    private void drawDetailedSkillInfo(SpriteBatch batch, Rectangle bounds, int skillIndex) {
        float padding = 15;
        float infoHeight = 100;
        float infoY = bounds.y + padding;

        // Draw info panel background
        batch.setColor(Color.WHITE);
        batch.draw(panelTexture,
                bounds.x + padding,
                infoY,
                bounds.width - padding * 2,
                infoHeight);

        // Draw selected skill name as header
        font.setColor(HEADER_COLOR);
        layout.setText(font, skillNames[skillIndex]);
        font.draw(batch, skillNames[skillIndex],
                bounds.x + padding * 2,
                infoY + infoHeight - padding);

        // Draw full description
        font.setColor(DESC_COLOR);
        layout.setText(font, skillDescriptions[skillIndex], Color.WHITE, bounds.width - padding * 4, 1, true);
        font.draw(batch, layout, bounds.x + padding * 2, infoY + infoHeight - padding * 3);

        // Draw effect details
        String effectInfo = getSkillEffectDetails(skillIndex);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, effectInfo, bounds.x + padding * 2, infoY + padding * 2);
    }

    private String getSkillEffectDetails(int skillIndex) {
        switch (skillIndex) {
            case 0:
                return "Effect: Basic physical damage based on your attack power";
            case 1:
                return "Effect: Magical damage based on dictionary word point value";
            case 2:
                return "Effect: Damage based on manually typed word, invalid words damage you";
            case 3:
                return "Effect: Restores 20% of your max HP, costs 10 MP";
            case 4:
                return "Effect: Increases defense by 50% for 2 turns, recovers 5 MP";
            default:
                return "";
        }
    }

    public boolean handleClick(float screenX, float screenY, Rectangle bounds) {
        float contentStartY = bounds.y + bounds.height - 110;
        float contentWidth = bounds.width - 60;
        float skillListWidth = contentWidth * 0.5f;

        float iconSize = 70;
        float padding = 15;
        float rowHeight = iconSize + padding * 2;

        // Check if click is within skill list area
        float skillListX = bounds.x + 30;
        float skillListY = contentStartY;
        float skillListHeight = rowHeight * 5 + padding * 2;

        // If click is outside skill list area, return false
        if (screenX < skillListX || screenX > skillListX + skillListWidth ||
                screenY < skillListY - skillListHeight || screenY > skillListY) {
            return false;
        }

        // Determine which skill row was clicked
        for (int i = 0; i < skillButtonTextures.length; i++) {
            float rowY = skillListY - (i + 1) * rowHeight;

            if (screenY >= rowY && screenY <= rowY + rowHeight) {
                // Only select if skill is unlocked
                boolean isUnlocked = character.getLevel() >= i + 1;
                if (isUnlocked) {
                    selectedSkill = i;
                    return true;
                }
            }
        }

        return false;
    }

    public boolean handleHover(float screenX, float screenY, Rectangle bounds) {
        float contentStartY = bounds.y + bounds.height - 110;
        float contentWidth = bounds.width - 60;
        float skillListWidth = contentWidth * 0.5f;

        float iconSize = 70;
        float padding = 15;
        float rowHeight = iconSize + padding * 2;

        // Reset hover state
        int oldHoveredSkill = hoveredSkill;
        hoveredSkill = -1;

        // Check if mouse is within skill list area
        float skillListX = bounds.x + 30;
        float skillListY = contentStartY;
        float skillListHeight = rowHeight * 5 + padding * 2;

        if (screenX >= skillListX && screenX <= skillListX + skillListWidth &&
                screenY >= skillListY - skillListHeight && screenY <= skillListY) {

            // Determine which skill row is hovered
            for (int i = 0; i < skillButtonTextures.length; i++) {
                float rowY = skillListY - (i + 1) * rowHeight;

                if (screenY >= rowY && screenY <= rowY + rowHeight) {
                    boolean isUnlocked = character.getLevel() >= i + 1;
                    if (isUnlocked) {
                        hoveredSkill = i;
                    }
                    return oldHoveredSkill != hoveredSkill; // Return true if state changed
                }
            }
        }

        return oldHoveredSkill != hoveredSkill; // Return true if state changed
    }

    public void dispose() {
        if (skillButtonTextures != null) {
            for (Texture texture : skillButtonTextures) {
                if (texture != null) texture.dispose();
            }
        }
        if (highlightTexture != null) highlightTexture.dispose();
        if (panelTexture != null) panelTexture.dispose();
        if (selectedBorderTexture != null) selectedBorderTexture.dispose();
    }
}