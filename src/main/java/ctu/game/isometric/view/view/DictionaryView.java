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

    private List<Word> displayedWords = new ArrayList<>();
    private boolean showingLearnedWords = true;
    private Word selectedWord = null;
    private String searchText = "";
    private int wordListStartIndex = 0;
    private static final int WORDS_PER_PAGE = 14;
    private BitmapFont labelFont;

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
        handleInput();
        updateWordList();
    }



    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            if (learnedTab.contains(touchPos.x, touchPos.y)) {
                showingLearnedWords = true;
                updateWordList();
            } else if (newTab.contains(touchPos.x, touchPos.y)) {
                showingLearnedWords = false;
                updateWordList();
            } else if (searchButton.contains(touchPos.x, touchPos.y)) {
                searchWords();
            } else if (backButton.contains(touchPos.x, touchPos.y)) {
                gameController.setCurrentState(GameState.EXPLORING);
            } else if (wordListArea.contains(touchPos.x, touchPos.y)) {
                selectWordFromList(touchPos.y);
            } else if (!showingLearnedWords && selectedWord != null &&
                    touchPos.x >= detailsArea.x + 20 && touchPos.x <= detailsArea.x + 200 &&
                    touchPos.y >= detailsArea.y + 15 && touchPos.y <= detailsArea.y + 45) {
                dictionary.markWordAsLearned(selectedWord.getTerm());
                updateWordList();
                selectedWord = null;
            }
        }

        // Handle scrolling
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            if (wordListStartIndex > 0) {
                wordListStartIndex--;
            }
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            if (wordListStartIndex + WORDS_PER_PAGE < displayedWords.size()) {
                wordListStartIndex++;
            }
        }
    }

    private void selectWordFromList(float y) {
        int index = (int)((wordListArea.y + wordListArea.height - y) / 30) + wordListStartIndex;
        if (index >= 0 && index < displayedWords.size()) {
            selectedWord = displayedWords.get(index);
        }
    }

    private void updateWordList() {
        displayedWords.clear();
        Set<Word> words = showingLearnedWords ? dictionary.getLearnedWords() : dictionary.getNewWords();
        displayedWords.addAll(words);
        wordListStartIndex = 0;
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
        displayedWords.addAll(dictionary.searchWords(searchText));
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

        float y = detailsArea.y + detailsArea.height - 20;

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

        if (!batch.isDrawing()) {
            batch.end();
        }
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
}