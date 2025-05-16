package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.util.WordNetValidator;
import ctu.game.isometric.util.WordScorer;

import java.util.*;

public class TimedQuizSystem extends SymbolicQuizSystem implements QuizTimer.TimerCallback {
    private QuizTimer timer;
    private boolean timeExpired;
    private float defaultTimeLimit = 30f; // 30 seconds default

    public TimedQuizSystem(Set<String> learnedWords, WordNetValidator wordNetValidator) {
        super(learnedWords, wordNetValidator);
        this.timer = new QuizTimer(defaultTimeLimit, this);
        this.timeExpired = false;
    }

    @Override
    public Map<String, Object> generateContextualSentenceQuiz() {
        Map<String, Object> quizData = super.generateContextualSentenceQuiz();

        if (quizData.containsKey("error")) {
            // Handle error case with a default quiz
            quizData = createDefaultQuiz();
        }

        // Add time limit to the quiz data
        int difficulty = (int) quizData.getOrDefault("difficulty", 3);
        float timeLimit = getTimeLimitForDifficulty(difficulty);
        quizData.put("timeLimit", timeLimit);
        timer.reset();
        timeExpired = false;

        // Store the current quiz
        currentQuiz = quizData;

        return quizData;
    }

    private Map<String, Object> createDefaultQuiz() {
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", "contextual_sentence");
        quizData.put("question", "Fill in the blank: The ____ is a common English greeting.");
        quizData.put("answer", "HELLO");
        quizData.put("difficulty", 1);
        quizData.put("points", 10);
        return quizData;
    }

    public void startQuiz() {
        timer.reset();
        timer.start();
        timeExpired = false;
    }

    public Map<String, Object> submitAnswer(String answer) {
        float timeTaken = timer.getElapsedTime();
        timer.pause();

        if (currentQuiz == null) {
            // Return error if trying to submit answer without an active quiz
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "No active quiz");
            errorResult.put("correct", false);
            errorResult.put("score", 0);
            errorResult.put("timeTaken", timeTaken);
            return errorResult;
        }

        String correctAnswer = (String) currentQuiz.get("answer");

        // Thorough normalization: trim spaces and convert to uppercase for consistent comparison
        String normalizedCorrectAnswer = correctAnswer.trim().toUpperCase();
        String normalizedUserAnswer = answer.trim().toUpperCase();

        // Exact match after normalization
        boolean isCorrect = normalizedUserAnswer.equals(normalizedCorrectAnswer);

        int difficulty = (int) currentQuiz.getOrDefault("difficulty", 3);
        int score = WordScorer.calculateScore(isCorrect, difficulty, (long)(timeTaken * 1000));

        // Apply time penalty if time expired
        if (timeExpired) {
            score = Math.max(0, score / 2); // 50% penalty for expired time
        }

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("score", score);
        result.put("timeTaken", timeTaken);
        result.put("timeExpired", timeExpired);
        result.put("correctAnswer", correctAnswer);
        result.put("userAnswer", answer);

        return result;
    }

    private float getTimeLimitForDifficulty(int difficulty) {
        // Harder questions get less time
        return Math.max(10, defaultTimeLimit - ((difficulty - 1) * 5));
    }

    // Cache current quiz
    private Map<String, Object> currentQuiz;

    @Override
    public void onTimerTick(float timeRemaining) {
        // Update UI with remaining time - handled by renderer
    }

    @Override
    public void onTimerComplete() {
        timeExpired = true;
        // Auto-submit with empty answer if time expires
    }

    public void setDefaultTimeLimit(float seconds) {
        this.defaultTimeLimit = seconds;
    }

    public QuizTimer getTimer() {
        return timer;
    }
}