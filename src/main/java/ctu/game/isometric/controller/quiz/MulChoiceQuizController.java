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

import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class MulChoiceQuizController {
    private GameController gameController;
    private TimedQuizSystem quizSystem;
    private Map<String, Object> currentQuiz;
    private String selectedAnswer = "";
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
    private Rectangle[] optionButtons;

    public MulChoiceQuizController(GameController gameController) {
        this.gameController = gameController;
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator()
        );



        this.font = generateVietNameseFont("GrenzeGotisch.ttf", 24);
        this.shapeRenderer = new ShapeRenderer();

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        this.submitButton = new Rectangle(width * 0.35f, height * 0.15f, width * 0.3f, height * 0.08f);
        this.nextButton = new Rectangle(width * 0.25f, height * 0.15f, width * 0.2f, height * 0.08f);
        this.exitButton = new Rectangle(width * 0.55f, height * 0.15f, width * 0.2f, height * 0.08f);

        // Initialize option buttons (typically 4 options for multiple choice)
        this.optionButtons = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            // Calculate row and column for 2x2 grid
            int row = i / 2;     // 0 for top row (i=0,1), 1 for bottom row (i=2,3)
            int col = i % 2;     // 0 for left column (i=0,2), 1 for right column (i=1,3)

            this.optionButtons[i] = new Rectangle(
                    width * (0.2f + col * 0.35f),        // X: Left or right column
                    height * (0.6f - row * 0.15f) -110 ,       // Y: Top or bottom row
                    width * 0.26f,                        // Narrower width for 2 columns
                    height * 0.12f                        // Taller height for better readability
            );
        }
    }

    public void startQuiz() {
        // Refresh quiz system with current learned words
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator()
        );

        // Generate a multiple choice quiz instead of contextual sentence quiz
        currentQuiz = quizSystem.generateMultipleChoiceQuiz();
        quizSystem.startQuiz();
        quizActive = true;
        showingResults = false;
        selectedAnswer = "";
        completedQuestions = 0;
    }

    public void update(float delta) {
        if (!quizActive) return;

        // Auto-submit on time expiry
        if (quizSystem.isPendingAutoSubmit()) {
            quizSystem.resetPendingAutoSubmit();
            submitAnswer(); // Submit with current selected answer (might be empty)
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

    @SuppressWarnings("unchecked")
    private void renderQuiz(SpriteBatch batch, int width, int height) {
        if (currentQuiz == null) return;

        float centerX = width / 2;
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

        // Title
        font.setColor(Color.GOLD);
        layout.setText(font, "MULTIPLE CHOICE QUIZ");
        font.draw(batch, "MULTIPLE CHOICE QUIZ", centerX - layout.width / 2, height * 0.85f);

        // Show total score
        String totalScoreText = "Total Score: " + totalScore;
        layout.setText(font, totalScoreText);
        font.draw(batch, totalScoreText, 100, height * 0.85f);

        // Display difficulty level
        String difficultyText = "Difficulty: " + currentQuiz.get("difficultyLevel");
        font.setColor(Color.CYAN);
        layout.setText(font, difficultyText);
        font.draw(batch, difficultyText, 100, height * 0.78f);

        // Display question count correctly
        int total = quizSystem.getTotalQuestions();
        int current = completedQuestions + 1;
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

        // Draw multiple choice options
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Get options from quiz data
        List<String> options = (List<String>) currentQuiz.get("options");

        // Draw option buttons
        for (int i = 0; i < options.size(); i++) {
            Rectangle button = optionButtons[i];

            // Highlight selected option
            if (options.get(i).equals(selectedAnswer)) {
                shapeRenderer.setColor(0.4f, 0.6f, 0.9f, 1); // Blue highlight for selected
            } else {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1); // Normal color
            }

            shapeRenderer.rect(button.x, button.y, button.width, button.height);
        }

        // Submit button (only enabled if an option is selected)
        submitButton.y = height * 0.15f;
        shapeRenderer.setColor(selectedAnswer.isEmpty() ? 0.5f : 0.3f,
                selectedAnswer.isEmpty() ? 0.5f : 0.7f,
                selectedAnswer.isEmpty() ? 0.5f : 0.3f, 1);
        shapeRenderer.rect(submitButton.x, submitButton.y, submitButton.width, submitButton.height);
        shapeRenderer.end();

        batch.begin();

        // Draw option labels
        font.setColor(Color.WHITE);
        for (int i = 0; i < options.size(); i++) {
            Rectangle button = optionButtons[i];
            String option = options.get(i);

            layout.setText(font, option);
            font.draw(batch, option,
                    button.x + (button.width - layout.width) / 2,
                    button.y + (button.height + layout.height) / 2);
        }

        // Draw submit text
        font.setColor(Color.WHITE);
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

        // Show correct answer
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

        // Remaining question information
        int total = quizSystem.getTotalQuestions();
        String questionCountText = "Questions: " + completedQuestions + " completed / " + total + " total";
        font.setColor(Color.CYAN);
        layout.setText(font, questionCountText);
        font.draw(batch, questionCountText, centerX - layout.width / 2, height * 0.45f);

        // Draw buttons
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        nextButton.y = height * 0.2f;
        exitButton.y = height * 0.2f;

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
        if (!quizActive || showingResults || selectedAnswer.isEmpty()) return;

        lastResult = quizSystem.submitAnswer(selectedAnswer);

        // Make sure userAnswer is included in the result map
        if (!lastResult.containsKey("userAnswer")) {
            lastResult.put("userAnswer", selectedAnswer);
        }

        if (lastResult.containsKey("score")) {
            totalScore += (Integer) lastResult.get("score");
        }

        completedQuestions++;
        showingResults = true;
    }

    public void handleNextQuiz() {
        if (showingResults) {
            currentQuiz = quizSystem.generateMultipleChoiceQuiz();

            // Check if session is complete
            if (currentQuiz.containsKey("sessionComplete")) {
                exitQuiz();
                return;
            }

            quizSystem.startQuiz();
            showingResults = false;
            selectedAnswer = "";
        }
    }

    public void exitQuiz() {
        quizActive = false;
        gameController.getCharacter().setScore(totalScore);
        gameController.setState(GameState.EXPLORING);
        this.totalScore = 0;
        this.completedQuestions = 0;
    }

    @SuppressWarnings("unchecked")
    public void handleOptionClick(int x, int y) {
        if (showingResults || !quizActive) return;
        // Check if any option was clicked
        List<String> options = (List<String>) currentQuiz.get("options");
        for (int i = 0; i < options.size(); i++) {
            if (optionButtons[i].contains(x, y)) {
                selectedAnswer = options.get(i);
                return;
            }
        }
    }

    public boolean handleClick(int x, int y) {
        if (!quizActive) return false;
        y = Gdx.graphics.getHeight() - y; // Invert Y coordinate for UI

        System.out.printf("Clicked at: (%d, %d)\n", x, y);
        if (!showingResults) {
            // First check options
            handleOptionClick(x, y);

            // Then check submit button
            if (submitButton.contains(x, y) && !selectedAnswer.isEmpty()) {
                submitAnswer();
                return true;
            }
        } else { // Handle result screen buttons
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