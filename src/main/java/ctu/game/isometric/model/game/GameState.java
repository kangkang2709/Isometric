package ctu.game.isometric.model.game;

public enum GameState {
    EXPLORING,   // Normal gameplay
    DIALOG,      // Dialog active - restrict movement
    MENU,        // Menu open - different input handling
    CUTSCENE,
    SETTINGS,   // Settings menu
    MAIN_MENU,  // Main menu
    COMBAT,
    CHARACTER_CREATION,
    GAMEPLAY,// Combat state
    LOAD_GAME
}
