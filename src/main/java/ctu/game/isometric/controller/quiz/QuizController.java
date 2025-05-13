package ctu.game.isometric.controller.quiz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.QuizTimer;
import ctu.game.isometric.util.TimedQuizSystem;

import java.util.Map;

public class QuizController {
    private GameController gameController;
    private TimedQuizSystem quizSystem;
    private Map<String, Object> currentQuiz;
    private String currentAnswer = "";
    private boolean quizActive = false;
    private boolean showingResults = false;
    private Map<String, Object> lastResult;
    private float centerY;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    // UI elements
    private Rectangle submitButton;
    private Rectangle nextButton;
    private Rectangle exitButton;

    public QuizController(GameController gameController) {
        this.gameController = gameController;
        // Initialize with current learned words
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator()
        );

        this.font = new BitmapFont();
        this.font.getData().setScale(1.5f);
        this.shapeRenderer = new ShapeRenderer();

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        this.submitButton = new Rectangle(width * 0.35f, height * 0.25f, width * 0.3f, height * 0.08f);
        this.nextButton = new Rectangle(width * 0.25f, height * 0.25f, width * 0.2f, height * 0.08f);
        this.exitButton = new Rectangle(width * 0.55f, height * 0.25f, width * 0.2f, height * 0.08f);
    }

    public void startQuiz() {
        // Refresh quiz system with current learned words
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator()
        );

        // Generate a new quiz
        currentQuiz = quizSystem.generateContextualSentenceQuiz();
        quizSystem.startQuiz();
        quizActive = true;
        showingResults = false;
        currentAnswer = "";
    }

    public void update(float delta) {
        if (!quizActive) return;
    }

    public void render(SpriteBatch batch) {
        if (!quizActive) return;

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        centerY = height / 2;

        // Save original projection matrix
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        // End current batch if needed
        boolean wasBatchDrawing = batch.isDrawing();
        if (wasBatchDrawing) {
            batch.end();
        }

        // Set up orthographic projection for UI
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));

        // Apply same projection to shapeRenderer
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // Draw background
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(width * 0.1f, height * 0.1f, width * 0.8f, height * 0.8f);
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        batch.begin();

        if (showingResults) {
            renderResults(batch, width, height);
        } else {
            renderQuiz(batch, width, height);
        }

        // Restore original projection
        batch.setProjectionMatrix(originalMatrix);

        if (!wasBatchDrawing) {
            batch.end();
        }
    }

    private void renderQuiz(SpriteBatch batch, int width, int height) {
        if (currentQuiz == null) return;

        // Panel dimensions
        float panelX = width * 0.1f;
        float panelY = height * 0.1f;
        float panelWidth = width * 0.8f;
        float panelHeight = height * 0.8f;
        float centerX = width / 2;

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

        // Title
        font.setColor(Color.GOLD);
        layout.setText(font, "FILL THE BLANK");
        font.draw(batch, "FILL THE BLANK", centerX - layout.width / 2, height * 0.85f);

        // Question
        String question = (String) currentQuiz.get("question");
        font.setColor(Color.WHITE);
        font.draw(batch, question, width * 0.15f, height * 0.7f, width * 0.7f, 1, true);

        // Timer
        float timeRemaining = quizSystem.getTimer().getTimeRemaining();
        font.setColor(timeRemaining < 10 ? Color.RED : Color.WHITE);
        String timeText = "Time: " + String.format("%.1f", timeRemaining);
        layout.setText(font, timeText);
        font.draw(batch, timeText, width * 0.8f - layout.width, height * 0.85f);

        // Draw answer input area
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Input field - position directly based on screen dimensions
        float inputFieldX = width * 0.2f;
        float inputFieldY = height * 0.45f;
        float inputFieldWidth = width * 0.6f;
        float inputFieldHeight = height * 0.1f;
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(inputFieldX, inputFieldY, inputFieldWidth, inputFieldHeight);

        // Submit button - redefine with exact coordinates
        submitButton.x = width * 0.35f;
        submitButton.y = height * 0.25f;
        submitButton.width = width * 0.3f;
        submitButton.height = height * 0.08f;
        shapeRenderer.setColor(0.3f, 0.7f, 0.3f, 1);
        shapeRenderer.rect(submitButton.x, submitButton.y, submitButton.width, submitButton.height);
        shapeRenderer.end();

        batch.begin();

        // Draw answer (centered in input box)
        font.setColor(Color.WHITE);
        layout.setText(font, currentAnswer);
        // Important: For vertical centering in LibGDX, we need to adjust for baseline
        float textY = inputFieldY + (inputFieldHeight + layout.height) / 2;
        font.draw(batch, currentAnswer, centerX - layout.width / 2, textY);

        // Draw submit text (centered in button)
        layout.setText(font, "Submit");
        float buttonTextY = submitButton.y + (submitButton.height + layout.height) / 2;
        font.draw(batch, "Submit",
                submitButton.x + (submitButton.width - layout.width) / 2,
                buttonTextY);
    }

    private void renderResults(SpriteBatch batch, int width, int height) {
        if (lastResult == null) return;

        float centerX = width / 2;
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

        // Title
        font.setColor(Color.GOLD);
        layout.setText(font, "QUIZ RESULTS");
        font.draw(batch, "QUIZ RESULTS", centerX - layout.width / 2, height * 0.85f);

        // Results
        boolean correct = (Boolean) lastResult.get("correct");
        int score = (Integer) lastResult.get("score");
        float timeTaken = (Float) lastResult.get("timeTaken");

        // Result status
        String resultText = correct ? "Correct!" : "Incorrect";
        font.setColor(correct ? Color.GREEN : Color.RED);
        layout.setText(font, resultText);
        font.draw(batch, resultText, centerX - layout.width / 2, height * 0.7f);

        // Show correct answer (especially important if the user was incorrect)
        String correctAnswer = (String) currentQuiz.get("answer");
        font.setColor(Color.YELLOW);
        String answerText = "Answer: " + correctAnswer;
        layout.setText(font, answerText);
        font.draw(batch, answerText, centerX - layout.width / 2, height * 0.65f);

        // User's answer if incorrect
        if (!correct) {
            font.setColor(Color.WHITE);
            String userAnswerText = "Your answer: " + (String) lastResult.get("userAnswer");
            layout.setText(font, userAnswerText);
            font.draw(batch, userAnswerText, centerX - layout.width / 2, height * 0.6f);
        }

        // Score
        font.setColor(Color.WHITE);
        String scoreText = "Score: " + score;
        layout.setText(font, scoreText);
        font.draw(batch, scoreText, centerX - layout.width / 2, height * 0.55f);

        // Time
        String timeText = "Time: " + String.format("%.1f", timeTaken) + "s";
        layout.setText(font, timeText);
        font.draw(batch, timeText, centerX - layout.width / 2, height * 0.5f);

        // Draw buttons - redefine with exact coordinates
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Button positions adjusted to accommodate new text elements
        nextButton.x = width * 0.25f;
        nextButton.y = height * 0.2f;
        nextButton.width = width * 0.2f;
        nextButton.height = height * 0.08f;

        exitButton.x = width * 0.55f;
        exitButton.y = height * 0.2f;
        exitButton.width = width * 0.2f;
        exitButton.height = height * 0.08f;

        shapeRenderer.setColor(0.3f, 0.7f, 0.3f, 1);
        shapeRenderer.rect(nextButton.x, nextButton.y, nextButton.width, nextButton.height);
        shapeRenderer.setColor(0.7f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);
        shapeRenderer.end();

        batch.begin();

        // Button text
        font.setColor(Color.WHITE);
        layout.setText(font, "Next Quiz");
        float nextButtonTextY = nextButton.y + (nextButton.height + layout.height) / 2;
        font.draw(batch, "Next Quiz",
                nextButton.x + (nextButton.width - layout.width) / 2,
                nextButtonTextY);

        layout.setText(font, "Exit");
        float exitButtonTextY = exitButton.y + (exitButton.height + layout.height) / 2;
        font.draw(batch, "Exit",
                exitButton.x + (exitButton.width - layout.width) / 2,
                exitButtonTextY);
    }

    public void submitAnswer() {
        if (!quizActive || showingResults) return;

        // Compare with correct answer
        String correctAnswer = (String) currentQuiz.get("answer");
        boolean isCorrect = currentAnswer.trim().equalsIgnoreCase(correctAnswer);

        lastResult = quizSystem.submitAnswer(currentAnswer);

        // Make sure userAnswer is included in the result map
        if (!lastResult.containsKey("userAnswer")) {
            lastResult.put("userAnswer", currentAnswer);
        }

        showingResults = true;
    }

    public void handleNextQuiz() {
        if (showingResults) {
            startQuiz();
        }
    }

    public void exitQuiz() {
        quizActive = false;
        gameController.setState(GameState.EXPLORING);
    }

    public void processInput(char character) {
        if (showingResults) return;
        currentAnswer += character;
    }

    public void backspace() {
        if (showingResults || currentAnswer.isEmpty()) return;
        currentAnswer = currentAnswer.substring(0, currentAnswer.length() - 1);
    }

    public boolean isQuizActive() {
        return quizActive;
    }

    public boolean isShowingResults() {
        return showingResults;
    }

    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }

    // Add getters as needed
    public QuizTimer getTimer() {
        return quizSystem.getTimer();
    }
}