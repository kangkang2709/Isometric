package ctu.game.isometric.controller;

import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.model.dialog.DialogModel;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.world.IsometricMap;

public class GameController {
    private IsometricGame game;
    private Character character;
    private IsometricMap map;
    private DialogModel dialogModel;
    private InputController inputController;
    private DialogController dialogController;

    public GameController(IsometricGame game) {
        this.game = game;
        this.map = new IsometricMap();
        this.character = new Character("characters/player.png", 2, 2);
        this.dialogModel = new DialogModel();
        this.dialogController = new DialogController(dialogModel);
        this.inputController = new InputController(this);

        // Show initial dialog
        dialogController.showDialog("Welcome to my isometric visual novel!");
    }

    public void update(float delta) {
        // Update input controller
        inputController.update(delta);

        // Update character animation
        character.update(delta);

        // Reset character movement flag
        character.setMoving(false);
    }

    public boolean canMove(int dx, int dy) {
        int newX = (int) (character.getGridX() + dx);
        int newY = (int) (character.getGridY() + dy);

        // Check map boundaries
        if (newX < 0 || newY < 0) return false;
        if (newY >= map.getMapData().length) return false;
        if (newX >= map.getMapData()[0].length) return false;

        // Check if tile is walkable (0 = empty/unwalkable in your current implementation)
        return map.getMapData()[newY][newX] != 0;
    }

    public void moveCharacter(int dx, int dy) {
        float newX = character.getGridX() + dx;
        float newY = character.getGridY() + dy;
        character.setPosition(newX, newY);
        character.setMoving(true);

        // Optional: Trigger a dialog when character reaches certain positions
        checkPositionEvents(newX, newY);
    }

    private void checkPositionEvents(float x, float y) {
        // Example: Show dialog when character reaches specific positions
        if (x == 3 && y == 3) {
            dialogController.showDialog("You found a secret area!");
        }
    }

    // Getters
    public Character getCharacter() { return character; }
    public IsometricMap getMap() { return map; }
    public DialogModel getDialogModel() { return dialogModel; }
    public InputController getInputController() { return inputController; }
    public DialogController getDialogController() { return dialogController; }
}