package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.screen.DarkestDungeon;

/**
 * Handles input processing for the DarkestDungeon combat screen
 */
public class DungeonInputProcessor extends InputAdapter {
    private final DarkestDungeon dungeonScreen;
    private final GameController gameController;

    // Cached references to frequently accessed fields to reduce access overhead
    private final float SCREEN_HEIGHT = 720;
    private boolean isPaused;
    private boolean showTutorial;
    private boolean victory;
    private boolean defeated;
    private boolean isEnded;
    private boolean waitingForInput;
    private boolean showInputField;
    private String inputWord;
    private int currentTutorialPage;

    public DungeonInputProcessor(DarkestDungeon dungeonScreen) {
        this.dungeonScreen = dungeonScreen;
        this.gameController = dungeonScreen.getGameController();

        // Initialize cached values
        this.isPaused = dungeonScreen.isPaused();
        this.showTutorial = dungeonScreen.isShowTutorial();
        this.victory = dungeonScreen.isVictory();
        this.defeated = dungeonScreen.isDefeated();
        this.isEnded = dungeonScreen.isEnded();
        this.waitingForInput = dungeonScreen.isWaitingForInput();
        this.showInputField = dungeonScreen.isShowInputField();
        this.inputWord = dungeonScreen.getInputWord();
        this.currentTutorialPage = dungeonScreen.getCurrentTutorialPage();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (showTutorial) {
                dungeonScreen.setShowTutorial(false);
                dungeonScreen.setCurrentTutorialPage(0);
                this.showTutorial = false;
                this.currentTutorialPage = 0;
            } else {
                dungeonScreen.setPaused(!dungeonScreen.isPaused());
                this.isPaused = !this.isPaused;
            }
            return true;
        }

        if (isPaused) {
            if (keycode == Input.Keys.Q) {
                Gdx.app.exit();
                return true;
            }
            if (keycode == Input.Keys.T) {
                boolean newTutorialState = !showTutorial;
                dungeonScreen.setShowTutorial(newTutorialState);
                dungeonScreen.setCurrentTutorialPage(0);
                this.showTutorial = newTutorialState;
                this.currentTutorialPage = 0;
                return true;
            }
            return true; // Ignore other input when paused
        }

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!Gdx.input.justTouched()) return false;

        screenY = Gdx.graphics.getHeight() - screenY; // Invert Y coordinate

        if (showTutorial) {
            return handleTutorialTouch(screenX, screenY);
        }

        if (victory) {
            return handleVictoryTouch(screenX, screenY);
        } else if (defeated) {
            return handleDefeatTouch(screenX, screenY);
        } else {
            if (isPaused && !showTutorial) {
                return handlePauseMenuTouch(screenX, screenY);
            }

            if (dungeonScreen.getCombatState() == DarkestDungeon.CombatState.PLAYER_TURN &&
                    !dungeonScreen.isAnimating() && !isPaused) {
                return handleCombatTouch(screenX, screenY);
            } else if (dungeonScreen.getCombatState() == DarkestDungeon.CombatState.COMBAT_END) {
                dungeonScreen.getGame().setScreen(
                        new DarkestDungeon(dungeonScreen.getGame(), gameController));
                return true;
            }
        }

        return false;
    }

    private boolean handleTutorialTouch(int screenX, int screenY) {
        boolean continueShowing = dungeonScreen.getTutorialRenderer().handleClick(screenX, screenY);
        if (!continueShowing) {
            dungeonScreen.setShowTutorial(false);
            dungeonScreen.setCurrentTutorialPage(0);
            this.showTutorial = false;
            this.currentTutorialPage = 0;
        } else {
            dungeonScreen.setCurrentTutorialPage(
                    dungeonScreen.getTutorialRenderer().getCurrentPage());
            this.currentTutorialPage = dungeonScreen.getCurrentTutorialPage();
        }
        return true;
    }

    private boolean handleVictoryTouch(int screenX, int screenY) {
        Rectangle continueButtonBounds = dungeonScreen.getContinueButtonBounds();
        if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
            gameController.getCharacter().addItem(
                    dungeonScreen.getItem(), dungeonScreen.getReward().getAmount());
            gameController.getCharacter().setHealth(dungeonScreen.getPlayerHP());
            gameController.getCharacter().setMana(dungeonScreen.getPlayerMana());

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    gameController.setState(GameState.EXPLORING);
                    gameController.getCharacter().setHealth(dungeonScreen.getPlayerHP());
                    gameController.getCharacter().setMana(dungeonScreen.getPlayerMana());
                    gameController.getMapRenderer().setZoomed(false);
                    gameController.setRenderCharacter(true);
                    dungeonScreen.getGame().changeScreen("GAME");

                    if (dungeonScreen.getNewLevel() > dungeonScreen.getCurrentLevel())
                        gameController.showLevelUpNotification();
                }
            }, 0.5f);

            String enemyName = dungeonScreen.getEnemyName();
            if (enemyName.equalsIgnoreCase("Demon"))
                gameController.completedDungeon2();
            else if (enemyName.equalsIgnoreCase("Frost Guardian"))
                gameController.defeatedFrostGuardian();
        }
        return true;
    }

    private boolean handleDefeatTouch(int screenX, int screenY) {
        Rectangle continueButtonBounds = dungeonScreen.getContinueButtonBounds();
        if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
            if (!isEnded) {
                dungeonScreen.getGame().changeScreen("GAME");
                gameController.setState(GameState.EXPLORING);
                gameController.setPreviousState(GameState.EXPLORING);
                gameController.returnToTower(dungeonScreen.getEnemyName());
                gameController.setRenderCharacter(true);
                gameController.getCharacter().setDirection("knocked_down");
            } else {
                dungeonScreen.getGame().changeScreen("GAME_OVER");
            }
            return true;
        }
        return true; // Ngăn xử lý các input khác khi đã thua
    }

    private boolean handlePauseMenuTouch(int screenX, int screenY) {
        float menuWidth = 400;
        float menuHeight = 350;
        float menuX = (dungeonScreen.SCREEN_WIDTH - menuWidth) / 2;
        float menuY = (dungeonScreen.SCREEN_HEIGHT - menuHeight) / 2;

        float buttonWidth = 240;
        float buttonHeight = 40;

        float continueButtonX = menuX + (menuWidth - buttonWidth) / 2;
        float continueButtonY = menuY + 210;
        float tutorialButtonY = menuY + 160;
        float menuButtonY = menuY + 110;
        float quitButtonY = menuY + 60;

        // Continue button
        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                screenY >= continueButtonY && screenY <= continueButtonY + buttonHeight) {
            dungeonScreen.setPaused(false);
            this.isPaused = false;
            return true;
        }

        // Tutorial button
        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                screenY >= tutorialButtonY && screenY <= tutorialButtonY + buttonHeight) {
            dungeonScreen.setShowTutorial(true);
            dungeonScreen.setCurrentTutorialPage(0);
            this.showTutorial = true;
            this.currentTutorialPage = 0;
            return true;
        }

        // Main menu button
        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                screenY >= menuButtonY && screenY <= menuButtonY + buttonHeight) {
            gameController.setState(GameState.MAIN_MENU);
            dungeonScreen.getGame().changeScreen("GAME");
            return true;
        }

        // Quit button
        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                screenY >= quitButtonY && screenY <= quitButtonY + buttonHeight) {
            Gdx.app.exit();
            return true;
        }

        return true; // Ngăn xử lý các input khác khi đang pause
    }

    private boolean handleCombatTouch(int screenX, int screenY) {
        if (screenX >= dungeonScreen.getMenuX() + 10 &&
                screenX <= dungeonScreen.getMenuX() + dungeonScreen.getMenuWidth() - 10) {

            // Check skill buttons
            for (int i = 0; i < 5; i++) {
                float skillY = dungeonScreen.getButtonY() -
                        i * (dungeonScreen.getButtonHeight() + dungeonScreen.getButtonSpacing());
                if (screenY >= skillY - dungeonScreen.getButtonHeight() && screenY <= skillY) {
                    if (dungeonScreen.isSkillEnabled(i)) {
                        dungeonScreen.handlePlayerAction(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        if (waitingForInput && showInputField) {
            if (character == '\r' || character == '\n') { // Enter key
                if (!inputWord.trim().isEmpty()) {
                    dungeonScreen.processWordInput(inputWord.trim());
                }
            } else if (character == '\b') { // Backspace
                if (inputWord.length() > 0) {
                    inputWord = inputWord.substring(0, inputWord.length() - 1);
                    dungeonScreen.setInputWord(inputWord);
                }
            } else if (java.lang.Character.isLetter(character)) {
                inputWord += java.lang.Character.toUpperCase(character);
                dungeonScreen.setInputWord(inputWord);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        screenY = Gdx.graphics.getHeight() - screenY; // Invert Y coordinate
        dungeonScreen.setMousePosition(screenX, screenY);
        dungeonScreen.updateTooltip();
        return false;
    }

    // Update cached values from dungeonScreen
    public void updateState() {
        this.isPaused = dungeonScreen.isPaused();
        this.showTutorial = dungeonScreen.isShowTutorial();
        this.victory = dungeonScreen.isVictory();
        this.defeated = dungeonScreen.isDefeated();
        this.isEnded = dungeonScreen.isEnded();
        this.waitingForInput = dungeonScreen.isWaitingForInput();
        this.showInputField = dungeonScreen.isShowInputField();
        this.inputWord = dungeonScreen.getInputWord();
        this.currentTutorialPage = dungeonScreen.getCurrentTutorialPage();
    }
}