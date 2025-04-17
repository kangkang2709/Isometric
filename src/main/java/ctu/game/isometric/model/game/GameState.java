package ctu.game.isometric.model.game;

public enum GameState {
    EXPLORING,   // Normal gameplay
    DIALOG,      // Dialog active - restrict movement
    MENU,        // Menu open - different input handling
    CUTSCENE,
    SETTINGS,   // Settings menu
}
