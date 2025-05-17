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
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.dictionary.Dictionary;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.WordNetValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class DictionaryView {
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

    public DictionaryView(GameController gameController, Dictionary dictionary, WordNetValidator wordNetValidator) {
        this.gameController = gameController;
        this.dictionary = dictionary;
        this.wordNetValidator = wordNetValidator;
        this.shapeRenderer = new ShapeRenderer();
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 1280, 720);
        newLearnedWords = 0;

        try {
            this.labelFont = generateVietNameseFont("ModernAntiqua-Regular.ttf", 20);
        } catch (Exception e) {
            this.labelFont = new BitmapFont();
            this.labelFont.getData().setScale(1.2f);
            Gdx.app.error("DictionaryView", "Failed to load Vietnamese font", e);
        }

        updateWordList();
    }

    public void update(float delta) {
        updateScrollBars();
    }
    public boolean handleKeyTyped(char character) {
        if (!isSearchFocused) {
            return false;
        }

        // Handle backspace
        if (character == '\b') {
            if (searchText.length() > 0) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            return false;
        }

        // Handle enter key
        if (character == '\r' || character == '\n') {
            searchWords();
            return true;
        }

        // Only accept printable characters
        if (!Character.isISOControl(character)) {
            searchText += character;
            return true;
        }

        return false;
    }

    // Methods to add to DictionaryView class
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
        } else if (backButton.contains(x, y)) {
            gameController.setCurrentState(GameState.EXPLORING);
        } else if (wordListArea.contains(x, y)) {
            selectWordFromList(y);
        } else if (wordListScrollBar.contains(x, y)) {
            // Handle word list scroll bar click
            if (displayedWords.size() > WORDS_PER_PAGE) {
                if (wordListScrollThumb.contains(x, y)) {
                    isDraggingWordListThumb = true;
                } else {
                    // Jump to position
                    float clickRatio = (wordListScrollBar.y + wordListScrollBar.height - y) / wordListScrollBar.height;
                    wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                            Math.max(0, (int)(clickRatio * displayedWords.size())));
                }
            }
        } else if (detailsScrollBar.contains(x, y) && selectedWord != null) {
            // Handle details scroll bar click
            if (maxDetailsScrollPosition > 0) {
                if (detailsScrollThumb.contains(x, y)) {
                    isDraggingDetailsThumb = true;
                } else {
                    // Jump to position
                    float clickRatio = (detailsScrollBar.y + detailsScrollBar.height - y) / detailsScrollBar.height;
                    detailsScrollPosition = Math.min(maxDetailsScrollPosition, Math.max(0, clickRatio * maxDetailsScrollPosition));
                }
            }
        } else if (!showingLearnedWords && selectedWord != null &&
                x >= detailsArea.x + 20 && x <= detailsArea.x + 200 &&
                y >= detailsArea.y + 15 && y <= detailsArea.y + 45) {
            dictionary.markWordAsLearned(selectedWord.getTerm());
            updateWordList();
            selectedWord = null;
        }
    }

    public void handleMouseScroll(float amountX, float amountY, float mouseX, float mouseY) {
        // Cap the scroll amount to prevent large jumps
        float cappedScrollAmount = Math.max(-10, Math.min(10, amountY));

        if (wordListArea.contains(mouseX, mouseY) || wordListScrollBar.contains(mouseX, mouseY)) {
            wordListStartIndex = Math.min(displayedWords.size() - WORDS_PER_PAGE,
                    Math.max(0, wordListStartIndex - (int)(cappedScrollAmount * 4)));
            System.out.printf("wordListStartIndex: %d\n", wordListStartIndex);
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
        // Update word list scroll thumb position
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

        // Update details scroll thumb
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

            // If at the top of visible list and can scroll up
            if (currentIndex == wordListStartIndex && wordListStartIndex > 0) {
                wordListStartIndex--;
            }

            // Select previous word if possible
            if (currentIndex > 0) {
                selectedWord = displayedWords.get(currentIndex - 1);
            }
        } else if (!displayedWords.isEmpty()) {

            selectedWord = displayedWords.get(0);
        }
    }

    public void selectNextWord() {
        if (selectedWord != null) {
            int currentIndex = displayedWords.indexOf(selectedWord);
            int lastIndex = displayedWords.size() - 1;

            // If at the bottom of visible list and can scroll down
            if (currentIndex == wordListStartIndex + WORDS_PER_PAGE - 1 && currentIndex < lastIndex) {
                wordListStartIndex++;
            }

            // Select next word if possible
            if (currentIndex < lastIndex) {
                selectedWord = displayedWords.get(currentIndex + 1);
            }
        } else if (!displayedWords.isEmpty()) {
            // If no word selected, select the first word
            selectedWord = displayedWords.get(0);
        }
    }

    private void selectWordFromList(float y) {
        int index = (int)((wordListArea.y + wordListArea.height - y) / 30) + wordListStartIndex;
        if (index >= 0 && index < displayedWords.size()) {
            selectedWord = displayedWords.get(index);
            detailsScrollPosition = 0; // Reset details scroll when selecting a new word
        }
    }

    private void updateWordList() {
        displayedWords.clear();
        Set<Word> words = showingLearnedWords ? dictionary.getLearnedWords() : dictionary.getNewWords();
        displayedWords.addAll(words);

        // Adjust start index if needed
        if (wordListStartIndex + WORDS_PER_PAGE > displayedWords.size()) {
            wordListStartIndex = Math.max(0, displayedWords.size() - WORDS_PER_PAGE);
        }
    }

    public void addNewWord(String word){
        if (word == null || word.isEmpty()) {
            return;
        }
        Word newWord = wordNetValidator.getWordDetails(word);
        if (!dictionary.getNewWords().contains(newWord) && !dictionary.getLearnedWords().contains(newWord) ){
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
        }
        wordListStartIndex = 0;
    }

    public void render(SpriteBatch batch) {
        camera.update();

        // First draw shapes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Main background
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(80, 60, 1120, 600);

        // Search area
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(searchBox.x, searchBox.y, searchBox.width, searchBox.height);

        // Search button
        shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
        shapeRenderer.rect(searchButton.x, searchButton.y, searchButton.width, searchButton.height);

        // Tabs
        shapeRenderer.setColor(showingLearnedWords ? new Color(0.4f, 0.7f, 0.4f, 1) : new Color(0.3f, 0.3f, 0.3f, 1));
        shapeRenderer.rect(learnedTab.x, learnedTab.y, learnedTab.width, learnedTab.height);

        shapeRenderer.setColor(!showingLearnedWords ? new Color(0.4f, 0.7f, 0.4f, 1) : new Color(0.3f, 0.3f, 0.3f, 1));
        shapeRenderer.rect(newTab.x, newTab.y, newTab.width, newTab.height);

        // Word list area
        shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 1);
        shapeRenderer.rect(wordListArea.x, wordListArea.y, wordListArea.width, wordListArea.height);

        // Word details area
        shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 1);
        shapeRenderer.rect(detailsArea.x, detailsArea.y, detailsArea.width, detailsArea.height);

        // Back button
        shapeRenderer.setColor(0.7f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);

        // Draw scroll bars
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(wordListScrollBar.x, wordListScrollBar.y, wordListScrollBar.width, wordListScrollBar.height);
        shapeRenderer.rect(detailsScrollBar.x, detailsScrollBar.y, detailsScrollBar.width, detailsScrollBar.height);

        // Draw scroll thumbs
        shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1);
        shapeRenderer.rect(wordListScrollThumb.x, wordListScrollThumb.y, wordListScrollThumb.width, wordListScrollThumb.height);
        if (selectedWord != null) {
            shapeRenderer.rect(detailsScrollThumb.x, detailsScrollThumb.y, detailsScrollThumb.width, detailsScrollThumb.height);
        }

        // If there's a word selected, highlight it
        if (selectedWord != null) {
            int index = displayedWords.indexOf(selectedWord);
            if (index >= wordListStartIndex && index < wordListStartIndex + WORDS_PER_PAGE) {
                shapeRenderer.setColor(0.4f, 0.4f, 0.6f, 1);
                float y = wordListArea.y + wordListArea.height - 30 * (index - wordListStartIndex + 1);
                shapeRenderer.rect(wordListArea.x, y, wordListArea.width, 30);
            }
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Then draw text with the batch
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, 1280, 720));

        if(batch.isDrawing()) batch.end();
        batch.begin();

        // Title - use larger font if available
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "DICTIONARY", 640, 700, 0, Align.center, false);

        // Rest of your text drawing code with default font
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, searchText.isEmpty() ? "Search..." : searchText,
                searchBox.x + 10, searchBox.y + 20);
        labelFont.draw(batch, "Search", searchButton.x + 20, searchButton.y + 20);

        // Tabs
        labelFont.draw(batch, "Learned Words", learnedTab.x + 40, learnedTab.y + 20);
        labelFont.draw(batch, "New Words", newTab.x + 60, newTab.y + 20);

        // Back button
        labelFont.draw(batch, "Back", backButton.x + 30, backButton.y + 25);

        // Word list
        // Ensure wordListStartIndex is valid
        wordListStartIndex = Math.max(0, Math.min(wordListStartIndex, displayedWords.isEmpty() ? 0 : displayedWords.size() - 1));

        if (displayedWords.isEmpty()) {
            labelFont.draw(batch, "No words found", wordListArea.x + 20, wordListArea.y + wordListArea.height - 20);
        } else {
            for (int i = wordListStartIndex; i < Math.min(wordListStartIndex + WORDS_PER_PAGE, displayedWords.size()); i++) {
                Word word = displayedWords.get(i);
                float y = wordListArea.y + wordListArea.height - 30 * (i - wordListStartIndex + 1);
                labelFont.draw(batch, word.getTerm(), wordListArea.x + 10, y + 20);
            }
        }

        renderWordDetails(batch);
    }

    private void renderWordDetails(SpriteBatch batch) {
        if (selectedWord == null) return;

        if (!batch.isDrawing()) {
            batch.begin();
        }

        // Enable scissors to clip content to details area
        batch.flush();
        Rectangle scissors = new Rectangle(detailsArea.x, detailsArea.y, detailsArea.width, detailsArea.height);
        ScissorStack.pushScissors(scissors);

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
            }
        }

        // Mark as learned button for new words
        if (!showingLearnedWords) {
            labelFont.setColor(0.4f, 0.7f, 1, 1);
            labelFont.draw(batch, "[ Mark as Learned ]", detailsArea.x + 20, detailsArea.y + 30);
            labelFont.setColor(Color.WHITE);
        }

        // End scissor
        batch.flush();
        ScissorStack.popScissors();
    }

    // Calculate total height of word details to determine scrolling range
    private float calculateWordDetailsHeight() {
        if (selectedWord == null) return 0;

        float height = 60; // Basic padding + word term height

        if (selectedWord.getPronunciation() != null && !selectedWord.getPronunciation().isEmpty()) {
            height += 30;
        }

        if (!selectedWord.getDefinitions().isEmpty()) {
            height += 30; // "Definitions:" header

            for (WordDefinition def : selectedWord.getDefinitions()) {
                String defText = "• " + def.getPartOfSpeech() + ": " + def.getDefinition();
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
            }
        }

        return height;
    }

    public void dispose() {
        shapeRenderer.dispose();
        if (labelFont != null) {
            labelFont.dispose();
        }
    }

    private float calculateTextHeight(String text, int fontSize, float width) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(labelFont, text, Color.WHITE, width, com.badlogic.gdx.utils.Align.left, true);
        return layout.height;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    // ScissorStack helper class to clip rendering
    private static class ScissorStack {
        public static void pushScissors(Rectangle scissor) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(
                    (int)scissor.x,
                    (int)(Gdx.graphics.getHeight() - scissor.y - scissor.height),
                    (int)scissor.width,
                    (int)scissor.height);
        }

        public static void popScissors() {
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
    }
}