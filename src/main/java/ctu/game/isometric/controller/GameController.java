package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.gameplay.BoardEventManager;
import ctu.game.isometric.controller.gameplay.GameplayController;
import ctu.game.isometric.controller.quest.BountyBoardController;
import ctu.game.isometric.controller.quiz.MulChoiceQuizController;
import ctu.game.isometric.controller.quiz.QuizController;
import ctu.game.isometric.model.dictionary.Dictionary;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.entity.NPC;
import ctu.game.isometric.model.game.Dice;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.EnemyLoader;
import ctu.game.isometric.util.ItemLoader;
import ctu.game.isometric.util.WordNetValidator;
import ctu.game.isometric.view.menu.CharacterCreation;
import ctu.game.isometric.view.menu.MainMenu;
import ctu.game.isometric.view.menu.PauseMenu;
import ctu.game.isometric.view.menu.SettingsMenu;
import ctu.game.isometric.view.renderer.CutsceneRenderer;
import ctu.game.isometric.view.renderer.MapRenderer;
import ctu.game.isometric.view.renderer.NPCRenderer;
import ctu.game.isometric.view.renderer.TransitionRenderer;
import ctu.game.isometric.view.ui.*;
import ctu.game.isometric.view.view.BountyBoardView;
import ctu.game.isometric.view.view.CharacterInfoDisplay;
import ctu.game.isometric.view.view.QuestTrackerView;

import java.util.*;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class GameController {
    private IsometricGame game;
    private Character character;

    public IsometricGame getGame() {
        return game;
    }

    private AssetManager assetManager;

    private IsometricMap map;

    private Map<String, IsometricMap> mapList = new HashMap<>();
    private Map<String, EventManager> eventManagerMap = new HashMap<>();

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

    private BoardEventManager boardEventManager;

    private TutorialUI tutorialUI;

    private MapRenderer mapRenderer;


    Map<String, String> mainObjectiveDescriptions = new HashMap<>();

    public void createMainObjectiveDescriptions() {
        mainObjectiveDescriptions.put("intro", "Mình cần tìm đường rời khỏi khu rừng này trước tiên.");
        mainObjectiveDescriptions.put("forest_done", "Có vẻ như mình đã đến một ngôi làng nhỏ, mình nên khám phá xung quanh.");
        mainObjectiveDescriptions.put("god_intro", "Cleric Klein có thể giúp mình hiểu rõ hơn về thế giới này.");
        mainObjectiveDescriptions.put("klein_meet", "Nói chuyện với Cleric Klein\n");
        mainObjectiveDescriptions.put("dungeon_call", "Tiến đến hầm ngục thông qua cổng dịch chuyển theo lời chỉ dẫn của Cleric Klein.\n");
        mainObjectiveDescriptions.put("dungeon_entry", "Vượt qua hầm ngục và tìm hiểu bí mật của thế giới này.\nMục tiêu: tìm kiếm 3 viên ngọc và sống sót đến tầng cuối.");
    }


    public void startMazeCutScene() {
        Array<String> cutSceneSubtitles = new Array<>();
        cutSceneSubtitles.add("Từ thuở xa xưa, các Hiền Giả Ngôn Từ đã cùng nhau xây dựng một mê cung cổ đại – Căn Hầm Ký Ức. \nNơi đây lưu giữ tinh hoa tri thức, phép thuật của ngôn từ, và vô số thử thách để rèn luyện thế hệ kế tiếp.");
        cutSceneSubtitles.add("Mỗi tầng của mê cung là một thử thách được mã hóa bằng tiếng Anh – nơi từ vựng trở thành công cụ, ngữ cảnh là lưỡi kiếm, và tư duy là ngọn đèn dẫn đường.");
        cutSceneSubtitles.add("Thế nhưng… một biến cố xảy ra. Quỷ vương Azrok – thực thể đến từ Hư Vô – đã xâm nhập mê cung, làm vỡ cấu trúc tri thức, tha hóa các câu đố, và biến các cư dân tri thức thành sinh vật lạc lối.");
        cutSceneSubtitles.add("Từ đó, mê cung không còn là nơi rèn luyện, mà trở thành chiến trường.\n Những cạm bẫy ngôn ngữ, rương ký ức, và sinh vật ngôn từ bị bóp méo giờ hiện diện khắp nơi.");
        cutSceneSubtitles.add("Ba viên Ngọc Tri Thức – Ý Niệm, Biểu Đồ và Dòng Chảy – là phần lõi còn nguyên vẹn. \nChúng được canh giữ sâu trong tầng 4, 6 và 8. \n Nếu thu thập được, bạn có thể khôi phục Căn Hầm… hoặc đóng cánh cổng mãi mãi.");
        cutSceneSubtitles.add("Giờ đây, bạn là người được chọn để bước vào mê cung tri thức đã đổ vỡ ấy. Con đường không dễ đi… \nNhưng chính nơi đây, ngôn từ sẽ được tái sinh, và số phận thế giới sẽ được viết lại bằng chính câu chữ của bạn.");
        startMulBGSubTitleCutscene("maze", cutSceneSubtitles);
        addFlag("maze_cutscene");
    }

    public void showLoopDialogue() {
        dialogController.showSimpleMessage(Arrays.asList("Tại sao mình lại quay trở về khu rừng này? Mình đã đi qua đây rồi mà.",
                "Định mệnh khiến mình không thể quay về thế giới cũ dù cho có thần giúp đỡ",
                "Chẳng lẽ phải tiêu diệt Quỷ Vương Azrok mới có thể trở về?"));
        addFlag("loop_dialogue");
    }

    public void addFlag(String flag) {
        if (character.getFlags() == null) {
            character.setFlags(new ArrayList<>());
        }
        System.out.println("Flag added: " + flag);

        if (!character.getFlags().contains(flag)) {
            character.getFlags().add(flag);
            if (mainObjectiveDescriptions.containsKey(flag)) {
                character.setCurrentObject(mainObjectiveDescriptions.get(flag));
                if (exploringUI != null) {
                    exploringUI.updateQuest();
                }
            }
        }

    }

    BitmapFont font;
    BitmapFont titleFont;
    BitmapFont regularFont;
    BitmapFont commonFont;
    BitmapFont bigCommonFont;

    public GameController(IsometricGame game) {
        this.game = game;
        this.font = generateVietNameseFont("GrenzeGotisch.ttf", 35);
        this.titleFont = generateVietNameseFont("GrenzeGotisch.ttf", 50);
        this.regularFont = generateVietNameseFont("Roboto-Black.ttf", 16);
        this.commonFont = generateVietNameseFont("NovaSquare-Regular.ttf", 20);
        this.bigCommonFont = generateVietNameseFont("NovaSquare-Regular.ttf", 26);

        this.map = new IsometricMap();
        this.eventManager = new EventManager(map, "board");

        this.mapList.put("board", map);
        this.mapList.put("main", new IsometricMap("maps/main.tmx"));
        this.mapList.put("library", new IsometricMap("maps/library.tmx"));
        this.mapList.put("tavern", new IsometricMap("maps/tavern.tmx"));
        this.mapList.put("forest", new IsometricMap("maps/forest.tmx"));
        this.mapList.put("tower", new IsometricMap("maps/tower.tmx"));
        this.mapList.put("unknown", new IsometricMap("maps/unknown.tmx"));
        this.mapList.put("dungeon2", new IsometricMap("maps/dungeon2.tmx"));

        this.eventManagerMap.put("board", eventManager);
        this.eventManagerMap.put("main", new EventManager(this.mapList.get("main"), "main"));
        this.eventManagerMap.put("library", new EventManager(this.mapList.get("library"), "library"));
        this.eventManagerMap.put("tavern", new EventManager(this.mapList.get("tavern"), "tavern"));
        this.eventManagerMap.put("forest", new EventManager(this.mapList.get("forest"), "forest"));
        this.eventManagerMap.put("tower", new EventManager(this.mapList.get("tower"), "tower"));
        this.eventManagerMap.put("dungeon2", new EventManager(this.mapList.get("dungeon2"), "dungeon2"));
        this.eventManagerMap.put("unknown", new EventManager(this.mapList.get("unknown"), "unknown"));

        this.character = new Character(10, 0);
        this.inputController = new InputController(this);
        this.dialogController = new DialogController(this);
        this.musicController = new MusicController();
        characterCreationController = new CharacterCreation(this);
        this.pauseMenu = new PauseMenu(this, titleFont, font);


        effectManager = new EffectManager();
        this.loadEffects();
        this.settingsMenu = new SettingsMenu(this, titleFont, font);
        this.mainMenuController = new MainMenu(this, font);
        this.transitionRenderer = new TransitionRenderer();
        this.cutsceneController = new CutsceneRenderer(this);
        loadGameController = new LoadGameController(this);
//        this.wordValidator.loadDictionary();
        this.wordNetValidator = new WordNetValidator();
        this.wordNetValidator.loadDictionary();
        this.assetManager = game.getAssetManager();

        this.gameplayController = new GameplayController(this);
        this.quizController = new QuizController(this);
        this.mulChoiceQuizController = new MulChoiceQuizController(this);

        initializeDictionary();
        this.musicController.initialize();
        this.musicController.playMusicForState(GameState.MAIN_MENU);
        inputController.setEffectManager(effectManager);

        achievementManager = new AchievementManager(this);
        achievementUI = new AchievementUI(achievementManager, font, regularFont);
        this.currentPlayTime = new Date();

        // Initialize the level up notification
        this.levelUpNotification = new LevelUpNotification(this);

        this.pathfinder = new Pathfinder(map);
        npcManager = new NPCManager(this);

        bountyBoardController = new BountyBoardController(this);
        bountyBoardView = new BountyBoardView(bountyBoardController, font, commonFont);
        questTrackerView = new QuestTrackerView(this);

        tutorialUI = new TutorialUI(this);
        subtitles = new Array<>();

        subtitles.add("Mình chỉ đang tìm tài liệu cho bài luận văn thôi...");
        subtitles.add("Nhưng cuốn sách này... không tiêu đề, đầy bụi... có gì đó thu hút mình.");
        subtitles.add("Khoảnh khắc chạm vào trang giấy, thế giới xung quanh dường như biến mất.");
        subtitles.add("Mình... đang bị kéo đi...");
        subtitles.add("Mình đang ở đâu...? Đây không phải là thư viện... Cũng không phải là mơ.");
        createMainObjectiveDescriptions();
    }

    Array<String> subtitles;

    public void createBoard() {
        boardEventManager = new BoardEventManager(this);

    }

    public IsometricMap changeMap(String mapName) {

        IsometricMap newMap = this.mapList.get(mapName);

        if (newMap != null) {
            System.out.println("Changing map to: " + mapName);
            transitionRenderer.startLoadingScreen(() -> {

                if (mapName.equalsIgnoreCase("board")) {
                    newMap.generateRandomMaze(getCharacter().getRun() / 2);
                    isNewRun = false;
                    if (!character.isTutorialCompleted("maze")) {
                        tutorialUI.show("maze");
                        character.setTutorialCompleted("maze");
                        if (!character.getFlags().contains("dungeon_entry"))
                            addFlag("dungeon_entry");
                    }

                }

                if (mapName.equals("dungeon2")) {
                    if (!character.getFlags().contains("dungeon2_entry")) {

                        dialogController.showSimpleMessage(Arrays.asList(
                                        "Ngươi bước vào một không gian nơi thời gian dường như ngừng trôi.",
                                        "Những bức tường đá cũ kỹ, rạn nứt bao phủ bởi bóng tối. Không gian tĩnh lặng đến mức có thể nghe thấy tiếng tim đập.\n Đây không phải chỉ là một lăng mộ – nó là một nơi bị lãng quên, nơi ký ức và hy sinh hòa quyện thành một thể.",
                                        "Ngươi cảm nhận được sự hiện diện của những linh hồn đã từng sống, những người đã để lại dấu ấn của mình trong không gian này. \nMỗi bước đi trên nền đất lạnh lẽo là một bước vào quá khứ, nơi những câu chuyện chưa kể đang chờ đợi được khám phá.",
                                        "[Bạn đã bước vào Dungeon: Lăng Mộ Ký Ức. Các khu vực chính: Thư viện, Nơi Thờ, Lăng Mộ Chính, và Khu vực Bị Cháy.]"
                                )
                        );
                        addFlag("dungeon2_entry");
                    }
                }

                this.map = newMap;
                this.character.setGameMap(map);
                this.pathfinder.setMap(newMap);
                if (mapName.equals("board")) {
                    boardEventManager.setMap(this.map);
                    boardEventManager.randomBoardEveryRun();
                }
                this.eventManager = this.eventManagerMap.get(mapName);
                this.game.getGameScreen().getMapRenderer().changeTiledMapRenderer(this.map, this.eventManager);
            });

            return newMap;
        } else {
            Gdx.app.error("GameController", "Map not found: " + mapName);
            return null;
        }
    }

    public IsometricMap changeMapInVillage(String mapName) {

        IsometricMap newMap = this.mapList.get("main");

        if (newMap != null) {
            transitionRenderer.startLoadingScreen(() -> {

                this.map = newMap;
                this.character.setGameMap(map);

                switch (mapName) {
                    case "tavern":
                        this.character.setPosition(28, 9);
                        break;
                    case "library":
                        this.character.setPosition(35, 19);
                        break;
                    case "main":
                        this.character.setPosition(31, 15);
                        break;
                    case "tower":
                        this.character.setPosition(28, 36);
                        break;
                    case "unknown":
                        this.character.setPosition(28, 22);
                        break;
                    case "forest":

                        this.character.setPosition(10, 14);
                        break;
                }
                this.pathfinder.setMap(newMap);
                this.eventManager = this.eventManagerMap.get("main");
                this.game.getGameScreen().getMapRenderer().changeTiledMapRenderer(this.map, this.eventManager);
            });

            return newMap;
        } else {
            Gdx.app.error("GameController", "Map not found: " + mapName);
            return null;
        }
    }

    public void changeSaveMap(String mapName) {

        IsometricMap newMap = this.mapList.get(mapName);
        if (newMap != null) {
            System.out.println("Changing map to: " + mapName);

            if (mapName.equalsIgnoreCase("board")) {
                newMap.generateRandomMaze(getCharacter().getRun() / 2);
                System.out.println("New run generated with run level: " + getCharacter().getRun());
                isNewRun = false;
            }


            this.map = newMap;
            boardEventManager.randomBoardEveryRun();
            this.character.setGameMap2(map);
            if (mapName.equals("forest")) {
                this.character.setPosition(10, 14);
            }
            this.pathfinder.setMap(newMap);
            this.eventManager = this.eventManagerMap.get(mapName);
            this.game.getGameScreen().getMapRenderer().changeTiledMapRenderer(this.map, this.eventManager);

        }
    }

    public void returnToTower(String enemyName) {
        IsometricMap newMap = this.mapList.get("tower");

        if (newMap != null) {
            this.map = newMap;
            this.character.setGameMap(map);
            this.character.setPosition(5, 7);
            this.pathfinder.setMap(newMap);
            this.eventManager = this.eventManagerMap.get("tower");
            this.game.getGameScreen().getMapRenderer().changeTiledMapRenderer(this.map, this.eventManager);

            if (enemyName.contains("Boss") || enemyName.contains("Lord"))
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        character.deRun();
                        dialogController.showSimpleMessage("Mình đã thất bại, may mà Cleric Klein đã cứu mình.\n Nhưng mình đã bị đẩy lùi về 1 tầng");
                    }
                }, 1.5f);
            else
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        dialogController.showSimpleMessage("Mình đã thất bại, may mà Cleric Klein đã cứu mình.\n");
                    }
                }, 1.5f);

        } else {
            Gdx.app.error("GameController", "Tower map not found.");
        }
    }

    public void returnToTowerAfterBoss(String BossName) {
        IsometricMap newMap = this.mapList.get("tower");

        if (newMap != null) {
            this.map = newMap;
            this.character.setGameMap(map);
            this.character.setPosition(5, 7);
            this.pathfinder.setMap(newMap);
            this.eventManager = this.eventManagerMap.get("tower");
            this.game.getGameScreen().getMapRenderer().changeTiledMapRenderer(this.map, this.eventManager);


            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    dialogController.showSimpleMessage(Arrays.asList(
                            "Chúc mừng ngươi đã đánh bại " + BossName + ".",
                            "Hãy tận dụng phần thưởng từ chiến thắng này để tiếp tục hành trình."
                    ));
                }
            }, 1.5f);

        } else {
            Gdx.app.error("GameController", "Tower map not found.");
        }
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

    public void completedDungeon2() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                dialogController.showSimpleMessage(Arrays.asList("Ngươi rất xứng đáng, bây giờ ta có thể an nghĩ được rồi.",
                        "Ta và chủ nhân cũ của ta từng cố gắng vượt qua đại mê cung nhưng thất bại.",
                        "Hi vọng dựa vào quyển nhật ký của chủ nhân, ngươi có thể tìm ra sự thật về thế giới này.",
                        "Và giờ thì tạm biệt, người xa xứ...."));
                npcManager.removeNPC(9);
                addFlag("completed_dungeon2");
            }
        }, 2f);
    }

    public void defeatedFrostGuardian() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                dialogController.showSimpleMessage(Arrays.asList(
                        "Ngươi đã chứng minh bản lĩnh và vượt qua thử thách của ta.",
                        "Phần thưởng dành cho ngươi đang nằm trong chiếc rương bên cạnh. Hãy nhận lấy, nó sẽ giúp ngươi đối đầu Quỷ Vương.",
                        "Nhớ kỹ, Quỷ Vương là bậc thầy về ngôn ngữ. Hắn có thể vô hiệu hóa mọi sát thương dưới 15. Hãy chuẩn bị thật tốt trước khi đối mặt với hắn.",
                        "Đường về nằm ở gần trại lửa, nó có sức mạnh đưa ngươi trở về nơi ngươi đã đến.",
                        "Giờ đây nhiệm vụ của ta đã hoàn thành. Chúc ngươi thành công trên hành trình phía trước."
                ));
                npcManager.removeNPC(10);
                addFlag("frost_guardian_defeated");
            }
        }, 2f);
    }

    public void showNPCBackStory() {
        NPC npc = findNPCNear(character.getGridX(), character.getGridY());

        if (npc != null) {
            npc.setBehaviorState("Dialogue");
            switch (npc.getNpcName()) {
                case "Upgrader Smith":
                    if (character.getScore() < 150) {
                        dialogController.showSimpleMessage(Arrays.asList("Ngươi cần ít nhất 150 điểm để nâng cấp trang bị của mình.",
                                "Hãy hoàn thành các nhiệm vụ hoặc đánh bại quái vật để kiếm điểm."));
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            boolean isDouble = character.upgradeItem("Elixir", 50);
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    if (isDouble) {
                                        dialogController.showSimpleMessage("Nhận 1 `Big Elixir` từ Smith tốn 50 vàng và 1 bình Elixir.");
                                    } else {
                                        dialogController.showSimpleMessage("Nhận 1 `Big Elixir` từ Smith tốn 200 vàng.");
                                    }
                                }
                            }, 1.5f);
                        });
                        dialogController.setOnCanncelFinishedAction(() -> {
                            boolean isDouble = character.upgradeItem("Arcane Essence", 50);
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    if (isDouble) {
                                        dialogController.showSimpleMessage("Nhận 1 `Big Arcane Essence` từ Smith tốn 50 vàng và 1 bình Arcane Essence.");
                                    } else {
                                        dialogController.showSimpleMessage("Nhận 1 `Big Arcane Essence` từ Smith tốn 200 vàng.");
                                    }
                                }
                            }, 1.5f);
                        });

                        dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    }
                    break;


                case "WordSeer Kang":
                    if (character.getLevel() < 3 && character.getRun() < 3) {
                        dialogController.showSimpleMessage("Ngươi chưa đủ tri thức để tham gia `Bài học ngữ nghĩa` của ta.\n" +
                                "Hãy quay lại khi ngươi đã đạt cấp độ 3 hoặc đã hoàn thành ít nhất phải vượt qua tầng 3 của đại Mê cung.");
                        break;
                    }
                    if (getCharacter().getAttempFlags().get("quizAttempts") >= 3) {
                        dialogController.showSimpleMessage("Ngươi đã tham gia bài kiểm tra này hôm nay rồi. Hãy quay lại vào ngày mai! [ 3/3 ]");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startQuiz(5);
                            getCharacter().getAttempFlags().put("quizAttempts", 1);
                        });
                        dialogController.startDialog(npc.getArcId(), npc.getSceneId());
                    }
                    break;
                case "Lorekeeper Tian":
                    if (character.getLevel() < 3 && character.getRun() < 3) {
                        dialogController.showSimpleMessage("Ngươi chưa đủ tri thức để tham gia `Bài học từ vựng` của ta.\n" +
                                "Hãy quay lại khi ngươi đã đạt cấp độ 3 hoặc đã hoàn thành ít nhất phải vượt qua tầng 3 của đại Mê cung.");
                        break;
                    }
                    if (getCharacter().getAttempFlags().get("mulQuizAttempts") >= 3) {
                        dialogController.showSimpleMessage("Ngươi đã tham gia bài kiểm tra này hôm nay rồi. Hãy quay lại vào ngày mai! [ 3/3 ]");
                    } else {
                        dialogController.setOnDialogFinishedAction(() -> {
                            startMulChoiceQuiz(5);
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
                    break;
                case "Cleric Klein":
                    if (!getCharacter().getFlags().contains("klein_meet")) {
                        dialogController.setOnDialogFinishedAction(() -> {
                            Enemy enemy = EnemyLoader.getEnemyById(1);

                            setState(GameState.GAMEPLAY);

                            if (!character.isTutorialCompleted("combat")) {
                                tutorialUI.show("combat");
                                character.setTutorialCompleted("combat");
                            }
                            gameplayController.activate();
                            gameplayController.startCombat(enemy);
                        });

                        dialogController.setOnCanncelFinishedAction(() -> {
                            if (character.getAttempFlags().get("loop") >= 1)
                                Timer.schedule(new Timer.Task() {
                                    @Override
                                    public void run() {
                                        dialogController.showSimpleMessage("Mình không nên từ chối vận mệnh này.\n Hãy bắt đầu cuộc hành trình của mình với Cleric Klein.");
                                        character.getFlags().remove("klein_meet");
                                    }
                                }, 1f);
                            else
                                startLoopEvent();
                        });

                        dialogController.startDialog("klein_meet", "scene_meet_cleric");
                        addFlag("klein_meet");
                        break;
                    } else if (getCharacter().getFlags().contains("klein_meet") && !getCharacter().getFlags().contains("dungeon_call")) {
                        dialogController.setOnDialogFinishedAction(() -> {
                            addFlag("dungeon_call");

                            character.addItem(ItemLoader.getItemById(1), 2);

                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    dialogController.startDialog("dungeon_entry", "scene_dungeon_overview");
                                }
                            }, 1f);
                        });
                        dialogController.startDialog("dungeon_call", "scene_prepare_for_dungeon");
                        break;
                    } else {
                        dialogController.showSimpleMessage("Nếu ngươi muốn biết thêm thông tin về hầm ngục, hãy đọc quyển sách trên tủ sách bên cạnh tấm thảm kia.");
                        break;
                    }
                case "Frost Guardian":
                    dialogController.setOnDialogFinishedAction(() -> {
                        if (character.getDefend() >= 30) {
                            defeatedFrostGuardian();
                        } else {
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    Enemy enemy = new Enemy(11, "Thủ vệ hồ", "Frost Guardian", "frost_guardian", 1, 25, 16);
                                    enemy.setDefensePower(21);
                                    game.getDarkestDungeonScreen().startCombat(enemy);
                                    game.changeScreen("DARK_DUNGEON");
                                }
                            }, 1f);

                        }
                    });
                    dialogController.setOnCanncelFinishedAction(() -> {
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                Enemy enemy = new Enemy(11, "Thủ vệ hồ", "Frost Guardian", "frost_guardian", 1, 21, 16);
                                enemy.setDefensePower(25);
                                game.getDarkestDungeonScreen().startCombat(enemy);
                                game.changeScreen("DARK_DUNGEON");
                            }
                        }, 1f);
                    });
                    dialogController.startDialog("frostguardian_ending", "scene_intro");
                    break;
                case "":
                case "Armon":
                    if (character.getFlags().contains("completed_ghost_quest")) {
                        dialogController.setOnDialogFinishedAction(() -> {
                            if (character.getDamage() >= 20) {
                                completedDungeon2();
                            } else {
                                Timer.schedule(new Timer.Task() {
                                    @Override
                                    public void run() {
                                        Enemy enemy = new Enemy(11, "Flame Slime Guardian", "Flame Guardian Armon", "Demon", 1, 11, 16);
                                        enemy.setDefensePower(15);
                                        game.getDarkestDungeonScreen().startCombat(enemy);
                                        game.changeScreen("DARK_DUNGEON");
                                    }
                                }, 1f);

                            }
                        });
                        dialogController.setOnCanncelFinishedAction(() -> {
//
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    Enemy enemy = new Enemy(11, "Flame Slime Guardian", "Flame Guardian Armon", "Demon", 1, 15, 17);
                                    enemy.setDefensePower(11);
                                    game.getDarkestDungeonScreen().startCombat(enemy);
                                    game.changeScreen("DARK_DUNGEON");
                                }
                            }, 1f);
                        });
                        dialogController.startDialog("armon_ending", "scene_intro");
                    } else if (character.getFlags().contains("ghost_name") && character.getFlags().contains("acient_note") && character.getFlags().contains("ghost_ashes")) {
                        dialogController.showMessageWithChoices("Ngươi đã lấy được **mảnh vỡ của bia đá rồi.\n" +
                                        "Hãy tiến đến nơi thờ và ta sẽ giúp ngươi hoàn thành nghi thức", "Được [YES]", "Chờ chút [NO]",
                                () -> {
                                    mapRenderer.moveCameraToTarget(1312, -176, 0.5f, 2.5f, 1.5f, 1.0f);
                                    Timer.schedule(new Timer.Task() {
                                        @Override
                                        public void run() {
                                            dialogController.showSimpleMessage("Hãy tìm đúng vị trí thực hiện nghi thức. Ta đã mở phong ấn chỗ đấy rồi");
                                            npcManager.changeNPCPosition(npc.getNpcID(), 11, 13);
                                            map.setTileWalkable(27, 14, true);
                                            addFlag("completed_ghost_quest");
                                        }
                                    }, 1.8f);
                                }, () -> {

                                });
                    } else if (character.getFlags().contains("ghost_name") && character.getFlags().contains("acient_note")) {
                        dialogController.setOnDialogFinishedAction(() -> {
                            mapRenderer.moveCameraToTarget(1248, 272, 0.5f, 2.5f, 1.5f, 1.0f);
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    dialogController.showSimpleMessage("Cơ quan mà Armon nói có lẽ nằm đâu đó ở đây");
                                }
                            }, 1.8f);
                        });
                        dialogController.startDialog("armon_helping", "scene_intro");
                    } else if (character.getFlags().contains("ghost_name")) {
                        npc.setNpcName("Armon");
                        dialogController.showSimpleMessage(Arrays.asList(
                                " Ta nhớ rồi... Ta từng là hộ vệ của chủ nhân lăng mộ. Nhưng ta không thể vào sâu hơn – ta chỉ là một linh hồn bị ràng buộc ở đây. \nNgươi... ngươi là người duy nhất có thể mở cánh cửa lăng mộ chính.",
                                "*** Hồn ma kể rằng sâu trong lăng mộ có nhật ký của chủ nhân, nơi ghi lại toàn bộ sự thật về thế giới này.\n" +
                                        " Tuy nhiên, để mở được cánh cửa của lăng mộ chính, cần thực hiện một nghi thức đặc biệt. ***",
                                "Ta không thể vào sâu hơn. Nhưng ngươi có thể. Hãy lại gần cánh cửa lăng mộ – ta cảm giác có điều gì đó ở đó có thể giúp ngươi.\n",
                                "Nếu tìm được manh mối, hãy quay trở lại tìm ta"
                        ));
                    } else {
                        dialogController.showSimpleMessage("Một linh hồn xa lạ đang đứng trước mặt mình, nó luon ông miệng nói `Tên của ta.. tên..là..gì`.\n"
                                + "Ngôi nhà bên cạnh và lăng mộ phía sau nó là của ai?.");
                    }
                    break;
                default:
                    break;
            }


        } else {
            dialogController.showSimpleMessage("Ta không thấy ai ở gần đây cả.\n" +
                    "Có lẽ ta nên đi tìm một người nào đó để trò chuyện.");
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
            if (isNPCNear(npc, x, y)) {
                return npc; // Return the first NPC found in range
            }
        }
        return null; // No NPC found in range
    }

    private boolean isNPCNear(NPC npc, float x, float y) {
        return Math.abs(npc.getXPosition() - x) <= 1f &&
                Math.abs(npc.getYPosition() - y) <= 1f &&
                npc.getMapName().equalsIgnoreCase(map.getMapName());
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

    public void hideTutorial() {
        tutorialUI.hide();
    }

    public BountyBoardController getBountyBoardController() {
        return bountyBoardController;
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


    public void loadEffects() {
        effectManager.loadEffect("Starlight", "effects/Starlight/");
        effectManager.loadEffect("Star_Trail", "effects/Star_Trail/");
    }

    public Map<String, EventManager> getEventManagerMap() {
        return eventManagerMap;
    }

    public Map<String, IsometricMap> getMapList() {
        return mapList;
    }


    public void loadCharacter(Character character, Date lastSaveTime) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

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

    }


    public void update(float delta) {

        switch (currentState) {
            case EXPLORING:
                if (dialogController.isDialogActive() && !dialogController.shouldRenderBackground()) {

                    effectManager.update(delta);
                    npcRenderer.update(delta);
                } else if (!merchantUI.isVisible()) {
                    inputController.updateCooldown(delta);
                    checkingCharacterPos(character);
                    character.update(delta);
                    updateFireBurn(delta);
                    npcRenderer.update(delta);
                }

                getBoardEventManager().getWordScrambleGame().update(delta);

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

                cutsceneController.update(delta);
                break;

        }

    }


    public void updateFireBurn(float delta) {
        if (inFireArea) {
            fireAreaTimer += delta;
            if (fireAreaTimer >= 5f) {
                if (character.getGridX() > 1 && character.getGridX() < 5 &&
                        character.getGridY() > 8 && character.getGridY() < 13) {
                    character.decreaseHealth(2);
                    dialogController.showSimpleMessage("Ngọn lửa đang thiêu đốt bạn! -2 HP");
                    if (character.getHealth() <= 1) {
                        dialogController.showSimpleMessage("Linh hồn mình được ai đó bảo vệ sao? Tại sao mình chưa bị thiêu rụi hoàn toàn?");
                        inFireArea = false;
                    }
                } else {
                    inFireArea = false; // Character left the area
                }
                fireAreaTimer = 0f; // Reset timer
            }
        }
    }

    public Array<String> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(Array<String> subtitles) {
        this.subtitles = subtitles;
    }

    public void startMulChoiceQuiz(int numQuestions) {
        setPreviousState(currentState);
        setState(GameState.MULTIPLE_CHOICE_QUIZZES);
        mulChoiceQuizController.startQuiz(numQuestions);
    }

    public void startQuiz(int numQuestions) {
        setPreviousState(currentState);
        setState(GameState.QUIZZES);
        quizController.startQuiz(numQuestions);
    }

    public MulChoiceQuizController getMultipleChoiceQuizController() {
        return mulChoiceQuizController;
    }

    public TransitionRenderer getTransitionController() {
        return transitionRenderer;
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

    public void startCutscene(String cutsceneName) {
        setPreviousState(currentState);
        setState(GameState.CUTSCENE);
        cutsceneController.loadCutscene(cutsceneName);
        addFlag(cutsceneName);
    }

    public void startSubTitleCutscene(String cutsceneName, Array<String> subtitles) {
        setPreviousState(currentState);
        setState(GameState.CUTSCENE);
        cutsceneController.loadBackgroundCutscene(cutsceneName, subtitles);
        addFlag(cutsceneName);
    }

    public void startMulBGSubTitleCutscene(String cutsceneName, Array<String> subtitles) {
        setPreviousState(currentState);
        setState(GameState.CUTSCENE);
        cutsceneController.loadOctopathStyleCutscene(cutsceneName, subtitles);
        addFlag(cutsceneName);
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
        if (map.getMapName().equals("board")) return map.isWalkable((int) newX, (int) newY);
        else return map.isWalkable((int) newX, (int) newY) && isNotBlockedByNPC(newX, newY);
    }

    public boolean isNotBlockedByNPC(float x, float y) {
        return Arrays.stream(npcManager.getNpcPositions())
                .noneMatch(npcPos -> Math.abs(npcPos[0] - x) < 0.5f && Math.abs(npcPos[1] - y) < 0.5f);
    }


    public void moveCharacter(int dx, int dy) {
        if (!canMove(dx, dy)) {
            return; // Skip this move if it's invalid
        }
        if (map.getMapName().equals("forest")) {
            if (!character.getFlags().contains("forest_info")) {
                dialogController.showSimpleMessage(Arrays.asList("Mình đang ở đâu? Đây không phải là thế giới của mình...",
                        "Giọng nói trong đầu mình trước đó là ai.",
                        "Trước tiên mình cần phải tìm đường rời khỏi đây đã."));
                addFlag("forest_info");
                return;
            } else if (character.getFlags().contains("loop") && !character.getFlags().contains("loop_dialogue")) {
                showLoopDialogue();
                addFlag("loop_dialogue");
            }
        }

        float newX = character.getGridX() + dx;
        float newY = character.getGridY() + dy;

        character.moveToward(newX, newY);


        // Optional: Trigger a dialog when character reaches certain positions
        if (eventManager.getMapName().equals("board"))
            this.boardEventManager.checkBoardPlayerPosition((int) newX, (int) newY);

        checkPositionEvents(newX, newY);
    }

    private Set<String> activeEvents = new HashSet<>();

    public void checkForestEvents(float x, float y) {
        String positionKey = "forest_" + (int) x + "_" + (int) y;

        // Prevent duplicate event triggers
        if (activeEvents.contains(positionKey)) {
            return;
        }

        if ((x == 0 && y == 14) || (x == 0 && y == 13)) {
            activeEvents.add(positionKey);
            character.clearPath();
            dialogController.showMessageWithChoices(
                    "Tiến về ngôi làng phía trước", "Đi tiếp [YES]", "Dừng lại [NO]",
                    () -> {
                        changeMap("main");
                        character.setPosition(15, 15);
                        addFlag("forest_done");
                    }, () -> {
                        character.setPosition(x + 1, y);
                    }
            );
            activeEvents.remove(positionKey); // Ensure removal after processing
        }
    }

    private boolean inFireArea = false;
    private float fireAreaTimer = 0f;

    public void checkDungeonEvents(float x, float y) {
        String positionKey = "dungeon_" + (int) x + "_" + (int) y;

        // Prevent duplicate event triggers
        if (activeEvents.contains(positionKey)) {
            return;
        }

        if (x > 4 && y > 20 && !character.getFlags().contains("dungeon2_library_entry")) {
            activeEvents.add(positionKey);
            character.clearPath();
            dialogController.showSimpleMessage(Arrays.asList(
                    "*** Những giá sách phủ đầy bụi, các cuốn sách mục nát như thể ký ức của chúng đã bị xóa sạch. \nMột không gian từng là nơi lưu giữ tri thức giờ đây chỉ còn là một bóng ma của quá khứ. ***",
                    "Những cuốn sách này... không có gì cả. Ai đã xóa đi ký ức của chúng? Và tại sao?"
            ));
            addFlag("dungeon2_library_entry");
            activeEvents.remove(positionKey); // Ensure removal after processing
        } else if (x > 1 && x < 5 && y > 8 && y <= 13) {
            if (!character.getFlags().contains("dungeon2_fire_entry")) {
                activeEvents.add(positionKey);
                character.clearPath();
                dialogController.showSimpleMessage(Arrays.asList(
                        "*** Mặt đất phủ đầy tro tàn, những bức tường ám khói đen.\n Đây là nơi đã chứng kiến một trận chiến khốc liệt, nơi mà sự sống và cái chết giao thoa, để lại dấu tích không bao giờ phai mờ.***",
                        "Khu vực này... đã bị phá hủy hoàn toàn. Lửa cháy khắp nơi, và mọi thứ đều bị thiêu rụi.",
                        "Mình phải tìm thấy `vật tàn dư` mà linh hồn đó đã nói và nhanh rời khỏi nơi này."
                ));
                addFlag("dungeon2_fire_entry");

                // Start fire area effect
                inFireArea = true;
                fireAreaTimer = 0f;
            }
        } else if (x > 15 && y > 17 && !character.getFlags().contains("dungeon2_tho_entry")) {
            activeEvents.add(positionKey);
            character.clearPath();
            dialogController.showSimpleMessage(Arrays.asList(
                    "*** Nhân vật quay lại nơi thờ phụng, nơi một hồn ma vô danh đứng lặng lẽ trước bàn thờ.\n Nó không hề tấn công, nhưng sự hiện diện của nó tạo ra một cảm giác nặng nề***",
                    "Linh hồn vô danh đứng đó, đôi mắt trống rỗng như thể đang nhìn xuyên qua thời gian.\n Nó thì thầm những lời không rõ nghĩa, như một đoạn ký ức bị đứt đoạn.."
            ));
            addFlag("dungeon2_tho_entry");
            activeEvents.remove(positionKey); // Ensure removal after processing
        }
    }

    private void checkEvents(String mapName, int x, int y) {
        if (mapName.equals("forest")) {
            checkForestEvents(x, y);
        } else if (mapName.equals("main")) {
            checkMainEvents(x, y);
        } else if (mapName.equals("dungeon2")) {
            checkDungeonEvents(x, y);
        }
    }


    public void startLoopEvent() {
        changeSaveMap("forest");

        character.loopIncrease();
        addFlag("loop");
        character.getFlags().remove("loop_dialogue");
        character.getFlags().remove("klein_meet");
    }

    public void checkMainEvents(float x, float y) {
        String positionKey = "main_" + (int) x + "_" + (int) y;

        // Prevent duplicate event triggers
        if (activeEvents.contains(positionKey)) {
            return;
        }

        if ((x == 31 && y == 4) || (x == 32 && y == 4)) {
            if (!character.getFlags().contains("god_intro")) {
                activeEvents.add(positionKey);
                character.clearPath();
                dialogController.setOnDialogFinishedAction(() -> {
                    mapRenderer.moveCameraToTarget(2048, 128, 0.5f, 2.5f, 1.5f, 1.0f);
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            dialogController.showSimpleMessage("Có lẽ đó là tòa tháp của Cleric mà vị thần nhắc tới");

                        }
                    }, 1.8f);
                });
                dialogController.startDialog("god_intro", "scene_01");
                addFlag("god_intro");
                character.setPosition(31, 5);
                activeEvents.remove(positionKey); // Ensure removal after processing
            } else if (character.getFlags().contains("god_intro") && character.getFlags().contains("loop_dialogue") && !character.getFlags().contains("god_dialogue")) {
                activeEvents.add(positionKey);
                character.clearPath();
                dialogController.startDialog("god_dialogue", "scene_01");
                addFlag("god_dialogue");
                activeEvents.remove(positionKey); // Ensure removal after processing
            }

        }
    }


    public void checkingCharacterPos(Character character) {
        int x = (int) character.getGridX();
        int y = (int) character.getGridY();
        checkEvents(map.getMapName(), x, y);
    }

    public void moveCharacterAlongPath(int targetX, int targetY) {
        float startX = character.getGridX();
        float startY = character.getGridY();

        if (map.getMapName().equals("forest")) {
            if (!character.getFlags().contains("forest_info")) {
                dialogController.showSimpleMessage(Arrays.asList("Mình đang ở đâu? Đây không phải là thế giới của mình...",
                        "Giọng nói trong đầu mình trước đó là ai.",
                        "Trước tiên mình cần phải tìm đường rời khỏi đây đã."));
                addFlag("forest_info");
                return;
            } else if (character.getFlags().contains("loop") && !character.getFlags().contains("loop_dialogue")) {
                showLoopDialogue();
                addFlag("loop_dialogue");
            }
        }

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

            if (eventManager.getMapName().equals("board"))
                checkPositionEvents((int) targetX, (int) targetY);
//

        }
    }

    public void disposeSome() {
        if (!isCreated)
            return;

        setCharacterCreationController(null);
        setLoadGameController(null);
    }

    public boolean isCreated() {
        return isCreated;
    }

    public void setCreated(boolean created) {
        this.isCreated = created;

        if (created && characterCreationController != null && currentState != GameState.MAIN_MENU) {

            setState(GameState.CUTSCENE);
            if (!character.getFlags().contains("intro")) {
                startMulBGSubTitleCutscene("intro", subtitles);
                character.setDirection("knocked_down");
                if (!character.isTutorialCompleted("movement")) {
                    tutorialUI.show("movement");
                    character.setTutorialCompleted("movement");
                }
            }

        }
    }

    public void resetEventsManager() {
        eventManagerMap.get("board").resetEvents(mapList.get("board"));
        eventManagerMap.get("main").resetEvents(mapList.get("main"));
        eventManagerMap.get("library").resetEvents(mapList.get("library"));
        eventManagerMap.get("tavern").resetEvents(mapList.get("tavern"));
        eventManagerMap.get("forest").resetEvents(mapList.get("forest"));
        eventManagerMap.get("tower").resetEvents(mapList.get("tower"));
        eventManagerMap.get("dungeon2").resetEvents(mapList.get("dungeon2"));
    }

    public void resetGame() {
        // Reset character with a new instance
        character = new Character(10, 14);
        checkPositionEvents(0, 0); // Reset current event

        isRenderCharacter = true;

        resetEventsManager();
        npcManager.resetNPCManager();

        // Reset controllers to initial state - make sure to reset character creation controller
        characterCreationController = null;
        characterCreationController = new CharacterCreation(this);

        trapUnlock.clear();

        loadGameController = null;
        loadGameController = new LoadGameController(this);

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
        if (merchantUI != null) {
            merchantUI.dispose();
        }

        transitionRenderer = new TransitionRenderer();

        createBoard();
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
    boolean isRenderCharacter = true;

    public boolean isRenderCharacter() {
        return isRenderCharacter;
    }

    public void setRenderCharacter(boolean renderCharacter) {
        isRenderCharacter = renderCharacter;
    }

    private void checkPositionEvents(float x, float y) {
        getMapRenderer().setZoomed(false);
        currentEvent = eventManager.checkPositionEvents(x, y);
        mapRenderer.setRenderInfoCard(false);

        isRenderCharacter = true;
        if (currentEvent != null) {
            hasActiveEvent = true;
            currentEventType = currentEvent.getEventType();
            if (currentEventType.equals("battle") && map.getMapName().equals("board")) {
                isRenderCharacter = false;
                mapRenderer.setRenderInfoCard(true);
                getMapRenderer().setAcceptingRoll(true);
            } else if (currentEventType.equals("trap") && !trapUnlock.containsKey(currentEventId)) {
                getMapRenderer().setAcceptingRoll(true);
            } else {
                getMapRenderer().setAcceptingRoll(false);
            }
            currentEventId = currentEvent.getId();
            currentEventX = currentEvent.getGridX();
            currentEventY = currentEvent.getGridY();
            properties = currentEvent.getProperties();

        } else {
            currentEventType = "";
            currentEventId = null;
            currentEventX = -1;
            currentEventY = -1;
            hasActiveEvent = false;
            properties = null;
        }
    }

    public void setEndEvent() {
        currentEventType = null;
        hasActiveEvent = false;
        currentEvent = null;
        properties = null;
        currentEventId = null;
    }

    String currentEventId;

    public String getCurrentEventId() {
        return currentEventId;
    }

    public void setCompletedEvent() {
        eventManager.completeEvent(currentEventId);
        currentEventType = null;
        hasActiveEvent = false;
        currentEvent = null;
        properties = null;
        currentEventId = null;
    }


    public MapProperties getProperties() {
        return properties;
    }

    public MapEvent getCurrentEvent() {
        return currentEvent;
    }

    Map<String, Boolean> trapUnlock = new HashMap<>();

    public void unlockTrap(String trapId) {
        if (trapUnlock.containsKey(trapId))
            return; // Trap already unlocked
        trapUnlock.put(trapId, true);
    }

    public void handleEventProperties(MapProperties properties, String event) {

        int gridX = (int) getCharacter().getGridX();
        int gridY = (int) getCharacter().getGridY();

        float score = getCharacter().getScore();

        if (currentEventX != gridX || currentEventY != gridY || getCharacter().isMoving() == true) {
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
                    if (arcId == null || sceneId == null) {
                        getDialogController().showSimpleMessage("!!!!KHÔNG THỂ TIẾN VÀO NƠI NÀY!!!!");
                        return;
                    }
                    if (arcId.equals("dungeon_book") && !getCharacter().getFlags().contains("dungeon_call")) {
                        getDialogController().showSimpleMessage("Quyển sách này thật kì lạ, làm sao để đọc được nó.");
                        return;
                    }
                    this.dialogController.startDialog(arcId, sceneId);

                }
                break;
            case "message":
                if (properties != null) {
                    String message = properties.get("message", String.class);
                    String flagName = properties.get("flag", String.class);
                    dialogController.showSimpleMessage(message);
                    addFlag(flagName);
                }
                break;
            case "gate":
                if (properties != null) {
                    String targetX = properties.get("xTarget", String.class);
                    String targetY = properties.get("yTarget", String.class);

                    boardEventManager.getWordScrambleGame().startGame();
                    boardEventManager.getWordScrambleGame().setQuizCompletionListener(success -> {
                        if (success) {
                            dialogController.showSimpleMessage(Arrays.asList(
                                    "Câu đố này là ai để lại, cảm giác thật quen thuộc.",
                                    "*** Trong lúc đó, ta được đưa đi đến một nơi khác. ***",
                                    "Hãy cẩn thận, có vẻ như đây là một nơi nguy hiểm."
                            ));
                            character.setPosition(Integer.parseInt(targetX), Integer.parseInt(targetY));
                        } else {
                            dialogController.showSimpleMessage("Câu đố này là ai để lại, cảm giác thật quen thuộc.");
                        }
                    });
                }
                break;
            case "flag":
                if (properties != null) {
                    String flagName = properties.get("flag", String.class);
                    if (flagName != null && !flagName.isEmpty()) {
                        addFlag(flagName);
                        if (flagName.equals("ghost_ashes")) {
                            dialogController.showSimpleMessage("Đã đạt được : Mảnh vỡ của bia đá [Tên bị mờ].\n " +
                                    "Trên đó khắc tên ai đó. Ngôn ngữ này ...");
                        } else if (flagName.equals("acient_note")) {
                            dialogController.showSimpleMessage("Tờ giấy mục nát, các dòng chữ trên đó bị mờ đi bởi thời gian. Nhưng có điều gì đó quan trọng được ghi ở đây.\n" +
                                    "Mình nên đưa nó cho linh hồn.");
                        } else dialogController.showSimpleMessage("Đã đạt được : " + flagName);

                        addFlag(flagName);
                        setCompletedEvent();
                    }
                }
                break;
            case "trap":
                if (properties != null && currentEventId != null) {
                    if (trapUnlock.containsKey(currentEventId)) {
                        boolean isSucess = new Random().nextBoolean();
                        if (isSucess) {
                            Items itemTrap = ItemLoader.getItemById(3);
                            openTreasureWithAnimation(itemTrap, 1, currentEventX, currentEventY);
                        } else {
                            Items itemTrap = ItemLoader.getItemById(4);
                            openTreasureWithAnimation(itemTrap, 1, currentEventX, currentEventY);
                        }
                    } else
                        dialogController.showSimpleMessage("Cái bẫy chưa được mở khóa, Hãy tung xúc sắc để phá bẫy và mở rương.");
                }
                break;

            case "tele":
                if (properties != null) {
                    String mapName = properties.get("map", String.class);

                    if (mapName == null || mapName.isEmpty()) {
                        getDialogController().showSimpleMessage("!!!!KHÔNG THỂ TIẾN VÀO NƠI NÀY!!!!");
                        return;
                    }

                    if (mapName.equals("board") && !getCharacter().getFlags().contains("dungeon_call")) {
                        getDialogController().showSimpleMessage("Cánh cổng thật kì lạ, nó dẫn đến đâu vậy?\n" +
                                "Ta nên hỏi ngài Klein về tác dụng của nó.");
                        return;
                    }
                    if (mapName.equals("unknown") && !character.getFlags().contains("klein_unlock")) {
                        getDialogController().showSimpleMessage("Ngôi nhà này đã bị khóa, có lẽ Cleric Klein sẽ mở khóa nó cho ta sau này.");
                        return;
                    } else if (mapName.equals("unknown") && character.getFlags().contains("klein_unlock")) {
                        changeMap("unknown");
                        if (!character.getFlags().contains("gate_stone")) {
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    dialogController.showSimpleMessage("Tại sao trong ngôi nhà này lại có một cánh cổng đá kì lạ như vậy?\n" +
                                            "Có lẽ ta nên tiến lên tìm hiểu thêm về nó.");
                                }
                            }, 2f);
                            addFlag("gate_stone");
                        }

                        return;
                    }
                    if (!mapName.equals(map.getMapName())) {
                        changeMap(mapName);
                        if (mapName.equals("board") && !character.getFlags().contains("maze_cutscene"))
                            startMazeCutScene();

                    }
                }
                break;
            case "return":
                if (properties != null) {
                    String mapName = properties.get("map", String.class);
                    System.out.println(mapName);
                    changeMapInVillage(mapName);
                }
                break;
            case "quiz":
                getDialogController().showSimpleMessage("Quiz mini-game will start soon!");

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {

                        startQuiz(1);

                    }
                }, 1f);

                // Add listener for quiz completion
                quizController.setQuizCompletionListener(success -> {
                    if (success) {
                        // Apply random movement effect after successful quiz
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    } else {
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    }
                });

                break;
            case "mulquiz":
                getDialogController().showSimpleMessage("Multi-Choice Quiz mini-game will start soon!");

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        startMulChoiceQuiz(1);
                    }
                }, 1f);

                mulChoiceQuizController.setQuizCompletionListener(success -> {
                    if (success) {
                        // Apply random movement effect after successful quiz
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    } else {
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    }
                });
                break;
            case "new_run_event":
                int run = character.getRun() + 1;
                isNewRun = true;
                switch (run) {
                    case 4:
                        dialogController.setOnDialogFinishedAction(() -> {
                            Enemy enemy = EnemyLoader.getEnemyById(6);
                            setState(GameState.GAMEPLAY);
                            gameplayController.activate();
                            gameplayController.startCombat(enemy);
                            character.updateRun();
                        });
                        dialogController.setOnCanncelFinishedAction(() -> {

                        });
                        dialogController.startDialog("scene_boss_floor_4", "scene_boss_crystal_intro");
                        break;
                    case 6:
                        dialogController.setOnDialogFinishedAction(() -> {
                            Enemy enemy = EnemyLoader.getEnemyById(7);
                            setState(GameState.GAMEPLAY);
                            gameplayController.activate();
                            gameplayController.startCombat(enemy);
                            character.updateRun();
                        });
                        dialogController.startDialog("scene_boss_floor_6", "scene_boss_sapphire_intro");
                        break;
                    case 8:
                        dialogController.setOnDialogFinishedAction(() -> {
                            Enemy enemy = EnemyLoader.getEnemyById(8);
                            setState(GameState.GAMEPLAY);
                            gameplayController.activate();
                            gameplayController.startCombat(enemy);
                            character.updateRun();
                        });
                        dialogController.startDialog("scene_boss_floor_8", "scene_boss_emerald_intro");
                        break;
                    case 10:
                        dialogController.setOnDialogFinishedAction(() -> {
                            Enemy enemy = EnemyLoader.getEnemyById(8);
                            setState(GameState.GAMEPLAY);
                            gameplayController.activate();
                            gameplayController.startCombat(enemy);
                            character.updateRun();
                        });
                        break;
                    default:
                        dialogController.showSimpleMessage("Bạn đã hoàn thành tầng " + character.getRun() + " của mê cung, hãy chuẩn bị cho tầng tiếp theo.");
                        changeMap("main");
                        character.updateRun();
                        break;

                }
                break;
            case "dungeon":
                getCharacter().updateRun();
                Random random = new Random();
                boolean isDungeon = random.nextBoolean();

                if (isDungeon) {
                    Enemy enemy = new Enemy(11, "Thủ vệ hồ", "Frost Guardian", "frost", 1, 25, 16);
                    enemy.setDefensePower(21);
                    game.getDarkestDungeonScreen().startCombat(enemy);
                    game.changeScreen("DARK_DUNGEON");
                } else {
                    Enemy enemy = new Enemy(11, "Thủ vệ hồ", "Frost Guardian", "minotaur", 1, 25, 16);
                    enemy.setDefensePower(21);
                    game.getDarkestDungeonScreen().startCombat(enemy);
                    game.changeScreen("DARK_DUNGEON");
                }

                isNewRun = true;
                setEndEvent();
                break;
            case "word_scramble":
                boardEventManager.getWordScrambleGame().startGame();
                boardEventManager.getWordScrambleGame().setQuizCompletionListener(success -> {
                    if (success) {
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    } else {
                        applyQuizMovementEffect(getCharacter().getScore() - score);
                        setCompletedEvent();

                    }
                });
                break;
            case "rest":
                dialogController.showSimpleMessage("Bạn đã nghỉ ngơi và hồi phục năng lượng. [FULL MP]");
                character.recoveryMana();
                effectManager.spawnEffectEvent("Star_Trail", 705, 400);
                break;
            case "cutscene":
                String cutsceneName = properties.get("cutscene", String.class);
                if (cutsceneName != null) {
                    System.out.println("Starting cutscene: " + cutsceneName);
                    switch (cutsceneName) {
                        case "completed_dungeon2": {
                            Array<String> subtitles = new Array<>();
                            subtitles.add("Một chiếc bàn đá đặt chính giữa căn phòng, được bao phủ bởi ánh sáng vàng ấm áp.\n Trên bàn là cuốn nhật ký với bìa da màu nâu, trên đó khắc hình biểu tượng của ba viên đá phép thuật. \n Ánh sáng từ cuốn nhật ký tỏa ra, lấp lánh như những ngôi sao nhỏ.");
                            subtitles.add("Đứng trước bàn đá, ánh sáng từ cơ thể họ phản chiếu lên các bức tường, làm nổi bật hình dáng.\n Họ đưa tay về phía cuốn nhật ký, ánh sáng vàng từ cuốn sách hòa quyện vào ánh sáng của họ.");
                            subtitles.add("Cầm lên cuốn nhật ký trên tay, ánh sáng từ cuốn sách chiếu sáng khuôn mặt họ, thể hiện sự quyết tâm và trọng trách lớn lao.\n Một giọng nói nhẹ nhàng nhưng đầy uy nghi vang lên, như thể chủ nhân của lăng mộ đang trực tiếp nói chuyện với nhân vật chính.");
                            subtitles.add(" - Người được thần chọn, nếu ngươi đã đến được đây, điều đó có nghĩa ngươi đã vượt qua thử thách của ta. Nhưng hành trình của ngươi chỉ mới bắt đầu. \n- Thế giới phép thuật này đang dần chìm vào Hư Vô – một thực thể không có hình dạng nhưng có thể xóa sạch mọi thứ. Kẻ xuyên không đầu tiên, ta, đã hy sinh để phong ấn Hư Vô, nhưng phong ấn đó đang dần yếu đi.");
                            subtitles.add(" - Để sửa chữa điều này, ngươi cần phải thu thập đủ ba viên đá cổ xưa: Viên Đá Ánh Sáng, Viên Đá Bóng Tối, và Viên Đá Cân Bằng. Chúng là chìa khóa để phong ấn lại Hư Vô và cũng là con đường duy nhất để ngươi trở về thế giới của mình. \n - Có lẽ Thần và Quyến giả của ngài đã giải thích cho ngươi về 3 viên đá này rồi đúng không");
                            subtitles.add(" - Tuy nhiên, nhiệm vụ này không dễ dàng. Quỷ Vương – hiện thân của Hư Vô – đang chờ ngươi. Hắn sẽ làm mọi cách để ngăn ngươi hoàn thành sứ mệnh.");
                            subtitles.add("**Giọng nói dần tan biến, và ánh sáng từ cuốn nhật ký dịu đi. Trên các trang sách, các dòng chữ bắt đầu hiện ra rõ ràng như được viết bằng ánh sáng, dẫn dắt nhân vật đến nhiệm vụ tiếp theo.");
                            startMulBGSubTitleCutscene("completed_dungeon2", subtitles);
                        }
                        break;
                        default:
                            break;
                    }
                }
                break;
        }

    }

    private void applyQuizMovementEffect(float scoreDifference) {
        if (scoreDifference == 0) {
            int dmg = 3 + (int) (Math.random() * 3); // Random damage between 1 and 3
            getDialogController().showSimpleMessage("Bạn đã trả lời sai. Bị tổn thương tinh thần -" + dmg + " HP.");
            character.decreaseHealth(dmg);

        } else {
            getDialogController().showSimpleMessage("Bạn đã trả lời đúng. Nhận thêm 1 lượt tung xúc sắc!");
            getDice().setBonusRoll(true);
            character.setBonusRolls(getDice().getBonusCount());
        }
    }

    boolean isNewRun = false;

    private void openTreasureWithAnimation(Items item, int amount, int x, int y) {

        effectManager.spawnEffectEvent("Star_Trail", 660, 370);

        // Create dialog message about the found item
        String message = "Bạn nhận được +" + amount + " " + item.getItemName() + "!";
        dialogController.showSimpleMessage(message);

        // Add the item to inventory after a short delay
        dialogController.setOnDialogFinishedAction(() -> {
            character.addItem(item, amount);
            eventManager.completeEvent(currentEvent.getId());
            setEndEvent();
        });
    }

    public Dice getDice() {
        return getInputController().getMapRenderer().getDiceRenderer();
    }

    public BoardEventManager getBoardEventManager() {
        return boardEventManager;
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

    public BitmapFont getRegularFont() {
        return regularFont;
    }

    public BitmapFont getCommonFont() {
        return commonFont;
    }

    public BitmapFont getTitleFont() {
        return titleFont;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setFont(BitmapFont font) {
        this.font = font;
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


    public DialogController getDialogController() {
        return dialogController;
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

    public SettingsMenu getSettingsMenuController() {
        return settingsMenu;
    }

    public MainMenu getMainMenuController() {
        return mainMenuController;
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
        if (mulChoiceQuizController != null) {
            mulChoiceQuizController.dispose();
        }
        if (wordNetValidator != null) {
            wordNetValidator.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (assetManager != null) {
            assetManager.dispose();
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (characterDisplay != null) {
            characterDisplay.dispose();
        }
        if (inventoryUI != null) {
            inventoryUI.dispose();
        }
        if (exploringUI != null) {
            exploringUI.dispose();
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

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public GameplayController getGameplayController() {
        return gameplayController;
    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
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

    public ExploringUI getExploringUI() {
        return exploringUI;
    }

    public void setExploringUI(ExploringUI exploringUI) {
        this.exploringUI = exploringUI;
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

    public LevelUpNotification getLevelUpNotification() {
        return levelUpNotification;
    }

    public BountyBoardView getBountyBoardView() {
        return bountyBoardView;
    }

    public void showLevelUpNotification() {
        if (levelUpNotification != null && character != null) {
            levelUpNotification.showLevelUp(character.getLevel());
            // Play a sound effect if desired
            if (effectManager != null) {
                effectManager.playClickSound();
            }


        }
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public WordNetValidator getWordNetValidator() {
        return wordNetValidator;
    }

    public BitmapFont getBigCommonFont() {
        return bigCommonFont;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
}
