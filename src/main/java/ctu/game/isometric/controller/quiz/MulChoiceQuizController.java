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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private QuizCompletionListener quizCompletionListener;

    // Session tracking
    private static Set<String> sessionUsedQuestions = new HashSet<>();
    private static final int MAX_SESSION_QUESTIONS = 50;

    // Statistics
    private int commonQuizCount = 0;
    private int learnedWordQuizCount = 0;

    public void setQuizCompletionListener(QuizCompletionListener listener) {
        this.quizCompletionListener = listener;
    }

    public MulChoiceQuizController(GameController gameController) {
        this.gameController = gameController;
        this.font = generateVietNameseFont("Roboto-Black.ttf", 18);
        this.shapeRenderer = new ShapeRenderer();
        initializeUIComponents();
    }

    private void initializeUIComponents() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        this.submitButton = new Rectangle(width * 0.35f, height * 0.15f, width * 0.3f, height * 0.08f);
        this.nextButton = new Rectangle(width * 0.25f, height * 0.15f, width * 0.2f, height * 0.08f);
        this.exitButton = new Rectangle(width * 0.55f, height * 0.15f, width * 0.2f, height * 0.08f);

        this.optionButtons = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int col = i % 2;

            this.optionButtons[i] = new Rectangle(
                    width * (0.2f + col * 0.35f),
                    height * (0.6f - row * 0.15f) - 110,
                    width * 0.26f,
                    height * 0.12f
            );
        }
    }

    private void createNewQuizSystem(int numberOfQuestions) {
        this.quizSystem = new TimedQuizSystem(
                gameController.getCharacter().getLearnedWords(),
                gameController.getWordNetValidator(),
                numberOfQuestions
        );
    }

    public void startQuiz(int numberOfQuestions) {
        createNewQuizSystem(numberOfQuestions);
        cleanupSessionUsedQuestions();

        // Reset statistics
        commonQuizCount = 0;
        learnedWordQuizCount = 0;

        currentQuiz = generateQuizWithFallback();

        if (currentQuiz == null) {
            currentQuiz = createEmergencyQuiz();
        }

        quizSystem.startQuiz();
        quizActive = true;
        showingResults = false;
        selectedAnswer = "";
        completedQuestions = 0;

        updateQuizStats();

        System.out.println("=== QUIZ SESSION STARTED ===");
        System.out.println("Session used questions: " + sessionUsedQuestions.size());
        System.out.println("Global used questions: " + TimedQuizSystem.getUsedQuestionsCount());
        System.out.println("Available common quizzes: " + CommonQuizBank.getAvailableQuizzesCount());
    }

    private Map<String, Object> generateQuizWithFallback() {
        Map<String, Object> quiz = quizSystem.generateMultipleChoiceQuiz();

        if (quiz != null && !quiz.containsKey("error") && !quiz.containsKey("sessionComplete")) {
            String question = (String) quiz.get("question");

            // Double check for session duplicates
            if (sessionUsedQuestions.contains(question)) {
                System.out.println("Session duplicate detected, trying fallback...");

                // Try common quiz as fallback
                CommonQuizBank.CommonQuiz commonQuiz = CommonQuizBank.getRandomCommonQuiz();
                Map<String, Object> fallbackQuiz = commonQuiz.toQuizMap();
                String fallbackQuestion = (String) fallbackQuiz.get("question");

                if (!sessionUsedQuestions.contains(fallbackQuestion)) {
                    sessionUsedQuestions.add(fallbackQuestion);
                    return fallbackQuiz;
                }

                // If even common quiz is duplicate, force clear session cache
                sessionUsedQuestions.clear();
                sessionUsedQuestions.add(question);
                return quiz;
            } else {
                sessionUsedQuestions.add(question);
                return quiz;
            }
        }

        return quiz; // Return whatever we got (might contain error or sessionComplete)
    }

    private Map<String, Object> createEmergencyQuiz() {
        long timestamp = System.currentTimeMillis();
        Map<String, Object> emergency = new java.util.HashMap<>();
        emergency.put("type", "multiple_choice");
        emergency.put("question", "Emergency Quiz " + timestamp + ": What is a common greeting?");
        emergency.put("answer", "HELLO");
        emergency.put("options", java.util.Arrays.asList("HELLO", "GOODBYE", "THANKS", "SORRY"));
        emergency.put("difficulty", 1);
        emergency.put("points", 5);
        emergency.put("timeLimit", 30f);
        emergency.put("difficultyLevel", "Easy");
        emergency.put("isEmergencyQuiz", true);
        return emergency;
    }

    private void updateQuizStats() {
        if (currentQuiz != null) {
            if (currentQuiz.containsKey("isCommonQuiz")) {
                commonQuizCount++;
            } else {
                learnedWordQuizCount++;
            }
        }
    }

    private void cleanupSessionUsedQuestions() {
        if (sessionUsedQuestions.size() > MAX_SESSION_QUESTIONS) {
            int keepSize = (int) (MAX_SESSION_QUESTIONS * 0.6);
            Set<String> newSet = new HashSet<>();

            int count = 0;
            for (String question : sessionUsedQuestions) {
                if (count >= keepSize) break;
                newSet.add(question);
                count++;
            }

            sessionUsedQuestions = newSet;
            System.out.println("Cleaned session cache. Remaining: " + sessionUsedQuestions.size());
        }
    }

    public void update(float delta) {
        if (!quizActive) return;

        if (quizSystem.isPendingAutoSubmit()) {
            quizSystem.resetPendingAutoSubmit();
            submitAnswer();
        }
    }

    public void render(SpriteBatch batch) {
        if (!quizActive) return;

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        centerY = height / 2;

        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());
        boolean wasBatchDrawing = batch.isDrawing();
        if (wasBatchDrawing) {
            batch.end();
        }

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
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

        // Title with quiz type indicator
        font.setColor(Color.GOLD);
        String title = "MULTIPLE CHOICE QUIZ";
        if (currentQuiz.containsKey("isCommonQuiz")) {
            title += " (Common)";
        } else if (currentQuiz.containsKey("isEmergencyQuiz")) {
            title += " (Emergency)";
        }
        layout.setText(font, title);
        font.draw(batch, title, centerX - layout.width / 2, height * 0.85f);

        // Show total score
        String totalScoreText = "Total Score: " + totalScore;
        layout.setText(font, totalScoreText);
        font.draw(batch, totalScoreText, 100, height * 0.85f);

        // Display difficulty level
        String difficultyText = "Difficulty: " + currentQuiz.get("difficultyLevel");
        font.setColor(Color.CYAN);
        layout.setText(font, difficultyText);
        font.draw(batch, difficultyText, 100, height * 0.78f);

        // Display question count
        int total = quizSystem.getTotalQuestions();
        int current = completedQuestions + 1;
        String questionCountText = "Question: " + current + " / " + total;
        layout.setText(font, questionCountText);
        font.draw(batch, questionCountText, width - 100 - layout.width, height * 0.78f);

        // Quiz statistics
        font.setColor(Color.GRAY);
        String statsText = String.format("Stats: %d learned | %d common | Session: %d | Global: %d",
                learnedWordQuizCount, commonQuizCount, sessionUsedQuestions.size(), TimedQuizSystem.getUsedQuestionsCount());
        layout.setText(font, statsText);
        font.draw(batch, statsText, 100, height * 0.72f);

        // Question
        String question = (String) currentQuiz.get("question");
        font.setColor(Color.WHITE);
        font.draw(batch, question, width * 0.15f, height * 0.65f, width * 0.7f, 1, true);

        // Timer
        float timeRemaining = quizSystem.getTimer().getTimeRemaining();
        font.setColor(timeRemaining < 10 ? Color.RED : Color.WHITE);
        String timeText = "Time: " + String.format("%.1f", timeRemaining) + "s";
        layout.setText(font, timeText);
        font.draw(batch, timeText, width * 0.8f - layout.width, height * 0.85f);

        // Draw options
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        List<String> options = (List<String>) currentQuiz.get("options");
        for (int i = 0; i < options.size(); i++) {
            Rectangle button = optionButtons[i];

            if (options.get(i).equals(selectedAnswer)) {
                shapeRenderer.setColor(0.4f, 0.6f, 0.9f, 1);
            } else {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
            }

            shapeRenderer.rect(button.x, button.y, button.width, button.height);
        }

        // Submit button
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

        // Submit button text
        layout.setText(font, "Submit");
        float buttonTextY = submitButton.y + (submitButton.height + layout.height) / 2;
        font.draw(batch, "Submit",
                submitButton.x + (submitButton.width - layout.width) / 2,
                buttonTextY);

        // Warning text
        font.setColor(Color.RED);
        String unikeyText = "Turn off Unikey or other Vietnamese input methods to avoid issues.";
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

        // Quiz source indicator
        if (lastResult.containsKey("isCommonQuiz") && (Boolean) lastResult.get("isCommonQuiz")) {
            font.setColor(Color.ORANGE);
            String sourceText = "(From Common Quiz Bank)";
            layout.setText(font, sourceText);
            font.draw(batch, sourceText, centerX - layout.width / 2, height * 0.67f);
        }

        // Show correct answer
        String correctAnswer = (String) currentQuiz.get("answer");
        font.setColor(Color.YELLOW);
        String answerText = "Answer: " + correctAnswer;
        layout.setText(font, answerText);
        font.draw(batch, answerText, centerX - layout.width / 2, height * 0.62f);

        // User's answer if incorrect
        if (!correct) {
            font.setColor(Color.WHITE);
            String userAnswerText = "Your answer: " + (String) lastResult.get("userAnswer");
            layout.setText(font, userAnswerText);
            font.draw(batch, userAnswerText, centerX - layout.width / 2, height * 0.57f);
        }

        // Score
        font.setColor(Color.WHITE);
        String scoreText = "Score: " + score;
        layout.setText(font, scoreText);
        font.draw(batch, scoreText, centerX - layout.width / 2, height * 0.52f);

        // Time
        String timeText = "Time: " + String.format("%.1f", timeTaken) + "s";
        layout.setText(font, timeText);
        font.draw(batch, timeText, centerX - layout.width / 2, height * 0.47f);

        // Progress
        int total = quizSystem.getTotalQuestions();
        String questionCountText = "Progress: " + completedQuestions + " / " + total;
        font.setColor(Color.CYAN);
        layout.setText(font, questionCountText);
        font.draw(batch, questionCountText, centerX - layout.width / 2, height * 0.42f);

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
        String buttonText = (quizSystem.getRemainingQuestions() <= 0) ? "Complete Quiz" : "Next Question";
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

        // If no answer selected and time ran out, auto-select a wrong answer
        if (selectedAnswer.isEmpty()) {
            String correctAnswer = (String) currentQuiz.get("answer");
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) currentQuiz.get("options");

            // Find first option that's not the correct answer
            for (String option : options) {
                if (!option.equals(correctAnswer)) {
                    selectedAnswer = option;
                    break;
                }
            }

            // Fallback: if somehow all options are correct (shouldn't happen), use first option
            if (selectedAnswer.isEmpty() && !options.isEmpty()) {
                selectedAnswer = options.get(0);
            }
        }

        // Proceed with submission only if we have an answer
        if (selectedAnswer.isEmpty()) return;

        lastResult = quizSystem.submitAnswer(selectedAnswer);

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
            currentQuiz = generateQuizWithFallback();

            if (currentQuiz == null || currentQuiz.containsKey("sessionComplete")) {
                exitQuiz();
                return;
            }

            updateQuizStats();
            quizSystem.startQuiz();
            showingResults = false;
            selectedAnswer = "";
        }
    }

    public void exitQuiz() {
        quizActive = false;
        gameController.getCharacter().setScore(totalScore);
        gameController.setState(GameState.EXPLORING);

        boolean quizCompleted = this.totalScore > 0;
        if (quizCompletionListener != null) {
            quizCompletionListener.onQuizCompleted(quizCompleted);
        }

        System.out.println("=== QUIZ SESSION ENDED ===");
        System.out.println("Final Score: " + totalScore);
        System.out.println("Learned word quizzes: " + learnedWordQuizCount);
        System.out.println("Common quizzes used: " + commonQuizCount);
        System.out.println("Session questions used: " + sessionUsedQuestions.size());

        this.totalScore = 0;
        this.completedQuestions = 0;
        this.commonQuizCount = 0;
        this.learnedWordQuizCount = 0;
    }

    @SuppressWarnings("unchecked")
    public void handleOptionClick(int x, int y) {
        if (showingResults || !quizActive) return;

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
        y = Gdx.graphics.getHeight() - y;

        if (!showingResults) {
            handleOptionClick(x, y);

            if (submitButton.contains(x, y) && !selectedAnswer.isEmpty()) {
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
        return quizSystem != null ? quizSystem.getTimer() : null;
    }

    // Static utility methods
    public static void clearSessionUsedQuestions() {
        sessionUsedQuestions.clear();
    }

    public static void clearAllUsedQuestions() {
        sessionUsedQuestions.clear();
        TimedQuizSystem.clearGlobalUsedQuestions();
        CommonQuizBank.resetUsedQuizzes();
    }

    public static int getSessionUsedQuestionsCount() {
        return sessionUsedQuestions.size();
    }
}