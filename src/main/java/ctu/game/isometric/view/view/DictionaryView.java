package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.dictionary.Dictionary;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.WordNetValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayDeque;
import java.util.Queue;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.JavaClipAudioPlayer;

public class DictionaryView {
    // Static color constants for memory efficiency
    private static final Color BACKGROUND_COLOR = new Color(0.2f, 0.2f, 0.2f, 1);
    private static final Color SEARCH_BOX_COLOR = new Color(0.3f, 0.3f, 0.3f, 1);
    private static final Color BUTTON_COLOR = new Color(0.5f, 0.5f, 0.5f, 1);
    private static final Color ACTIVE_TAB_COLOR = new Color(0.4f, 0.7f, 0.4f, 1);
    private static final Color INACTIVE_TAB_COLOR = new Color(0.3f, 0.3f, 0.3f, 1);
    private static final Color WORD_LIST_COLOR = new Color(0.25f, 0.25f, 0.25f, 1);
    private static final Color SELECTED_WORD_COLOR = new Color(0.4f, 0.4f, 0.6f, 1);
    private static final Color SCROLL_BAR_COLOR = new Color(0.3f, 0.3f, 0.3f, 1);
    private static final Color SCROLL_THUMB_COLOR = new Color(0.6f, 0.6f, 0.6f, 1);
    private static final Color BACK_BUTTON_COLOR = new Color(0.7f, 0.3f, 0.3f, 1);
    private static final Color PRONOUNCE_BUTTON_ACTIVE = new Color(0.3f, 0.6f, 0.3f, 1);

    // Simple object pooling without LibGDX ObjectPool
    private final Queue<GlyphLayout> glyphLayoutPool = new ArrayDeque<>();
    private static final int MAX_POOL_SIZE = 10;

    // Text measurement cache
    private final Map<String, Float> textHeightCache = new HashMap<>();
    private final Map<String, GlyphLayout> glyphLayoutCache = new HashMap<>();

    // Word details caching
    private Word cachedSelectedWord = null;
    private float cachedDetailsHeight = 0;
    private final List<String> cachedFormattedDefinitions = new ArrayList<>();

    // Dirty flags for rendering optimization
    private boolean needsRedraw = true;
    private boolean wordListChanged = false;
    private boolean selectedWordChanged = false;

    private final GameController gameController;
    private final Dictionary dictionary;
    private final WordNetValidator wordNetValidator;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // UI areas
    private Rectangle searchBox = new Rectangle(100, 650, 300, 30);
    private Rectangle searchButton = new Rectangle(410, 650, 100, 30);
    private Rectangle learnedTab = new Rectangle(100, 600, 200, 30);
    private Rectangle newTab = new Rectangle(310, 600, 200, 30);
    private Rectangle wordListArea = new Rectangle(100, 150, 300, 420);
    private Rectangle detailsArea = new Rectangle(420, 150, 760, 420);
    private Rectangle backButton = new Rectangle(590, 80, 100, 40);

    // Scroll bars
    private Rectangle wordListScrollBar = new Rectangle(400 - 10, 150, 10, 420);
    private Rectangle wordListScrollThumb = new Rectangle(400 - 10, 150, 10, 100);
    private Rectangle detailsScrollBar = new Rectangle(1180 - 10, 150, 10, 420);
    private Rectangle detailsScrollThumb = new Rectangle(1180 - 10, 150, 10, 100);

    private boolean isDraggingWordListThumb = false;
    private boolean isDraggingDetailsThumb = false;
    private float detailsScrollPosition = 0;
    private float maxDetailsScrollPosition = 0;

    private List<Word> displayedWords = new ArrayList<>();
    private boolean showingLearnedWords = true;
    private Word selectedWord = null;
    private String searchText = "";
    private int wordListStartIndex = 0;
    private static final int WORDS_PER_PAGE = 14;
    private BitmapFont labelFont;
    private boolean isSearchFocused = false;
    int newLearnedWords = 0;

    // TTS with improved thread management
    private Voice voice;
    private boolean isTTSEnabled = true;
    private ExecutorService ttsExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean isSpeaking = false;
    private Rectangle pronounceButton = new Rectangle(530, 650, 120, 30);

    // Reusable objects to reduce garbage collection
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Rectangle scissorRect = new Rectangle();
    private final Vector3 tempVector = new Vector3();
    private final Color tempColor = new Color();

     Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, 1280, 720);

    public DictionaryView(GameController gameController, Dictionary dictionary, WordNetValidator wordNetValidator) {
        this.gameController = gameController;
        this.dictionary = dictionary;
        this.wordNetValidator = wordNetValidator;
        this.shapeRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 1280, 720);
        newLearnedWords = 0;

        // Initialize object pool
        for (int i = 0; i < MAX_POOL_SIZE; i++) {
            glyphLayoutPool.offer(new GlyphLayout());
        }

        try {
            this.labelFont = generateVietNameseFont("ModernAntiqua-Regular.ttf", 20);
        } catch (Exception e) {
            this.labelFont = new BitmapFont();
            this.labelFont.getData().setScale(1.2f);
            Gdx.app.error("DictionaryView", "Failed to load Vietnamese font", e);
        }

        initializeTTS();
        updateWordList();
    }

    private GlyphLayout obtainGlyphLayout() {
        GlyphLayout layout = glyphLayoutPool.poll();
        return layout != null ? layout : new GlyphLayout();
    }

    private void freeGlyphLayout(GlyphLayout layout) {
        if (glyphLayoutPool.size() < MAX_POOL_SIZE) {
            glyphLayoutPool.offer(layout);
        }
    }

    private void initializeTTS() {
        try {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager voiceManager = VoiceManager.getInstance();
            voice = voiceManager.getVoice("kevin16");

            if (voice != null) {
                voice.allocate();
                voice.setRate(150);
                voice.setPitch(100);
                voice.setVolume(1.0f);
            } else {
                isTTSEnabled = false;
                Gdx.app.error("DictionaryView", "TTS voice not available");
            }
        } catch (Exception e) {
            isTTSEnabled = false;
            Gdx.app.error("DictionaryView", "Failed to initialize TTS", e);
        }
    }

    private void speakText(String text) {
        if (!isTTSEnabled || voice == null || text == null || text.trim().isEmpty()) {
            return;
        }

        // Cancel current speech if speaking
        if (isSpeaking) {
            try {
                // FreeTTS doesn't have stop method, use interrupt instead
                ttsExecutor.shutdownNow();
                ttsExecutor = Executors.newSingleThreadExecutor();
                isSpeaking = false;
            } catch (Exception e) {
                Gdx.app.error("DictionaryView", "Error stopping speech", e);
            }
        }

        ttsExecutor.submit(() -> {
            try {
                isSpeaking = true;
                voice.speak(text);
            } catch (Exception e) {
                Gdx.app.error("DictionaryView", "Error during speech synthesis", e);
            } finally {
                isSpeaking = false;
            }
        });
    }

    private void pronounceSelectedWord() {
        if (selectedWord == null) return;

        String textToSpeak = selectedWord.getTerm();
        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            textToSpeak = selectedWord.getTerm();
        }

        speakText(textToSpeak);
    }

    public void update(float delta) {
        if (wordListChanged || selectedWordChanged) {
            updateScrollBars();
            needsRedraw = true;
            wordListChanged = false;
            selectedWordChanged = false;
        }
    }

    public boolean handleKeyTyped(char character) {
        if (!isSearchFocused) {
            return false;
        }

        if (character == '\b') {
            if (searchText.length() > 0) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            return false;
        }

        if (character == '\r' || character == '\n') {
            searchWords();
            return true;
        }

        if (!Character.isISOControl(character)) {
            searchText += character;
            return true;
        }

        return false;
    }

    public void handleMouseClick(float x, float y) {
        y = Gdx.graphics.getHeight() - y;

        isSearchFocused = searchBox.contains(x, y);

        if (learnedTab.contains(x, y)) {
            showingLearnedWords = true;
            updateWordList();
        } else if (newTab.contains(x, y)) {
            showingLearnedWords = false;
            updateWordList();
        } else if (searchButton.contains(x, y)) {
            searchWords();
        } else if (pronounceButton.contains(x, y) && selectedWord != null) {
            pronounceSelectedWord();
        } else if (backButton.contains(x, y)) {
            gameController.setCurrentState(GameState.EXPLORING);
        } else if (wordListArea.contains(x, y)) {
            selectWordFromList(y);
        } else if (wordListScrollBar.contains(x, y)) {
            if (displayedWords.size() > WORDS_PER_PAGE) {
                if (wordListScrollThumb.contains(x, y)) {
                    isDraggingWordListThumb = true;
                } else {
                    float clickRatio = (wordListScrollBar.y + wordListScrollBar.height - y) / wordListScrollBar.height;
                    wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                            Math.max(0, (int)(clickRatio * displayedWords.size())));
                }
            }
        } else if (detailsScrollBar.contains(x, y) && selectedWord != null) {
            if (maxDetailsScrollPosition > 0) {
                if (detailsScrollThumb.contains(x, y)) {
                    isDraggingDetailsThumb = true;
                } else {
                    float clickRatio = (detailsScrollBar.y + detailsScrollBar.height - y) / detailsScrollBar.height;
                    detailsScrollPosition = Math.min(maxDetailsScrollPosition, Math.max(0, clickRatio * maxDetailsScrollPosition));
                }
            }
        }
    }

    public void handleMouseScroll(float amountX, float amountY, float mouseX, float mouseY) {
        float cappedScrollAmount = Math.max(-10, Math.min(10, amountY));

        if (wordListArea.contains(mouseX, mouseY) || wordListScrollBar.contains(mouseX, mouseY)) {
            wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                    Math.max(0, wordListStartIndex - (int)(cappedScrollAmount * 4)));
        } else if ((detailsArea.contains(mouseX, mouseY) || detailsScrollBar.contains(mouseX, mouseY))
                && selectedWord != null) {
            detailsScrollPosition = Math.min(maxDetailsScrollPosition,
                    Math.max(0, detailsScrollPosition - cappedScrollAmount * 8));
        }
    }

    public void handleMouseDrag(float x, float y) {
        if (isDraggingWordListThumb) {
            float dragRatio = (wordListScrollBar.y + wordListScrollBar.height - y) / wordListScrollBar.height;
            wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                    Math.max(0, (int)(dragRatio * displayedWords.size())));
        } else if (isDraggingDetailsThumb) {
            float dragRatio = (detailsScrollBar.y + detailsScrollBar.height - y) / detailsScrollBar.height;
            detailsScrollPosition = Math.min(maxDetailsScrollPosition, Math.max(0, dragRatio * maxDetailsScrollPosition));
        }
    }

    public void handleMouseRelease() {
        isDraggingWordListThumb = false;
        isDraggingDetailsThumb = false;
    }

    private void updateScrollBars() {
        if (displayedWords.size() > WORDS_PER_PAGE) {
            float thumbHeight = Math.max(50, wordListArea.height * WORDS_PER_PAGE / displayedWords.size());
            float maxThumbY = wordListArea.y + wordListArea.height - thumbHeight;
            float scrollRange = wordListArea.height - thumbHeight;
            float scrollRatio = (float) wordListStartIndex / (displayedWords.size() - WORDS_PER_PAGE);

            wordListScrollThumb.height = thumbHeight;
            wordListScrollThumb.y = maxThumbY - scrollRange * scrollRatio;
        } else {
            wordListScrollThumb.height = wordListArea.height;
            wordListScrollThumb.y = wordListArea.y;
        }

        if (selectedWord != null) {
            float contentHeight = calculateWordDetailsHeight();
            maxDetailsScrollPosition = Math.max(0, contentHeight - detailsArea.height);

            if (contentHeight > detailsArea.height) {
                float thumbHeight = Math.max(50, detailsArea.height * detailsArea.height / contentHeight);
                float maxThumbY = detailsArea.y + detailsArea.height - thumbHeight;
                float scrollRange = detailsArea.height - thumbHeight;
                float scrollRatio = detailsScrollPosition / maxDetailsScrollPosition;

                detailsScrollThumb.height = thumbHeight;
                detailsScrollThumb.y = maxThumbY - scrollRange * scrollRatio;
            } else {
                detailsScrollThumb.height = detailsArea.height;
                detailsScrollThumb.y = detailsArea.y;
                detailsScrollPosition = 0;
            }
        }
    }

    public void selectPreviousWord() {
        if (selectedWord != null) {
            int currentIndex = displayedWords.indexOf(selectedWord);

            if (currentIndex == wordListStartIndex && wordListStartIndex > 0) {
                wordListStartIndex--;
            }

            if (currentIndex > 0) {
                selectedWord = displayedWords.get(currentIndex - 1);
                selectedWordChanged = true;
            }
        } else if (!displayedWords.isEmpty()) {
            selectedWord = displayedWords.get(0);
            selectedWordChanged = true;
        }
    }

    public void selectNextWord() {
        if (selectedWord != null) {
            int currentIndex = displayedWords.indexOf(selectedWord);
            int lastIndex = displayedWords.size() - 1;

            if (currentIndex == wordListStartIndex + WORDS_PER_PAGE - 1 && currentIndex < lastIndex) {
                wordListStartIndex++;
            }

            if (currentIndex < lastIndex) {
                selectedWord = displayedWords.get(currentIndex + 1);
                selectedWordChanged = true;
            }
        } else if (!displayedWords.isEmpty()) {
            selectedWord = displayedWords.get(0);
            selectedWordChanged = true;
        }
    }

    private void selectWordFromList(float y) {
        int index = (int)((wordListArea.y + wordListArea.height - y) / 30) + wordListStartIndex;
        if (index >= 0 && index < displayedWords.size()) {
            selectedWord = displayedWords.get(index);
            detailsScrollPosition = 0;
            selectedWordChanged = true;
        }
    }

    private void updateWordList() {
        displayedWords.clear();
        Set<Word> words = showingLearnedWords ? dictionary.getLearnedWords() : dictionary.getNewWords();
        displayedWords.addAll(words);

        if (wordListStartIndex + WORDS_PER_PAGE > displayedWords.size()) {
            wordListStartIndex = Math.max(0, displayedWords.size() - WORDS_PER_PAGE);
        }

        wordListChanged = true;
    }

    public void addNewWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        Word newWord = wordNetValidator.getWordDetails(word);
        if (!dictionary.getNewWords().contains(newWord) && !dictionary.getLearnedWords().contains(newWord)) {
            System.out.printf("word: %s\n", newWord.getTerm());
            dictionary.addNewWord(newWord);
            newLearnedWords++;
        }
    }

    private void searchWords() {
        if (searchText.isEmpty()) {
            updateWordList();
            return;
        }
        displayedWords.clear();
        Set<Word> searchResults = dictionary.searchWords(searchText);
        if (searchResults != null && !searchResults.isEmpty()) {
            displayedWords.addAll(searchResults);
            selectedWord = displayedWords.iterator().next();
            selectedWordChanged = true;
        }
        wordListStartIndex = 0;
        wordListChanged = true;
    }

    public void render(SpriteBatch batch) {
        camera.update();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Main background
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(80, 60, 1120, 600);

        // Search area
        shapeRenderer.setColor(SEARCH_BOX_COLOR);
        shapeRenderer.rect(searchBox.x, searchBox.y, searchBox.width, searchBox.height);

        // Search button
        shapeRenderer.setColor(BUTTON_COLOR);
        shapeRenderer.rect(searchButton.x, searchButton.y, searchButton.width, searchButton.height);

        // Pronounce button
        if (selectedWord != null && isTTSEnabled) {
            shapeRenderer.setColor(PRONOUNCE_BUTTON_ACTIVE);
        } else {
            shapeRenderer.setColor(SEARCH_BOX_COLOR);
        }
        shapeRenderer.rect(pronounceButton.x, pronounceButton.y, pronounceButton.width, pronounceButton.height);

        // Tabs
        shapeRenderer.setColor(showingLearnedWords ? ACTIVE_TAB_COLOR : INACTIVE_TAB_COLOR);
        shapeRenderer.rect(learnedTab.x, learnedTab.y, learnedTab.width, learnedTab.height);

        shapeRenderer.setColor(!showingLearnedWords ? ACTIVE_TAB_COLOR : INACTIVE_TAB_COLOR);
        shapeRenderer.rect(newTab.x, newTab.y, newTab.width, newTab.height);

        // Word list area
        shapeRenderer.setColor(WORD_LIST_COLOR);
        shapeRenderer.rect(wordListArea.x, wordListArea.y, wordListArea.width, wordListArea.height);

        // Word details area
        shapeRenderer.setColor(WORD_LIST_COLOR);
        shapeRenderer.rect(detailsArea.x, detailsArea.y, detailsArea.width, detailsArea.height);

        // Back button
        shapeRenderer.setColor(BACK_BUTTON_COLOR);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);

        // Draw scroll bars
        shapeRenderer.setColor(SCROLL_BAR_COLOR);
        shapeRenderer.rect(wordListScrollBar.x, wordListScrollBar.y, wordListScrollBar.width, wordListScrollBar.height);
        shapeRenderer.rect(detailsScrollBar.x, detailsScrollBar.y, detailsScrollBar.width, detailsScrollBar.height);

        // Draw scroll thumbs
        shapeRenderer.setColor(SCROLL_THUMB_COLOR);
        shapeRenderer.rect(wordListScrollThumb.x, wordListScrollThumb.y, wordListScrollThumb.width, wordListScrollThumb.height);
        if (selectedWord != null) {
            shapeRenderer.rect(detailsScrollThumb.x, detailsScrollThumb.y, detailsScrollThumb.width, detailsScrollThumb.height);
        }

        // Highlight selected word
        if (selectedWord != null) {
            int index = displayedWords.indexOf(selectedWord);
            if (index >= wordListStartIndex && index < wordListStartIndex + WORDS_PER_PAGE) {
                shapeRenderer.setColor(SELECTED_WORD_COLOR);
                float y = wordListArea.y + wordListArea.height - 30 * (index - wordListStartIndex + 1);
                shapeRenderer.rect(wordListArea.x, y, wordListArea.width, 30);
            }
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text rendering
        batch.setProjectionMatrix(uiMatrix);

        if(batch.isDrawing()) batch.end();
        batch.begin();

        // Title
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "DICTIONARY", 640, 700, 0, Align.center, false);

        // Search text
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, searchText.isEmpty() ? "Search..." : searchText,
                searchBox.x + 10, searchBox.y + 20);
        labelFont.draw(batch, "Search", searchButton.x + 20, searchButton.y + 20);

        // Pronounce button text
        if (selectedWord != null && isTTSEnabled) {
            labelFont.setColor(Color.WHITE);
        } else {
            labelFont.setColor(Color.GRAY);
        }
        labelFont.draw(batch, "Pronounce", pronounceButton.x + 20, pronounceButton.y + 20);
        labelFont.setColor(Color.WHITE);

        // Tabs
        labelFont.draw(batch, "Learned Words", learnedTab.x + 40, learnedTab.y + 20);
        labelFont.draw(batch, "New Words", newTab.x + 60, newTab.y + 20);

        // Back button
        labelFont.draw(batch, "Back", backButton.x + 30, backButton.y + 25);

        // Render word list
        renderWordList(batch);
        renderWordDetails(batch);

    }

    private void renderWordList(SpriteBatch batch) {
        wordListStartIndex = Math.max(0, Math.min(wordListStartIndex, displayedWords.isEmpty() ? 0 : displayedWords.size() - 1));

        if (displayedWords.isEmpty()) {
            labelFont.draw(batch, "No words found", wordListArea.x + 20, wordListArea.y + wordListArea.height - 20);
            return;
        }

        final float baseY = wordListArea.y + wordListArea.height - 30;
        final float wordX = wordListArea.x + 10;
        final int endIndex = Math.min(wordListStartIndex + WORDS_PER_PAGE, displayedWords.size());

        for (int i = wordListStartIndex; i < endIndex; i++) {
            Word word = displayedWords.get(i);
            float y = baseY - (30 * (i - wordListStartIndex));
            labelFont.draw(batch, word.getTerm(), wordX, y + 20);
        }
    }

    private void renderWordDetails(SpriteBatch batch) {
        if (selectedWord == null) return;

        if (!batch.isDrawing()) {
            batch.begin();
        }

        batch.flush();
        scissorRect.set(detailsArea.x, detailsArea.y, detailsArea.width, detailsArea.height);
        ScissorStack.pushScissors(scissorRect);

        float y = detailsArea.y + detailsArea.height - 20 + detailsScrollPosition;

        // Word term
        labelFont.draw(batch, selectedWord.getTerm(), detailsArea.x + 20, y);
        y -= 40;

        // Pronunciation
        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            labelFont.draw(batch, "Pronunciation: " + selectedWord.getPronunciation(),
                    detailsArea.x + 20, y);
            y -= 30;
        }

        // Definitions
        if (!selectedWord.getDefinitions().isEmpty()) {
            labelFont.draw(batch, "Definitions:", detailsArea.x + 20, y);
            y -= 30;

            for (WordDefinition def : selectedWord.getDefinitions()) {
                String defText = "• " + def.getPartOfSpeech() + ": " + def.getDefinition();
                float defTextHeight = calculateTextHeight(defText, 16, detailsArea.width - 40);
                labelFont.draw(batch, defText, detailsArea.x + 20, y,
                        detailsArea.width - 40, Align.left, true);
                y -= defTextHeight + 10;

                // Examples
                if (!def.getExamples().isEmpty()) {
                    labelFont.draw(batch, "Examples:", detailsArea.x + 40, y);
                    y -= 20;

                    for (String example : def.getExamples()) {
                        String formattedExample = "- " + example;
                        float exampleHeight = calculateTextHeight(formattedExample, 16, detailsArea.width - 60);
                        labelFont.draw(batch, formattedExample, detailsArea.x + 40, y,
                                detailsArea.width - 60, Align.left, true);
                        y -= exampleHeight + 5;
                    }
                }

                // Synonyms
                if (!def.getSynonyms().isEmpty()) {
                    String synonyms = "Synonyms: " + String.join(", ", def.getSynonyms());
                    float synonymsHeight = calculateTextHeight(synonyms, 16, detailsArea.width - 60);
                    labelFont.draw(batch, synonyms, detailsArea.x + 40, y,
                            detailsArea.width - 60, Align.left, true);
                    y -= synonymsHeight + 15;
                }

                // Antonyms
                if (!def.getAntonyms().isEmpty()) {
                    String antonyms = "Antonyms: " + String.join(", ", def.getAntonyms());
                    float antonymsHeight = calculateTextHeight(antonyms, 16, detailsArea.width - 60);
                    labelFont.draw(batch, antonyms, detailsArea.x + 40, y,
                            detailsArea.width - 60, Align.left, true);
                    y -= antonymsHeight + 15;
                }
            }
        }

        batch.flush();
        ScissorStack.popScissors();
    }

    private float calculateWordDetailsHeight() {
        if (selectedWord == null) return 0;

        // Use cached value if word hasn't changed
        if (selectedWord.equals(cachedSelectedWord)) {
            return cachedDetailsHeight;
        }

        cachedSelectedWord = selectedWord;
        cachedFormattedDefinitions.clear();

        float height = 60; // Basic padding + word term height

        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            height += 30;
        }

        if (!selectedWord.getDefinitions().isEmpty()) {
            height += 30; // "Definitions:" header

            for (WordDefinition def : selectedWord.getDefinitions()) {
                String defText = "• " + def.getPartOfSpeech() + ": " + def.getDefinition();
                cachedFormattedDefinitions.add(defText);
                height += calculateTextHeight(defText, 16, detailsArea.width - 40) + 10;

                if (!def.getExamples().isEmpty()) {
                    height += 20; // "Examples:" header

                    for (String example : def.getExamples()) {
                        String formattedExample = "- " + example;
                        height += calculateTextHeight(formattedExample, 16, detailsArea.width - 60) + 5;
                    }
                }

                if (!def.getSynonyms().isEmpty()) {
                    String synonyms = "Synonyms: " + String.join(", ", def.getSynonyms());
                    height += calculateTextHeight(synonyms, 16, detailsArea.width - 60) + 15;
                }

                if (!def.getAntonyms().isEmpty()) {
                    String antonyms = "Antonyms: " + String.join(", ", def.getAntonyms());
                    height += calculateTextHeight(antonyms, 16, detailsArea.width - 60) + 15;
                }
            }
        }

        cachedDetailsHeight = height;
        return height;
    }

    private float calculateTextHeight(String text, int fontSize, float width) {
        String cacheKey = text + "_" + fontSize + "_" + width;
        return textHeightCache.computeIfAbsent(cacheKey, k -> {
            GlyphLayout layout = obtainGlyphLayout();
            layout.setText(labelFont, text, Color.WHITE, width, Align.left, true);
            float height = layout.height;
            freeGlyphLayout(layout);
            return height;
        });
    }

    public void dispose() {
        // Clear caches
        textHeightCache.clear();
        glyphLayoutCache.clear();
        cachedFormattedDefinitions.clear();
        glyphLayoutPool.clear();

        // Dispose graphics resources
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }

        if (labelFont != null) {
            labelFont.dispose();
            labelFont = null;
        }

        // Clean up TTS resources
        if (ttsExecutor != null) {
            ttsExecutor.shutdown();
            try {
                if (!ttsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    ttsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ttsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (voice != null) {
            try {
                voice.deallocate();
            } catch (Exception e) {
                Gdx.app.error("DictionaryView", "Error deallocating TTS voice", e);
            }
            voice = null;
        }
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    // ScissorStack helper class to clip rendering
    private static class ScissorStack {
        private static final Rectangle scissors = new Rectangle();

        public static void pushScissors(Rectangle scissor) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            scissors.set(
                    scissor.x,
                    Gdx.graphics.getHeight() - scissor.y - scissor.height,
                    scissor.width,
                    scissor.height);
            Gdx.gl.glScissor(
                    (int)scissors.x,
                    (int)scissors.y,
                    (int)scissors.width,
                    (int)scissors.height);
        }

        public static void popScissors() {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
    }
}