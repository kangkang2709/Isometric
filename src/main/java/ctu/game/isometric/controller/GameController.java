package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.quest.BountyBoardController;
import ctu.game.isometric.controller.quiz.MulChoiceQuizController;
import ctu.game.isometric.model.dictionary.Dictionary;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.entity.NPC;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.ItemLoader;
import ctu.game.isometric.view.menu.CharacterCreation;
import ctu.game.isometric.view.menu.MainMenu;
import ctu.game.isometric.view.menu.PauseMenu;
import ctu.game.isometric.view.menu.SettingsMenu;
import ctu.game.isometric.view.renderer.CutsceneRenderer;
import ctu.game.isometric.controller.gameplay.GameplayController;
import ctu.game.isometric.controller.quiz.QuizController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.EnemyLoader;
import ctu.game.isometric.util.WordNetValidator;
import ctu.game.isometric.view.renderer.MapRenderer;
import ctu.game.isometric.view.renderer.NPCRenderer;
import ctu.game.isometric.view.renderer.TransitionRenderer;
import ctu.game.isometric.view.ui.*;
import ctu.game.isometric.view.view.BountyBoardView;
import ctu.game.isometric.view.view.CharacterInfoDisplay;
import ctu.game.isometric.view.view.QuestTrackerView;

import java.util.*;

public class GameController {
    private IsometricGame game;
    private Character character;
    private IsometricMap map;
    private OrthographicCamera camera;
    private InputController inputController;
    private DialogController dialogController; // New field
    private MusicController musicController;
    private PauseMenu pauseMenu;
    private SettingsMenu settingsMenu;
    private MainMenu mainMenuController;
    private TransitionRenderer transitionRenderer;
    private GameplayController gameplayController;
    private LoadGameController loadGameController;

    private CharacterCreation characterCreationController;
    private GameState currentState = GameState.MAIN_MENU;
    private GameState previousState = GameState.MAIN_MENU;
    private CutsceneRenderer cutsceneController;
    boolean isCreated = false;
    private ExploringUI exploringUI;
    private InventoryUI inventoryUI;

    private EffectManager effectManager;
    private WordNetValidator wordNetValidator;
    private QuizController quizController;
    private MulChoiceQuizController mulChoiceQuizController;
    private ctu.game.isometric.view.view.DictionaryView dictionaryView;
    private Dictionary dictionary;
    private BitmapFont font;
    private CharacterInfoDisplay characterDisplay;

    private EventManager eventManager;
    private MapEvent currentEvent;
    private AchievementManager achievementManager;
    private AchievementUI achievementUI;

    private Pathfinder pathfinder;
    private Date currentPlayTime;
    private MerchantUI merchantUI;
    // Add LevelUpNotification field
    private LevelUpNotification levelUpNotification;

    private NPCManager npcManager;
    private NPCRenderer npcRenderer;
    private BountyBoardController bountyBoardController;
    private BountyBoardView bountyBoardView;
    private QuestTrackerView questTrackerView;

    private TutorialUI tutorialUI;

    public GameController(IsometricGame game) {
        this.game = game;

        this.map = new IsometricMap();
        this.eventManager = new EventManager(map);

        this.character = new Character(10, 10);
        this.inputController = new InputController(this);
        this.dialogController = new DialogController(this);
        this.musicController = new MusicController();
        characterCreationController = new CharacterCreation(this);
        this.pauseMenu = new PauseMenu(this);


        effectManager = new EffectManager();
        this.loadEffects();
        this.settingsMenu = new SettingsMenu(this);
        this.mainMenuController = new MainMenu(this);
        this.transitionRenderer = new TransitionRenderer();
        this.cutsceneController = new CutsceneRenderer(this);
        loadGameController = new LoadGameController(this);
//        this.wordValidator.loadDictionary();
        this.wordNetValidator = new WordNetValidator();
        this.wordNetValidator.loadDictionary();


        this.gameplayController = new GameplayController(this);
        this.quizController = new QuizController(this);
        this.mulChoiceQuizController = new MulChoiceQuizController(this);

        initializeDictionary();
        this.musicController.initialize();
        this.musicController.playMusicForState(GameState.MAIN_MENU);
        inputController.setEffectManager(effectManager);

        achievementManager = new AchievementManager(this);
        achievementUI = new AchievementUI(this);
        this.currentPlayTime = new Date();

        // Initialize the level up notification
        this.levelUpNotification = new LevelUpNotification(this);

        this.pathfinder = new Pathfinder(map);
        npcManager = new NPCManager(this);

        bountyBoardController = new BountyBoardController(this);
        bountyBoardView = new BountyBoardView(bountyBoardController);
        questTrackerView = new QuestTrackerView(this);

        tutorialUI = new TutorialUI(this);

    }


    boolean isLoadNPCs = false;

    public void initializeNPCs(MapRenderer mapRenderer) {
        this.npcRenderer = new NPCRenderer(npcManager.getNpcs(), mapRenderer, character);

        if (isLoadNPCs == false) {
            npcRenderer.loadNPCAnimations();
            isLoadNPCs = true;
        }
        this.pathfinder.setNpcPositions(npcManager.getNpcPositions());
    }


    public void interactWithNPC() {
//
//        if (!getCharacter().getIsTutorials().get("npc")) {
//            tutorialUI.show("npc");
//            getCharacter().setTutorialCompleted("npc");
//        }

        NPC npc = findNPCNear(character.getGridX(), character.getGridY());
        if (npc != null) {
            npc.setBehaviorState("Dialogue");
            switch (npc.getNpcName()) {
                case "QuizMaster":
                    if (getCharacter().getAttempFlags().get("quizAttempts") >= 3) {
                        dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startQuiz();
                            getCharacter().getAttempFlags().put("quizAttempts", 1);
                        });
                        dialogController.startDialog("chapter_quiz_intro", "scene_meet_npc");

                    }
                    break;
                case "MulQuizMaster":
                    if (getCharacter().getAttempFlags().get("mulQuizAttempts") >= 1) {
                        dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startMulChoiceQuiz();
                            getCharacter().getAttempFlags().put("mulQuizAttempts", 1);

                        });
                        dialogController.startDialog("chapter_quiz_intro", "scene_meet_npc");
                    }
                    break;
                case "Patche Trader":
                    dialogController.showMessageWithChoices("Chúng tôi có vài ủy thác. Bạn có muốn tiếp nhận không?", "CCCD",
                            "Yes [Cần CCCD]", "No", () -> setState(GameState.BOUNTY_BOARD));
                    ;
                    break;
                case "Merchant":
                    dialogController.showMessageWithChoices("I have something to trade. What do you want", "CCCD",
                            "Yes [Cần CCCD]", "No", () -> merchantUI.show());
                    break;
                case "Waitess Hera":
                    dialogController.showMessageWithChoices("Do want to heal", "CCCD",
                            "Yes [Cần CCCD]", "No", () -> getCharacter().recovery());
                    break;
                case "Teleporter":
                    dialogController.startDialog("teleporting_background", "scene_intro");
                default:
                    break;
            }

        } else {
            dialogController.showSimpleMessage("No NPC nearby to interact with.");
        }
    }

    public void showNPCBackStory() {
        NPC npc = findNPCNear(character.getGridX(), character.getGridY());

        if (npc != null) {
            npc.setBehaviorState("Dialogue");
            switch (npc.getNpcName()) {
                case "WordSeer Kang":
                    if (getCharacter().getAttempFlags().get("quizAttempts") >= 3) {
                        dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startQuiz();
                            getCharacter().getAttempFlags().put("quizAttempts", 1);
                        });
                        dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    }

                    break;
                case "Lorekeeper Tian":
                    if (getCharacter().getAttempFlags().get("mulQuizAttempts") >= 3) {
                        dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startMulChoiceQuiz();
                            getCharacter().getAttempFlags().put("mulQuizAttempts", 1);

                        });
                        dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    }
                    break;
                case "Barbarian Captain":
                    dialogController.setOnDialogFinishedAction(() -> {
                        setState(GameState.BOUNTY_BOARD);
                    });
                    dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    break;
                case "Patche Trader":
                    dialogController.setOnDialogFinishedAction(() -> {
                        merchantUI.show();
                    });
                    dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    break;
                case "Waitess Hera":
                    dialogController.setOnDialogFinishedAction(() -> {
                        getCharacter().recovery();
                        effectManager.spawnEffectEvent("Star_Trail", 660, 390);
                    });
                    dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    break;
                case "Teleporter":
                    dialogController.startDialog("teleporting_background", "scene_intro");
                default:
                    break;
            }

        } else {
            dialogController.showSimpleMessage("No NPC nearby to interact with.");
        }
    }


    public void changeBehaviorStateNPC() {
        for (NPC npc : npcManager.getNpcs().values()) {
            if (npc.getBehaviorState().equals("Dialogue")) {
                npc.setBehaviorState("Idle");
            }
        }
    }

    public NPC findNPCNear(float x, float y) {
        for (NPC npc : npcManager.getNpcs().values()) {
            float npcX = npc.getXPosition();
            float npcY = npc.getYPosition();
            // Check if the NPC is within a 1 tile distance
            if (Math.abs(npcX - x) <= 1f && Math.abs(npcY - y) <= 1f) {
                return npc; // Return the first NPC found in range
            }
        }
        return null; // No NPC found in range
    }

    public void initializeDictionary() {
        if (dictionary == null) {
            dictionary = new ctu.game.isometric.model.dictionary.Dictionary();
        }

        if (dictionaryView == null) {
            dictionaryView = new ctu.game.isometric.view.view.DictionaryView(this, dictionary, this.wordNetValidator);
        }
    }

    public TutorialUI getTutorialUI() {
        return tutorialUI;
    }

    // You might want to add methods to show/hide the tutorial at appropriate times
    public void showTutorial() {
        tutorialUI.show();
    }

    public void hideTutorial() {
        tutorialUI.hide();
    }

    public BountyBoardController getBountyBoardController() {
        return bountyBoardController;
    }

    public void setBountyBoardController(BountyBoardController bountyBoardController) {
        this.bountyBoardController = bountyBoardController;
    }

    public void showAchievementUI() {
        achievementUI.show();
    }

    public AchievementUI getAchievementUI() {
        return achievementUI;
    }

    public void resetLearnedWords() {
        Set<Word> learnedWordList = new HashSet<>();
        for (String learnedWord : getCharacter().getLearnedWords()) {
            Word word = wordNetValidator.getWordDetails(learnedWord);
            if (word != null) {
                learnedWordList.add(word);
            }
        }
        dictionary.setLearnedWords(learnedWordList);
        dictionary.getNewWords().clear();

    }

    public void moveCharacterAlongPath(int targetX, int targetY) {
        float startX = character.getGridX();
        float startY = character.getGridY();

        // Find path with a reasonable maximum length
        Array<int[]> path = pathfinder.findPath((int) startX, (int) startY, targetX, targetY, 30);

        if (path.size > 0) {
            // Remove the first point if it's the current position
            if (path.size > 1 && path.get(0)[0] == (int) startX && path.get(0)[1] == (int) startY) {
                path.removeIndex(0);
            }

            character.setPath(path);
            effectManager.playClickSound();

            // Set the target indicator position
            inputController.showTargetIndicator(targetX, targetY);

            checkPositionEvents((int) targetX, (int) targetY);
        }
    }


    public void loadEffects() {
        effectManager.loadEffect("Starlight", "effects/Starlight/");
        effectManager.loadEffect("Leaf_fall", "effects/Leaf_fall/");
        effectManager.loadEffect("Star_Trail", "effects/Star_Trail/");
    }

    public void loadCharacter(Character character, Date lastSaveTime) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }
        System.out.println("Attempt flags before initialization: " + character.getAttempFlags());
//        if(character.getAttempFlags() == null) {
//            character.setAttempFlags(new HashMap<>());
//            character.getAttempFlags().put("quizAttempts", 0);
//            character.getAttempFlags().put("mulQuizAttempts", 0);
//            character.getAttempFlags().put("fallen", 0);
//            character.getAttempFlags().put("wrongWord", 0);
//            System.out.println("Character attempt flags initialized.");
//        }
        this.character = character;
        this.character.setLastSaveTime(lastSaveTime);

        if (currentPlayTime.compareTo(lastSaveTime) > 0) {
            // Create Calendar instances
            Calendar currentCal = Calendar.getInstance();
            Calendar lastSaveCal = Calendar.getInstance();
            currentCal.setTime(currentPlayTime);
            lastSaveCal.setTime(lastSaveTime);

            // Get date components (year, month, day)
            int currentDay = currentCal.get(Calendar.DAY_OF_MONTH);
            int currentMonth = currentCal.get(Calendar.MONTH);
            int currentYear = currentCal.get(Calendar.YEAR);

            int lastDay = lastSaveCal.get(Calendar.DAY_OF_MONTH);
            int lastMonth = lastSaveCal.get(Calendar.MONTH);
            int lastYear = lastSaveCal.get(Calendar.YEAR);

            // Check if calendar day has changed
            if (currentYear > lastYear || currentMonth > lastMonth || currentDay > lastDay) {
                // Calendar day has changed, reset both counters

                this.character.getAttempFlags().put("quizAttempts", 0);
                this.character.getAttempFlags().put("mulQuizAttempts", 0);
            } else {
                System.out.println(this.character.getAttempFlags().get("quizAttempts"));
                System.out.println(this.character.getAttempFlags().get("mulQuizAttempts"));
            }
        }

        // Load the saved character
        this.isCreated = true;

        // Re-initialize map and other references
        this.character.setGameMap(this.getMap());

        // Now load the saved dictionary from file
        if (character.getWordFilePath() != null) {

        }
    }

    public void update(float delta) {

        switch (currentState) {
            case EXPLORING:
                if (dialogController.isDialogActive() && !dialogController.shouldRenderBackground()) {
                    if (currentEvent != null && currentEvent.getEventType().equals("treasure")) {
                        effectManager.update(delta);

                    }
                    npcRenderer.update(delta);
                } else if (!merchantUI.isVisible()) {
                    inputController.updateCooldown(delta);
                    character.update(delta);
                    npcRenderer.update(delta);
                }

                levelUpNotification.update(delta);
                map.getPuzzle().update(character);
                break;
            case BOUNTY_BOARD:

                break;
            case CHARACTER_CREATION:
                characterCreationController.update(delta);
                break;
            case GAMEPLAY:
                gameplayController.update(delta);
                break;
            case MENU:
                pauseMenu.update(delta);
                break;
            case DICTIONARY:
                dictionaryView.update(delta);
                break;
            case MAIN_MENU:
                mainMenuController.update(delta);
                break;
            case LOAD_GAME:
                loadGameController.update(delta);
                break;
            case QUIZZES:
                quizController.update(delta);
                break;
            case MULTIPLE_CHOICE_QUIZZES:
                mulChoiceQuizController.update(delta);
                break;
            case SETTINGS:
                settingsMenu.update(delta);
                break;
            case CUTSCENE:
                if (character.getFlags() != null) {
                    if (character.getFlags().isEmpty())
                        startCutscene("intro");
                    tutorialUI.show("movement");
                    getCharacter().setTutorialCompleted("movement");
                }
                cutsceneController.update(delta);
                break;

        }

    }

    public void startMulChoiceQuiz() {
        setPreviousState(currentState);
        setState(GameState.MULTIPLE_CHOICE_QUIZZES);
        mulChoiceQuizController.startQuiz();
    }


    public void startQuiz() {
        setPreviousState(currentState);
        setState(GameState.QUIZZES);
        quizController.startQuiz();
    }

    public MulChoiceQuizController getMultipleChoiceQuizController() {
        return mulChoiceQuizController;
    }

    public void setMulChoiceQuizController(MulChoiceQuizController mulChoiceQuizController) {
        this.mulChoiceQuizController = mulChoiceQuizController;
    }

    public TransitionRenderer getTransitionController() {
        return transitionRenderer;
    }

    public void setTransitionController(TransitionRenderer transitionRenderer) {
        this.transitionRenderer = transitionRenderer;
    }

    public ctu.game.isometric.view.view.DictionaryView getDictionaryView() {
        return dictionaryView;
    }

    public void setDictionaryView(ctu.game.isometric.view.view.DictionaryView dictionaryView) {
        this.dictionaryView = dictionaryView;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState currentState) {
        this.currentState = currentState;
    }

    public void setState(GameState newState) {
        if (currentState == newState) return;

        final GameState oldState = currentState;

        if (newState != GameState.SETTINGS) {
            previousState = oldState;
        }

        transitionRenderer.startLoadingScreen(() -> {
            // This code executes after the fade out, during loading
            currentState = newState;

            // Update music for the new state
            if (musicController != null) {
                musicController.playMusicForState(newState);
            }
        });

    }

    private void onStateChanged(GameState oldState, GameState newState) {
        // Notify relevant subsystems about state change
    }

    public void startCutscene(String cutsceneName) {
        setPreviousState(currentState);
        setState(GameState.CUTSCENE);
        cutsceneController.loadCutscene(cutsceneName);
        character.getFlags().add(cutsceneName);
    }

    public CutsceneRenderer getCutsceneController() {
        return cutsceneController;
    }


    public void returnToPreviousState() {
        setState(previousState);
    }

    public GameState getPreviousState() {
        return previousState;
    }

    public boolean canMove(int dx, int dy) {
        float newX = character.getGridX() + dx;
        float newY = character.getGridY() + dy;
        return map.isWalkable((int) newX, (int) newY) && isNotBlockedByNPC(newX, newY);
    }

    public boolean isNotBlockedByNPC(float x, float y) {
        return Arrays.stream(npcManager.getNpcPositions())
                .noneMatch(npcPos -> Math.abs(npcPos[0] - x) < 0.5f && Math.abs(npcPos[1] - y) < 0.5f);
    }

    // Add a method to change maps safely
    public void changeMap(IsometricMap newMap, int startX, int startY) {
        this.map = newMap;

        // Ensure character is placed at a valid position on the new map
        if (isValidPosition(startX, startY)) {
            character.setPosition(startX, startY);
        } else {
            // Find a valid starting position if the provided one is invalid
            findValidStartPosition();
        }
    }

    private boolean isValidPosition(int x, int y) {
        if (map == null || map.getMapData() == null) return false;

        int[][] mapData = map.getMapData();
        if (mapData.length == 0) return false;

        if (x < 0 || y < 0 || y >= mapData.length || x >= mapData[0].length) {
            return false;
        }

        return mapData[y][x] != 0;
    }

    private void findValidStartPosition() {
        // Find the first walkable tile on the new map
        int[][] mapData = map.getMapData();
        for (int y = 0; y < mapData.length; y++) {
            for (int x = 0; x < mapData[y].length; x++) {
                if (mapData[y][x] != 0) {
                    character.setPosition(x, y);
                    return;
                }
            }
        }
        // If no walkable tile found, place at (0,0) as a last resort
        character.setPosition(0, 0);
    }


    public void moveCharacter(int dx, int dy) {


        if (!canMove(dx, dy)) {
            return; // Skip this move if it's invalid
        }

        float newX = character.getGridX() + dx;
        float newY = character.getGridY() + dy;

        character.moveToward(newX, newY);
//        character.setMoving(true);

        // Optional: Trigger a dialog when character reaches certain positions
        checkPositionEvents(newX, newY);
    }

    public float[] toIsometric(float x, float y) {
        float isoX = (x + y) * (map.getTileWidth() / 2.0f);
        float isoY = (y - x) * (map.getTileHeight() / 2.0f);
        return new float[]{isoX, isoY};
    }

    public boolean isCreated() {
        return isCreated;
    }

    public void setCreated(boolean created) {
        this.isCreated = created;
        if (created && characterCreationController != null && currentState != GameState.MAIN_MENU) {
            setState(GameState.CUTSCENE);
        }
    }

    // Add to GameController.java
    // In GameController.java, enhance resetGame method
    // In GameController.java - update the resetGame method
    public void resetGame() {
        // Reset character with a new instance
        character = new Character(20, 20);

        // Reset map with a new instance
        this.map = new IsometricMap();

        this.eventManager = null;
        this.eventManager = new EventManager(map);

        // Reset controllers to initial state - make sure to reset character creation controller
        if (characterCreationController == null) {
            characterCreationController = new CharacterCreation(this);
        }


        if (loadGameController == null) {
            loadGameController = new LoadGameController(this);
        }

        if (cutsceneController != null) {
            cutsceneController.dispose();
            cutsceneController = new CutsceneRenderer(this);
        }

        if (dialogController != null) {
            dialogController = new DialogController(this);
        }

        if (gameplayController != null) {
            gameplayController.dispose();
            gameplayController = new GameplayController(this);
        }


        // Reset to main menu state

        setState(GameState.MAIN_MENU);
        // Reset music
        musicController.playMusicForState(GameState.MAIN_MENU);

        System.gc(); // Request garbage collection

    }

    public LoadGameController getLoadGameController() {
        return loadGameController;
    }

    public void setLoadGameController(LoadGameController loadGameController) {
        this.loadGameController = loadGameController;
    }

    private String currentEventType;
    private int currentEventX;
    private int currentEventY;
    private boolean hasActiveEvent = false;
    private MapProperties properties;

    private void checkPositionEvents(float x, float y) {
        currentEvent = eventManager.checkPositionEvents(x, y);

        if (currentEvent != null) {
            hasActiveEvent = true;
            currentEventType = currentEvent.getEventType();
            currentEventX = currentEvent.getGridX();
            currentEventY = currentEvent.getGridY();
            properties = currentEvent.getProperties();
        } else {
            hasActiveEvent = false;
            properties = null;
        }
    }

    public void setEndEvent() {
        hasActiveEvent = false;
        currentEvent = null;
        properties = null;
    }

    public MapProperties getProperties() {
        return properties;
    }

    public MapEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(MapEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public void handleEventProperties(MapProperties properties, String event) {

        if (currentEventX != getCharacter().getGridX() || currentEventY != getCharacter().getGridY() || getCharacter().isMoving() == true) {
            return;
        }
        switch (event) {
            case "battle":

                int enemyId = 1; // Default to first enemy
                if (properties.containsKey("enemy")) {
                    Object enemyObj = properties.get("enemy");
                    if (enemyObj instanceof String) {
                        enemyId = Integer.parseInt((String) enemyObj);
                    } else if (enemyObj instanceof Integer) {
                        enemyId = (Integer) enemyObj;
                    }
                }
                if (eventManager.isEnemyDefeated(enemyId) &&
                        eventManager.getBooleanProperty(properties, "one_time", true)) {
                    eventManager.completeEvent(currentEvent.getId());

                } else {
                    Enemy enemy = EnemyLoader.getEnemyById(enemyId);
                    setState(GameState.GAMEPLAY);
                    gameplayController.activate();
                    gameplayController.startCombat(enemy);
                    gameplayController.setCurrentEvent(currentEvent);
                }
                break;
            case "treasure":
                if (currentEvent.isOneTime() && currentEvent.isCompleted()) {
                    return;
                }
                int itemId = -1;
                Object itemObj = properties.get("item");
                if (itemObj instanceof String) {
                    itemId = Integer.parseInt((String) itemObj);
                } else if (itemObj instanceof Integer) {
                    itemId = (Integer) itemObj;
                }
                int amount = properties.containsKey("amount") ? (Integer) properties.get("amount") : 1;
                if (itemId != -1) {
                    Items item = ItemLoader.getItemById(itemId);
                    openTreasureWithAnimation(item, amount, currentEventX, currentEventY);
                }
                break;
            case "dialog":
                if (properties != null) {
                    String arcId = properties.get("arc", String.class);
                    String sceneId = properties.get("scene", String.class);
                    this.dialogController.startDialog(arcId, sceneId);
                }
                break;
            case "quiz":
                if (getCharacter().getAttempFlags().get("quizAttempts") >= 1) {
                    dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                } else {
                    dialogController.setOnDialogFinishedAction(() -> startQuiz());
                    dialogController.startDialog("chapter_quiz_intro", "scene_meet_npc");
                    getCharacter().getAttempFlags().put("quizAttempts", 1);
                }
                break;
            case "mulquiz":
                if (getCharacter().getAttempFlags().get("mulQuizAttempts") >= 1) {
                    dialogController.showSimpleMessage("You have already attempted this quiz today. Come back tomorrow!");
                } else {
                    dialogController.setOnDialogFinishedAction(() -> startMulChoiceQuiz());
                    dialogController.startDialog("chapter_quiz_intro", "scene_meet_npc");
                    getCharacter().getAttempFlags().put("mulQuizAttempts", 1);
                }
                break;
            case "cutscene":
                String cutsceneName = properties.get("cutscene", String.class);
                if (cutsceneName != null) {
                    startCutscene(cutsceneName);
                }
                break;
        }

    }


    private void openTreasureWithAnimation(Items item, int amount, int x, int y) {
        // Get character position for effect placement
        float[] isoPos = toIsometric(x, y);

        effectManager.spawnEffectEvent("Star_Trail", 660, 390);

        // Create dialog message about the found item
        String message = "Bạn nhận được" + amount + " " + item.getItemName() + "!";
        dialogController.showSimpleMessage(message);

        // Add the item to inventory after a short delay
        dialogController.setOnDialogFinishedAction(() -> {
            character.addItem(item, amount);
            eventManager.completeEvent(currentEvent.getId());

        });
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    public boolean hasActiveEvent() {
        return hasActiveEvent && currentState == GameState.EXPLORING;
    }

    public String getCurrentEventType() {
        return currentEventType;
    }

    public int getCurrentEventX() {
        return currentEventX;
    }

    public int getCurrentEventY() {
        return currentEventY;
    }

    public BitmapFont getFont() {
        return font;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setFont(BitmapFont font) {
        this.font = font;
    }

    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    public void setPathfinder(Pathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }

    public Character getCharacter() {
        return character;
    }

    public IsometricMap getMap() {
        return map;
    }

    public InputController getInputController() {
        return inputController;
    }

    public QuestTrackerView getQuestTrackerView() {
        return questTrackerView;
    }

    public void setQuestTrackerView(QuestTrackerView questTrackerView) {
        this.questTrackerView = questTrackerView;
    }

    public DialogController getDialogController() {
        return dialogController;
    }

    public void setDialogController(DialogController dialogController) {
        this.dialogController = dialogController;
    }

    public MusicController getMusicController() {
        return musicController;
    }

    public PauseMenu getMenuController() {
        return pauseMenu;
    }

    public void setPreviousState(GameState previousState) {
        this.previousState = previousState;
    }

    public void setMenuController(PauseMenu pauseMenu) {
        this.pauseMenu = pauseMenu;
    }

    public SettingsMenu getSettingsMenuController() {
        return settingsMenu;
    }

    public MainMenu getMainMenuController() {
        return mainMenuController;
    }

    public void cycleTransitionType() {
        TransitionRenderer.TransitionType[] types = TransitionRenderer.TransitionType.values();
        int nextIndex = (transitionRenderer.getCurrentType().ordinal() + 1) % types.length;
        transitionRenderer.setTransitionType(types[nextIndex]);
        System.out.println("Changed transition to: " + types[nextIndex]);
    }

    public void dispose() {
        transitionRenderer.dispose();
        musicController.dispose();
        pauseMenu.dispose();
        settingsMenu.dispose();
        mainMenuController.dispose();
        characterCreationController.dispose();
        gameplayController.dispose();
        loadGameController.dispose();
        cutsceneController.dispose();
        effectManager.dispose();
        exploringUI.dispose();
        effectManager.dispose();
        musicController.dispose();
        merchantUI.dispose();
        dictionaryView.dispose();
        achievementUI.dispose();

        npcRenderer.dispose();
        npcManager.dispose();
        bountyBoardView.dispose();
        questTrackerView.dispose();
        bountyBoardController.dispose();
        if (quizController != null) {
            quizController.dispose();
        }


    }


    public QuizController getQuizController() {
        return quizController;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }

    public GameplayController getGameplayController() {
        return gameplayController;
    }

    public void setGameplayController(GameplayController gameplayController) {
        this.gameplayController = gameplayController;
    }

    public CharacterCreation getCharacterCreationController() {
        return characterCreationController;
    }

    public void setCharacterCreationController(CharacterCreation characterCreation) {
        this.characterCreationController = characterCreation;
    }

    public MerchantUI getMerchantUI() {
        return merchantUI;
    }

    public void setMerchantUI(MerchantUI merchantUI) {
        this.merchantUI = merchantUI;
    }

    public CharacterInfoDisplay getCharacterDisplay() {
        return characterDisplay;
    }

    public void setCharacterDisplay() {
        this.characterDisplay = new CharacterInfoDisplay(getCharacter());
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public boolean isLoadNPCs() {
        return isLoadNPCs;
    }

    public void setLoadNPCs(boolean loadNPCs) {
        isLoadNPCs = loadNPCs;
    }

    public ExploringUI getExploringUI() {
        return exploringUI;
    }

    public void setExploringUI(ExploringUI exploringUI) {
        this.exploringUI = exploringUI;
    }

    public void setInputController(InputController inputController) {
        this.inputController = inputController;
    }

    public InventoryUI getInventoryUI() {
        return inventoryUI;
    }

    public void setInventoryUI(InventoryUI inventoryUI) {
        this.inventoryUI = inventoryUI;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public NPCRenderer getNpcRenderer() {
        return npcRenderer;
    }

    public void setNpcRenderer(NPCRenderer npcRenderer) {
        this.npcRenderer = npcRenderer;
    }

    public void setEffectManager(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    public WordNetValidator getWordNetValidator() {
        return wordNetValidator;
    }

    public void setWordNetValidator(WordNetValidator wordNetValidator) {
        this.wordNetValidator = wordNetValidator;
    }

    public LevelUpNotification getLevelUpNotification() {
        return levelUpNotification;
    }

    public BountyBoardView getBountyBoardView() {
        return bountyBoardView;
    }

    public void setBountyBoardView(BountyBoardView bountyBoardView) {
        this.bountyBoardView = bountyBoardView;
    }

    /**
     * Show a level up notification when character levels up
     */
    public void showLevelUpNotification() {
        if (levelUpNotification != null && character != null) {
            levelUpNotification.showLevelUp(character.getLevel());
            // Play a sound effect if desired
            if (effectManager != null) {
                effectManager.playClickSound();
            }


        }
    }

}
