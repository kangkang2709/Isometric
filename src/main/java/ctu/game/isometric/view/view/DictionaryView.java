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
import ctu.game.isometric.view.renderer.BorderRenderer;

public class DictionaryView {
    // Static color constants for memory efficiency

    // FF7R-inspired color scheme
    private static final Color BACKGROUND_COLOR = new Color(0.05f, 0.08f, 0.15f, 0.9f);
    private static final Color PANEL_COLOR = new Color(0.1f, 0.15f, 0.2f, 0.85f);
    private static final Color ACCENT_COLOR = new Color(0.2f, 0.6f, 0.8f, 1f);
    private static final Color ACCENT_GLOW = new Color(0.2f, 0.6f, 0.8f, 0.5f);
    private static final Color TEXT_HIGHLIGHT = new Color(0.9f, 0.9f, 1f, 1f);
    private static final Color BUTTON_COLOR = new Color(0.15f, 0.3f, 0.4f, 0.9f);
    private static final Color ACTIVE_TAB_COLOR = new Color(0.2f, 0.5f, 0.7f, 1f);
    private static final Color INACTIVE_TAB_COLOR = new Color(0.15f, 0.2f, 0.25f, 0.8f);
    private static final Color SCROLL_BAR_COLOR = new Color(0.15f, 0.2f, 0.25f, 0.6f);
    private static final Color SCROLL_THUMB_COLOR = new Color(0.2f, 0.6f, 0.8f, 0.8f);
    private static final Color BACK_BUTTON_COLOR = new Color(0.7f, 0.3f, 0.3f, 0.9f);

    // Search box effect colors
    private static final Color SEARCH_FOCUSED_COLOR = new Color(0.1f, 0.2f, 0.35f, 0.9f);
    private static final Color SEARCH_GLOW_COLOR = new Color(0.2f, 0.6f, 0.8f, 0.3f);

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

    // Search box effects
    private float searchFocusAnimation = 0f;
    private float cursorBlinkTimer = 0f;
    private boolean showCursor = true;
    private float searchGlowIntensity = 0f;

    private final GameController gameController;
    private final Dictionary dictionary;
    private final WordNetValidator wordNetValidator;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // UI areas
    private Rectangle searchBox = new Rectangle(100, 625, 300, 30);
    private Rectangle searchButton = new Rectangle(410, 625, 100, 30);
    private Rectangle learnedTab = new Rectangle(100, 580, 200, 30);
    private Rectangle newTab = new Rectangle(310, 580, 200, 30);
    private Rectangle wordListArea = new Rectangle(100, 150, 300, 420);
    private Rectangle detailsArea = new Rectangle(420, 150, 760, 420);
    private Rectangle backButton = new Rectangle(590, 80, 100, 40);
    private Rectangle pronounceButton = new Rectangle(530, 625, 120, 30);
    private Rectangle scoreButton = new Rectangle(660, 625, 120, 30);
    // Scroll bars
    private int currentWordScore = 0;
    private boolean scoreCalculated = false;

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

    // Reusable objects to reduce garbage collection
    private final Rectangle scissorRect = new Rectangle();

    Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, 1280, 720);

    private void drawStyledText(SpriteBatch batch, String text, float x, float y) {

        // Main text
        titleFont.setColor(0f, 1f, 0.82f, 1f); // Gần với #00FFD1
        titleFont.draw(batch, text, x, y);
    }

    BitmapFont titleFont;

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
            titleFont = generateVietNameseFont("NovaSquare-Regular.ttf", 20);
            this.labelFont = generateVietNameseFont("ModernAntiqua-Regular.ttf", 18);
        } catch (Exception e) {
            // Fallback to default font if custom font loading fails

            this.labelFont = new BitmapFont();
            this.titleFont = new BitmapFont();

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

                // Tinh chỉnh để giọng đọc nghe dễ chịu hơn
                voice.setRate(170);      // Tốc độ đọc vừa phải (mặc định thường là 150)
                voice.setPitch(105);      // Tông giọng trầm hơn chút (mặc định thường là 100)
                voice.setVolume(1.2f);   // Âm lượng vừa phải, không quá lớn

                isTTSEnabled = true;
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
        // Update search box animations
        updateSearchBoxEffects(delta);

        if (wordListChanged || selectedWordChanged) {
            updateScrollBars();
            needsRedraw = true;
            wordListChanged = false;
            selectedWordChanged = false;
        }
    }

    private void updateSearchBoxEffects(float delta) {
        // Search focus animation
        float targetFocus = isSearchFocused ? 1f : 0f;
        searchFocusAnimation += (targetFocus - searchFocusAnimation) * 5f * delta;

        // Cursor blink animation
        cursorBlinkTimer += delta;
        if (cursorBlinkTimer >= 1f) {
            showCursor = !showCursor;
            cursorBlinkTimer = 0f;
        }

        // Search glow effect
        if (isSearchFocused) {
            searchGlowIntensity = 0.5f + 0.3f * (float) Math.sin(Gdx.app.getGraphics().getFrameId() * 0.1f);
        } else {
            searchGlowIntensity *= 0.95f;
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
        }
        else if (searchButton.contains(x, y)) {
            searchWords();
        } else if (pronounceButton.contains(x, y) && selectedWord != null) {
            pronounceSelectedWord();
        }else if (scoreButton.contains(x, y) && selectedWord != null) {
            calculateAndDisplayScore();
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
                            Math.max(0, (int) (clickRatio * (displayedWords.size() - WORDS_PER_PAGE))));
                    updateScrollBars();
                }
            }
        } else if (detailsScrollBar.contains(x, y) && selectedWord != null) {
            if (maxDetailsScrollPosition > 0) {
                if (detailsScrollThumb.contains(x, y)) {
                    isDraggingDetailsThumb = true;
                } else {
                    float clickRatio = (detailsScrollBar.y + detailsScrollBar.height - y) / detailsScrollBar.height;
                    detailsScrollPosition = Math.min(maxDetailsScrollPosition, Math.max(0, clickRatio * maxDetailsScrollPosition));
                    updateScrollBars();
                }
            }
        }
    }
    private void calculateAndDisplayScore() {
        if (selectedWord == null) return;

        currentWordScore = wordNetValidator.getTotalScore(selectedWord);
        scoreCalculated = true;

    }
    public void handleMouseScroll(float amountX, float amountY, float mouseX, float mouseY) {
        float cappedScrollAmount = Math.max(-10, Math.min(10, amountY));

        if (wordListArea.contains(mouseX, mouseY) || wordListScrollBar.contains(mouseX, mouseY)) {
            int oldIndex = wordListStartIndex;
            wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                    Math.max(0, wordListStartIndex - (int) (cappedScrollAmount * 2)));
            if (oldIndex != wordListStartIndex) {
                updateScrollBars();
            }
        } else if ((detailsArea.contains(mouseX, mouseY) || detailsScrollBar.contains(mouseX, mouseY))
                && selectedWord != null) {
            float oldPosition = detailsScrollPosition;
            detailsScrollPosition = Math.min(maxDetailsScrollPosition,
                    Math.max(0, detailsScrollPosition - cappedScrollAmount * 20));
            if (oldPosition != detailsScrollPosition) {
                updateScrollBars();
            }
        }
    }

    public void handleMouseDrag(float x, float y) {
        if (isDraggingWordListThumb) {
            float dragRatio = (wordListScrollBar.y + wordListScrollBar.height - y) / wordListScrollBar.height;
            dragRatio = Math.max(0, Math.min(1, dragRatio));
            wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                    Math.max(0, (int) (dragRatio * (displayedWords.size() - WORDS_PER_PAGE))));
            updateScrollBars();
        } else if (isDraggingDetailsThumb) {
            float dragRatio = (detailsScrollBar.y + detailsScrollBar.height - y) / detailsScrollBar.height;
            dragRatio = Math.max(0, Math.min(1, dragRatio));
            detailsScrollPosition = Math.min(maxDetailsScrollPosition, Math.max(0, dragRatio * maxDetailsScrollPosition));
            updateScrollBars();
        }
    }

    public void handleMouseRelease() {
        isDraggingWordListThumb = false;
        isDraggingDetailsThumb = false;
    }

    private void updateScrollBars() {
        // Word list scroll bar
        if (displayedWords.size() > WORDS_PER_PAGE) {
            float contentRatio = (float) WORDS_PER_PAGE / displayedWords.size();
            float thumbHeight = Math.max(20, wordListArea.height * contentRatio);

            float scrollRange = wordListArea.height - thumbHeight;
            float scrollRatio = (float) wordListStartIndex / (displayedWords.size() - WORDS_PER_PAGE);

            wordListScrollThumb.height = thumbHeight;
            wordListScrollThumb.y = wordListArea.y + scrollRange * (1 - scrollRatio);
        } else {
            wordListScrollThumb.height = wordListArea.height;
            wordListScrollThumb.y = wordListArea.y;
        }

        // Details scroll bar
        if (selectedWord != null) {
            float contentHeight = calculateWordDetailsHeight();
            maxDetailsScrollPosition = Math.max(0, contentHeight - detailsArea.height);

            if (maxDetailsScrollPosition > 0) {
                float contentRatio = detailsArea.height / contentHeight;
                float thumbHeight = Math.max(20, detailsArea.height * contentRatio);

                float scrollRange = detailsArea.height - thumbHeight;
                float scrollRatio = detailsScrollPosition / maxDetailsScrollPosition;

                detailsScrollThumb.height = thumbHeight;
                detailsScrollThumb.y = detailsArea.y + scrollRange * (1 - scrollRatio);
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
                updateScrollBars();
            }

            if (currentIndex > 0) {
                selectedWord = displayedWords.get(currentIndex - 1);
                selectedWordChanged = true;
                // Reset score for new word
                scoreCalculated = false;
                currentWordScore = 0;
            }
        } else if (!displayedWords.isEmpty()) {
            selectedWord = displayedWords.get(0);
            selectedWordChanged = true;
            scoreCalculated = false;
            currentWordScore = 0;
        }
    }
    public void selectNextWord() {
        if (selectedWord != null) {
            int currentIndex = displayedWords.indexOf(selectedWord);
            int lastIndex = displayedWords.size() - 1;

            if (currentIndex == wordListStartIndex + WORDS_PER_PAGE - 1 && currentIndex < lastIndex) {
                wordListStartIndex++;
                updateScrollBars();
            }

            if (currentIndex < lastIndex) {
                selectedWord = displayedWords.get(currentIndex + 1);
                selectedWordChanged = true;
                // Reset score for new word
                scoreCalculated = false;
                currentWordScore = 0;
            }
        } else if (!displayedWords.isEmpty()) {
            selectedWord = displayedWords.get(0);
            selectedWordChanged = true;
            scoreCalculated = false;
            currentWordScore = 0;
        }
    }

    private void selectWordFromList(float y) {
        int index = (int) ((wordListArea.y + wordListArea.height - y) / 30) + wordListStartIndex;
        if (index >= 0 && index < displayedWords.size()) {
            selectedWord = displayedWords.get(index);
            detailsScrollPosition = 0;
            selectedWordChanged = true;
            // Reset score calculation for new word
            scoreCalculated = false;
            currentWordScore = 0;
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
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw panels with FF7R style
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Main background with angular design
        shapeRenderer.setColor(BACKGROUND_COLOR);
        drawAngularPanel(shapeRenderer, 80, 60, 1120, 600);

        // Enhanced search area with effects
        drawSearchBoxWithEffects();

        // Search button
        shapeRenderer.setColor(BUTTON_COLOR);
        drawAngularButton(shapeRenderer, searchButton.x, searchButton.y, searchButton.width, searchButton.height);

        // Pronounce button
        if (selectedWord != null && isTTSEnabled) {
            shapeRenderer.setColor(ACCENT_COLOR);
        } else {
            shapeRenderer.setColor(BUTTON_COLOR);
        }

        drawAngularButton(shapeRenderer, scoreButton.x, scoreButton.y, scoreButton.width, scoreButton.height);

        drawAngularButton(shapeRenderer, pronounceButton.x, pronounceButton.y, pronounceButton.width, pronounceButton.height);

        // Tabs with angled corners
        shapeRenderer.setColor(showingLearnedWords ? ACTIVE_TAB_COLOR : INACTIVE_TAB_COLOR);
        drawAngularTab(shapeRenderer, learnedTab.x, learnedTab.y, learnedTab.width, learnedTab.height, true);

        shapeRenderer.setColor(!showingLearnedWords ? ACTIVE_TAB_COLOR : INACTIVE_TAB_COLOR);
        drawAngularTab(shapeRenderer, newTab.x, newTab.y, newTab.width, newTab.height, false);

        // Word list area
        shapeRenderer.setColor(PANEL_COLOR);
        drawAngularPanel(shapeRenderer, wordListArea.x, wordListArea.y, wordListArea.width, wordListArea.height);

        // Word details area
        shapeRenderer.setColor(PANEL_COLOR);
        drawAngularPanel(shapeRenderer, detailsArea.x, detailsArea.y, detailsArea.width, detailsArea.height);

        // Back button
        shapeRenderer.setColor(BACK_BUTTON_COLOR);
        drawAngularButton(shapeRenderer, backButton.x, backButton.y, backButton.width, backButton.height);

        // Draw scroll bars with modern look
        shapeRenderer.setColor(SCROLL_BAR_COLOR);
        shapeRenderer.rect(wordListScrollBar.x, wordListScrollBar.y, wordListScrollBar.width, wordListScrollBar.height);
        shapeRenderer.rect(detailsScrollBar.x, detailsScrollBar.y, detailsScrollBar.width, detailsScrollBar.height);

        // Draw scroll thumbs with accent color
        shapeRenderer.setColor(SCROLL_THUMB_COLOR);
        shapeRenderer.rect(wordListScrollThumb.x, wordListScrollThumb.y, wordListScrollThumb.width, wordListScrollThumb.height);
        if (selectedWord != null) {
            shapeRenderer.rect(detailsScrollThumb.x, detailsScrollThumb.y, detailsScrollThumb.width, detailsScrollThumb.height);
        }

        // Highlight selected word
        if (selectedWord != null) {
            int index = displayedWords.indexOf(selectedWord);
            if (index >= wordListStartIndex && index < wordListStartIndex + WORDS_PER_PAGE) {
                shapeRenderer.setColor(ACCENT_GLOW);
                float y = wordListArea.y + wordListArea.height - 30 * (index - wordListStartIndex + 1);
                shapeRenderer.rect(wordListArea.x + 2, y, wordListArea.width - 4, 30);
            }
        }

        shapeRenderer.end();

        // Draw borders with glow effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        BorderRenderer.drawBorder(shapeRenderer, new Rectangle(80, 60, 1120, 600), 2f);
        BorderRenderer.drawBorder(shapeRenderer, wordListArea, 1f);
        BorderRenderer.drawBorder(shapeRenderer, detailsArea, 1f);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text rendering
        batch.setProjectionMatrix(uiMatrix);

        if (batch.isDrawing()) batch.end();
        batch.begin();

        // Title
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "DICTIONARY", 640, 700, 0, Align.center, false);

        // Enhanced search text with cursor
        renderSearchText(batch);

        drawStyledText(batch, "Search", searchButton.x + 20, searchButton.y + 20);
        drawStyledText(batch, "Get Score", scoreButton.x + 21, scoreButton.y + 22);
        // Pronounce button text
        if (selectedWord != null && isTTSEnabled) {
            labelFont.setColor(Color.WHITE);
        } else {
            labelFont.setColor(Color.GRAY);
        }

        drawStyledText(batch, "Pronounce", pronounceButton.x + 18, pronounceButton.y + 22);
        labelFont.setColor(Color.WHITE);

        // Tabs
        drawStyledText(batch, "Learned Words", learnedTab.x + 40, learnedTab.y + 20);
        drawStyledText(batch, "New Words", newTab.x + 44, newTab.y + 20);

        // Back button
        drawStyledText(batch, "Back", backButton.x + 30, backButton.y + 25);

        // Render word list
        renderWordList(batch);
        renderWordDetails(batch);
    }

    private void drawSearchBoxWithEffects() {
        // Glow effect when focused
        if (searchGlowIntensity > 0.1f) {
            shapeRenderer.setColor(SEARCH_GLOW_COLOR.r, SEARCH_GLOW_COLOR.g, SEARCH_GLOW_COLOR.b, searchGlowIntensity);
            drawAngularPanel(shapeRenderer, searchBox.x - 8, searchBox.y - 8, searchBox.width + 16, searchBox.height + 16);
        }

        // Search box background
        Color bgColor = isSearchFocused ? SEARCH_FOCUSED_COLOR : PANEL_COLOR;
        float alpha = 0.85f + searchFocusAnimation * 0.15f;
        shapeRenderer.setColor(bgColor.r, bgColor.g, bgColor.b, alpha);
        drawAngularPanel(shapeRenderer, searchBox.x - 2, searchBox.y - 2, searchBox.width + 4, searchBox.height + 4);
    }

    private void renderSearchText(SpriteBatch batch) {
        String displayText = searchText.isEmpty() ? "SEARCH..." : searchText;

        if (searchText.isEmpty()) {
            labelFont.setColor(0.5f, 0.5f, 0.5f, 0.7f);
        } else {
            labelFont.setColor(Color.WHITE);
        }

        labelFont.draw(batch, displayText, searchBox.x + 10, searchBox.y + 20);

        // Draw cursor when focused
        if (isSearchFocused && showCursor && !searchText.isEmpty()) {
            GlyphLayout layout = obtainGlyphLayout();
            layout.setText(labelFont, searchText);
            float cursorX = searchBox.x + 10 + layout.width;
            labelFont.setColor(ACCENT_COLOR);
            labelFont.draw(batch, "|", cursorX, searchBox.y + 20);
            freeGlyphLayout(layout);
        }
    }

    private void drawAngularPanel(ShapeRenderer renderer, float x, float y, float width, float height) {
        float cornerSize = 15f;

        // Main body
        renderer.rect(x + cornerSize, y, width - 2 * cornerSize, height);
        renderer.rect(x, y + cornerSize, width, height - 2 * cornerSize);

        // Corners
        renderer.triangle(
                x, y + cornerSize,
                x + cornerSize, y + cornerSize,
                x + cornerSize, y
        );
        renderer.triangle(
                x + width, y + cornerSize,
                x + width - cornerSize, y + cornerSize,
                x + width - cornerSize, y
        );
        renderer.triangle(
                x, y + height - cornerSize,
                x + cornerSize, y + height - cornerSize,
                x + cornerSize, y + height
        );
        renderer.triangle(
                x + width, y + height - cornerSize,
                x + width - cornerSize, y + height - cornerSize,
                x + width - cornerSize, y + height
        );
    }

    private void drawAngularButton(ShapeRenderer renderer, float x, float y, float width, float height) {
        float cornerSize = 8f;

        // Main body
        renderer.rect(x + cornerSize, y, width - 2 * cornerSize, height);
        renderer.rect(x, y + cornerSize, width, height - 2 * cornerSize);

        // Corners
        renderer.triangle(
                x, y + cornerSize,
                x + cornerSize, y + cornerSize,
                x + cornerSize, y
        );
        renderer.triangle(
                x + width, y + cornerSize,
                x + width - cornerSize, y + cornerSize,
                x + width - cornerSize, y
        );
        renderer.triangle(
                x, y + height - cornerSize,
                x + cornerSize, y + height - cornerSize,
                x + cornerSize, y + height
        );
        renderer.triangle(
                x + width, y + height - cornerSize,
                x + width - cornerSize, y + height - cornerSize,
                x + width - cornerSize, y + height
        );
    }

    private void drawAngularTab(ShapeRenderer renderer, float x, float y, float width, float height, boolean leftTab) {
        float angleOffset = 10f;

        if (leftTab) {
            renderer.triangle(
                    x, y,
                    x + angleOffset, y,
                    x, y + angleOffset
            );
            renderer.rect(x + angleOffset, y, width - angleOffset, height);
            renderer.rect(x, y + angleOffset, angleOffset, height - angleOffset);
        } else {
            renderer.triangle(
                    x + width, y,
                    x + width - angleOffset, y,
                    x + width, y + angleOffset
            );
            renderer.rect(x, y, width - angleOffset, height);
            renderer.rect(x + width - angleOffset, y + angleOffset, angleOffset, height - angleOffset);
        }
    }

    private void renderWordList(SpriteBatch batch) {
        wordListStartIndex = Math.max(0, Math.min(wordListStartIndex, displayedWords.isEmpty() ? 0 : displayedWords.size() - 1));

        if (displayedWords.isEmpty()) {
            labelFont.draw(batch, "NO WORD FOUND", wordListArea.x + 20, wordListArea.y + wordListArea.height - 20);
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

        // Calculate starting Y position with proper bounds checking
        float startY = detailsArea.y + detailsArea.height - 20 + detailsScrollPosition;
        float currentY = startY;

        // Minimum Y position to prevent text from being cut off
        float minVisibleY = detailsArea.y - 50; // Allow some margin for partially visible text
        float maxVisibleY = detailsArea.y + detailsArea.height + 50;

        // Word term
        if (currentY >= minVisibleY && currentY <= maxVisibleY) {
            titleFont.setColor(TEXT_HIGHLIGHT);
            titleFont.draw(batch, selectedWord.getTerm().toUpperCase(), detailsArea.x + 20, currentY);
        }
        currentY -= 40;

        if (scoreCalculated && currentWordScore > 0) {
            if (currentY >= minVisibleY && currentY <= maxVisibleY) {
                titleFont.setColor(ACCENT_COLOR);
                titleFont.draw(batch, "SCORE: " + currentWordScore, detailsArea.x + 20, currentY);
            }
            currentY -= 35;
        }
        // Pronunciation (if you uncomment it later)
        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            if (currentY >= minVisibleY && currentY <= maxVisibleY) {
                labelFont.setColor(Color.WHITE);
                labelFont.draw(batch, "Pronunciation: " + selectedWord.getPronunciation(),
                        detailsArea.x + 20, currentY);
            }
            currentY -= 30;
        }

        // Definitions
        if (!selectedWord.getDefinitions().isEmpty()) {
            if (currentY >= minVisibleY && currentY <= maxVisibleY) {
                drawStyledText(batch, "DEFINITIONS:", detailsArea.x + 20, currentY);
            }
            currentY -= 35;

            for (WordDefinition def : selectedWord.getDefinitions()) {
                // Definition text
                String defText = "• " + def.getPartOfSpeech().toUpperCase() + ": " + def.getDefinition();
                float defTextHeight = calculateTextHeight(defText, 16, detailsArea.width - 40);

                if (currentY >= minVisibleY - defTextHeight && currentY <= maxVisibleY) {
                    labelFont.setColor(Color.WHITE);
                    labelFont.draw(batch, defText, detailsArea.x + 20, currentY,
                            detailsArea.width - 40, Align.left, true);
                }
                currentY -= defTextHeight + 15;

                // Examples
                if (!def.getExamples().isEmpty()) {
                    if (currentY >= minVisibleY && currentY <= maxVisibleY) {
                        drawStyledText(batch, "EXAMPLES:", detailsArea.x + 40, currentY);
                    }
                    currentY -= 30;

                    for (String example : def.getExamples()) {
                        String formattedExample = "- " + example;
                        float exampleHeight = calculateTextHeight(formattedExample, 16, detailsArea.width - 60);

                        if (currentY >= minVisibleY - exampleHeight && currentY <= maxVisibleY) {
                            labelFont.setColor(0.9f, 0.9f, 1f, 1f); // Slightly blue-tinted white
                            labelFont.draw(batch, formattedExample, detailsArea.x + 40, currentY,
                                    detailsArea.width - 60, Align.left, true);
                        }
                        currentY -= exampleHeight + 10;
                    }
                    currentY -= 10; // Extra spacing after examples
                }

                // Synonyms
                if (!def.getSynonyms().isEmpty()) {
                    String synonyms = "SYNONYMS: " + String.join(", ", def.getSynonyms());
                    float synonymsHeight = calculateTextHeight(synonyms, 16, detailsArea.width - 60);

                    if (currentY >= minVisibleY - synonymsHeight && currentY <= maxVisibleY) {
                        labelFont.setColor(1f, 0.84f, 0f, 1f); // Gold color
                        labelFont.draw(batch, synonyms, detailsArea.x + 40, currentY,
                                detailsArea.width - 60, Align.left, true);
                    }
                    currentY -= synonymsHeight + 15;
                }

                // Antonyms
                if (!def.getAntonyms().isEmpty()) {
                    String antonyms = "ANTONYMS: " + String.join(", ", def.getAntonyms());
                    float antonymsHeight = calculateTextHeight(antonyms, 16, detailsArea.width - 60);

                    if (currentY >= minVisibleY - antonymsHeight && currentY <= maxVisibleY) {
                        labelFont.setColor(1f, 0.5f, 0.5f, 1f); // Light red color
                        labelFont.draw(batch, antonyms, detailsArea.x + 40, currentY,
                                detailsArea.width - 60, Align.left, true);
                    }
                    currentY -= antonymsHeight + 20;
                }

                // Section separator
                currentY -= 15;
            }
        }

        // Reset font color
        labelFont.setColor(Color.WHITE);

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

        float height = 80; // Basic padding + word term height (increased for title)

        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            height += 30;
        }

        if (!selectedWord.getDefinitions().isEmpty()) {
            height += 35; // "Definitions:" header (increased spacing)

            for (WordDefinition def : selectedWord.getDefinitions()) {
                String defText = "• " + def.getPartOfSpeech() + ": " + def.getDefinition();
                cachedFormattedDefinitions.add(defText);
                height += calculateTextHeight(defText, 16, detailsArea.width - 40) + 15; // Increased spacing

                if (!def.getExamples().isEmpty()) {
                    height += 30; // "Examples:" header

                    for (String example : def.getExamples()) {
                        String formattedExample = "- " + example;
                        height += calculateTextHeight(formattedExample, 16, detailsArea.width - 60) + 10;
                    }
                    height += 10; // Extra spacing after examples
                }

                if (!def.getSynonyms().isEmpty()) {
                    String synonyms = "SYNONYMS: " + String.join(", ", def.getSynonyms());
                    height += calculateTextHeight(synonyms, 16, detailsArea.width - 60) + 15;
                }

                if (!def.getAntonyms().isEmpty()) {
                    String antonyms = "ANTONYMS: " + String.join(", ", def.getAntonyms());
                    height += calculateTextHeight(antonyms, 16, detailsArea.width - 60) + 20;
                }

                height += 15; // Section separator
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
                    (int) scissors.x,
                    (int) scissors.y,
                    (int) scissors.width,
                    (int) scissors.height);
        }

        public static void popScissors() {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
    }
}