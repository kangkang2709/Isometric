package ctu.game.isometric.model.puzzle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Achievement;
import ctu.game.isometric.model.world.IsometricMap;

import java.util.ArrayList;
import java.util.List;

import static ctu.game.isometric.IsometricGame.getGameController;

public class PressurePlatePuzzle {
    private List<PressurePlate> plates;
    private IsometricMap map;
    private boolean completed = false;
    private int requiredActivations;
    private String puzzleId;

    public PressurePlatePuzzle(String puzzleId, IsometricMap map, int requiredActivations) {
        this.puzzleId = puzzleId;
        this.map = map;
        this.plates = new ArrayList<>();
        this.requiredActivations = requiredActivations;
    }

    public void addPlate(int x, int y, String effectType, int targetX, int targetY) {
        plates.add(new PressurePlate(x, y, effectType, targetX, targetY));
        if (!effectType.equals("trap"))
            map.setTileWalkable(targetX, targetY, false);
    }

    public void addPlate(int x, int y, String effectType, int targetX, int targetY, TextureRegion inactiveTexture, TextureRegion activeTexture) {
        PressurePlate plate = new PressurePlate(x, y, effectType, targetX, targetY, inactiveTexture, activeTexture);
        plate.setTextures(inactiveTexture, activeTexture);
        plates.add(plate);
        if (!effectType.equals("trap"))
            map.setTileWalkable(targetX, targetY, false);
    }


    public void update(Character character) {

        for (PressurePlate plate : plates) {
            if (!plate.isActivated()) {
                plate.checkCharacter(character);
            }

        }


    }

    public void reset() {
        for (PressurePlate plate : plates) {
            plate.reset();

            // Revert any map changes from the plate effects
            if (plate.getEffectType().equals("door")) {
                map.setTileWalkable(plate.getTargetX(), plate.getTargetY(), false);
            }
        }
        completed = false;
    }

    public void loadTexturesForType(String effectType, String inactiveTexturePath, String activeTexturePath) {
        Texture inactiveTexture = new Texture(Gdx.files.internal(inactiveTexturePath));
        Texture activeTexture = new Texture(Gdx.files.internal(activeTexturePath));

        TextureRegion inactiveRegion = new TextureRegion(inactiveTexture);
        TextureRegion activeRegion = new TextureRegion(activeTexture);

        for (PressurePlate plate : plates) {
            if (plate.getEffectType().equals(effectType)) {
                plate.setTextures(inactiveRegion, activeRegion);
            }
        }
    }

    public void clear() {
        plates.clear();
        completed = false;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<PressurePlate> getPlates() {
        return plates;
    }

    public void setPlates(List<PressurePlate> plates) {
        this.plates = plates;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public class PressurePlate {
        private int gridX, gridY;
        private boolean activated = false;
        private String effectType; // "door", "bridge", "trap", "teleport", etc.
        private int targetX, targetY; // Target coordinates for the effect
        private TextureRegion inactiveTexture;
        private TextureRegion activeTexture;

        public PressurePlate(int x, int y, String effectType, int targetX, int targetY) {
            this.gridX = x;
            this.gridY = y;
            this.effectType = effectType;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public PressurePlate(int x, int y, String effectType, int targetX, int targetY, TextureRegion inactiveTexture, TextureRegion activeTexture) {
            this.gridX = x;
            this.gridY = y;
            this.effectType = effectType;
            this.targetX = targetX;
            this.targetY = targetY;
            this.inactiveTexture = inactiveTexture;
            this.activeTexture = activeTexture;
        }

        public boolean checkCharacter(Character character) {
            if (!activated && (int) Math.floor(character.getGridX()) == gridX &&
                    (int) Math.floor(character.getGridY()) == gridY) {
                activated = true;
                triggerEffect(character);
                return true;
            }
            return false;
        }

        private void triggerEffect(Character character) {
            switch (effectType) {
                case "door":
                    map.setTileWalkable(targetX, targetY, true);
                    if (targetX == 11 && targetY == 8) {
                        getGameController().getDialogController().showSimpleMessage("Nghi thức đã hoàn thành, cánh cửa đã mở!");
                    }
                    break;
                case "bridge":
                    // Create a bridge (make the target tile walkable)
                    map.setTileWalkable(targetX, targetY, true);
                    break;
                case "trap":
                    // Damage the character
                    character.setHealth(character.getHealth() - 5);
                    if (character.getHealth() <= 0) {
                        character.setHealth(0); // Ensure health doesn't go negative
                        boolean isGameOver = character.gameOver();
                        if (isGameOver) {
                            getGameController().returnToTower("trap");
                        } else {
                            getGameController().getGame().changeScreen("GAME_OVER");
                        }
                    }


                    break;
                case "teleport":
                    // Teleport character to target location
                    character.setPosition(targetX, targetY);
                    break;
                case "reward":
                    // Give the player some reward (exp, item, etc)
                    character.addScore(10);
                    character.addExperience(5);
                    break;
            }
        }

        public void reset() {
            activated = false;
        }

        public boolean isActivated() {
            return activated;
        }

        public int getGridX() {
            return gridX;
        }

        public int getGridY() {
            return gridY;
        }

        public String getEffectType() {
            return effectType;
        }

        public int getTargetX() {
            return targetX;
        }

        public int getTargetY() {
            return targetY;
        }

        public void setTextures(TextureRegion inactive, TextureRegion active) {
            this.inactiveTexture = inactive;
            this.activeTexture = active;
        }

        public TextureRegion getCurrentTexture() {
            return activated ? activeTexture : inactiveTexture;
        }
    }
}