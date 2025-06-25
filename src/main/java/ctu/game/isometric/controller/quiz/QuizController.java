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

import java.util.Map;

public class QuizController {
    private GameController gameController;
    private TimedQuizSystem quizSystem;
    private Map<String, Object> currentQuiz;
    private String currentAnswer = "";
    private boolean quizActive = false;
    private boolean showingResults = false;
    private Map<String, Object> lastResult;

    private int totalScore = 0;
    private int completedQuestions = 0;

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
                gameController.getWordNetValidator(),
                5 // Default to 5 questions per session
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


    public void startQuiz(int numQuestions) {
        // Refresh quiz system with current learned word
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator(),
                numQuestions
        );

        // Generate a new quiz
        currentQuiz = quizSystem.generateContextualSentenceQuiz();
        quizSystem.startQuiz();
        quizActive = true;
        showingResults = false;
        currentAnswer = "";
        completedQuestions = 0;
    }

    public void update(float delta) {
        if (!quizActive) return;

        // Auto-submit on time expiry
        if (quizSystem.isPendingAutoSubmit()) {
            quizSystem.resetPendingAutoSubmit();
            submitAnswer(); // Submit with current answer (might be empty)
        }
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

        float centerX = width / 2;
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

        // Title
        font.setColor(Color.GOLD);
        layout.setText(font, "FILL THE BLANK");
        font.draw(batch, "FILL THE BLANK", centerX - layout.width / 2, height * 0.85f);

        // Show total score
        String totalScoreText = "Total Score: " + totalScore;
        layout.setText(font, totalScoreText);
        font.draw(batch, totalScoreText, 100, height * 0.85f);

        // Display difficulty level
        String difficultyText = "Difficulty: " + currentQuiz.get("difficulty");
        font.setColor(Color.CYAN);
        layout.setText(font, difficultyText);
        font.draw(batch, difficultyText, 100, height * 0.78f);

        // Display question count correctly
        int total = quizSystem.getTotalQuestions();
        int current = completedQuestions + 1; // Current question is completedQuestions + 1
        String questionCountText = "Question: " + current + " / " + total;
        layout.setText(font, questionCountText);
        font.draw(batch, questionCountText, width - 100 - layout.width, height * 0.78f);

        // Question
        String question = (String) currentQuiz.get("question");
        font.setColor(Color.WHITE);
        font.draw(batch, question, width * 0.15f, height * 0.7f, width * 0.7f, 1, true);

        // Timer
        float timeRemaining = quizSystem.getTimer().getTimeRemaining();
        font.setColor(timeRemaining < 10 ? Color.RED : Color.WHITE);
        String timeText = "Time: " + String.format("%.1f", timeRemaining) + "s";
        layout.setText(font, timeText);
        font.draw(batch, timeText, width * 0.8f - layout.width, height * 0.85f);

        // Input field and submit button
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Input field
        float inputFieldX = width * 0.2f;
        float inputFieldY = height * 0.45f;
        float inputFieldWidth = width * 0.6f;
        float inputFieldHeight = height * 0.1f;
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(inputFieldX, inputFieldY, inputFieldWidth, inputFieldHeight);

        // Submit button
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
        String displayText = currentAnswer;

        // If the answer field is empty, show underscores representing each character
        if (displayText.isEmpty() && currentQuiz != null) {
            String correctAnswer = (String) currentQuiz.get("answer");
            StringBuilder underscores = new StringBuilder();
            for (int i = 0; i < correctAnswer.length(); i++) {
                underscores.append("_ ");
            }
            displayText = underscores.toString().trim();
            font.setColor(Color.GRAY); // Make underscores appear in gray
        }

        layout.setText(font, displayText);
        // Important: For vertical centering in LibGDX, we need to adjust for baseline
        float textY = inputFieldY + (inputFieldHeight + layout.height) / 2;
        font.draw(batch, displayText, centerX - layout.width / 2, textY);

        // Draw submit text (centered in button)
        font.setColor(Color.WHITE);
        layout.setText(font, "Submit");
        float buttonTextY = submitButton.y + (submitButton.height + layout.height) / 2;
        font.draw(batch, "Submit",
                submitButton.x + (submitButton.width - layout.width) / 2,
                buttonTextY);

        font.setColor(Color.RED);
        String unikeyText = "Turn off Unikey or other Vietnamese input methods to avoid issues.";
        // draw it at the bottom of the screen
        layout.setText(font, unikeyText);
        font.draw(batch, unikeyText,
                centerX - layout.width / 2,
                height * 0.1f + layout.height / 2);
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

        // Remaining question information - correctly display completed questions
        int total = quizSystem.getTotalQuestions();
        String questionCountText = "Questions: " + completedQuestions + " completed / " + total + " total";
        font.setColor(Color.CYAN);
        layout.setText(font, questionCountText);
        font.draw(batch, questionCountText, centerX - layout.width / 2, height * 0.45f);

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

        String buttonText = (total == 1) ? "Next Quiz" : "Complete Quiz";
        layout.setText(font, buttonText);
        float nextButtonTextY = nextButton.y + (nextButton.height + layout.height) / 2;
        font.draw(batch, buttonText,
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

        lastResult = quizSystem.submitAnswer(currentAnswer);

        // Make sure userAnswer is included in the result map
        if (!lastResult.containsKey("userAnswer")) {
            lastResult.put("userAnswer", currentAnswer);
        }

        if (lastResult.containsKey("score")) {
            totalScore += (Integer) lastResult.get("score");
        }

        // Increment completed questions when an answer is submitted
        completedQuestions++;

        showingResults = true;
    }


    public boolean handleClick(int x, int y) {
        if (!quizActive) return false;
        y = Gdx.graphics.getHeight() - y; // Invert Y coordinate for UI

        if (!showingResults) {
            // Check if submit button is clicked
            if (submitButton.contains(x, y)) {
                submitAnswer();
                return true;
            }
        } else {
            if (nextButton.contains(x, y)) {
                handleNextQuiz();
                return true;
            } else if (exitButton.contains(x, y)) {
                exitQuiz();
                return true;
            }
        }

        return false;
    }

    public void handleNextQuiz() {
        if (showingResults) {
            currentQuiz = quizSystem.generateContextualSentenceQuiz();

            // Check if session is complete
            if (currentQuiz.containsKey("sessionComplete")) {
                exitQuiz();
                return;
            }

            quizSystem.startQuiz();
            showingResults = false;
            currentAnswer = "";
        }
    }

    public void exitQuiz() {
        quizActive = false;
        gameController.getCharacter().setScore(totalScore);
        gameController.setState(GameState.EXPLORING);
        this.totalScore = 0;
        this.completedQuestions = 0;
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

    public QuizTimer getTimer() {
        return quizSystem.getTimer();
    }
}