package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.util.WordNetValidator;

import java.util.*;

public class TimedQuizSystem extends SymbolicQuizSystem implements QuizTimer.TimerCallback {
    private QuizTimer timer;
    private boolean timeExpired;
    private float defaultTimeLimit = 30f; // 30 seconds default

    // Track session questions
    private int maxQuestionsPerSession = 5;
    private int currentQuestionCount = 0;
    private Set<String> usedQuestions = new HashSet<>();
    private Map<String, Object> currentQuiz;

    // Auto-submit flag
    private boolean pendingAutoSubmit = false;

    public TimedQuizSystem(Set<String> learnedWords, WordNetValidator wordNetValidator,int numberOfQuestions) {
        super(learnedWords, wordNetValidator);
        this.timer = new QuizTimer(defaultTimeLimit, this);
        this.timeExpired = false;
        this.maxQuestionsPerSession = numberOfQuestions;
    }


    @Override
    public Map<String, Object> generateMultipleChoiceQuiz() {
        if (currentQuestionCount >= maxQuestionsPerSession) {
            Map<String, Object> endData = new HashMap<>();
            endData.put("sessionComplete", true);
            endData.put("message", "Quiz session complete!");
            return endData;
        }

        // Try to generate a non-repeating question (max 5 attempts)
        Map<String, Object> quizData = null;
        int attempts = 0;

        while (attempts < 1) {
            quizData = super.generateMultipleChoiceQuiz();
            if (quizData == null || quizData.containsKey("error")) {
                break; // If there's an error or quizData is null, stop trying
            }

            String question = (String) quizData.get("question");
            if (!usedQuestions.contains(question)) {
                // Found a new question
                usedQuestions.add(question);
                break;
            }
            attempts++;
        }

        // End session if we couldn't find a unique question or encountered errors
        if (quizData == null || quizData.containsKey("error") || attempts >= 1) {
            Map<String, Object> endData = new HashMap<>();
            endData.put("sessionComplete", true);
            endData.put("message", "No more unique questions available!");
            return endData;
        }

        // Add time limit to the quiz data based on difficulty
        int difficulty = (int) quizData.getOrDefault("difficulty", 3);
        float timeLimit = getTimeLimitForDifficulty(difficulty);
        quizData.put("timeLimit", timeLimit);
        quizData.put("difficultyLevel", getDifficultyLabel(difficulty));

        timer.reset();
        timeExpired = false;
        pendingAutoSubmit = false;

        // Store the current quiz
        currentQuiz = quizData;
        currentQuestionCount++;

        return quizData;
    }



    @Override
    public Map<String, Object> generateContextualSentenceQuiz() {
        // Check if we've reached the question limit
        if (currentQuestionCount >= maxQuestionsPerSession) {
            Map<String, Object> endData = new HashMap<>();
            endData.put("sessionComplete", true);
            endData.put("message", "Quiz session complete!");
            return endData;
        }

        // Try to generate a non-repeating question (max 5 attempts)
        Map<String, Object> quizData = null;
        int attempts = 0;

        while (attempts < 1) {
            quizData = super.generateContextualSentenceQuiz();

            if (quizData.containsKey("error")) {
                break; // If there's an error, stop trying
            }

            String question = (String) quizData.get("question");
            if (!usedQuestions.contains(question)) {
                // Found a new question
                usedQuestions.add(question);
                break;
            }
            attempts++;
        }

        // End session if we couldn't find a unique question or encountered errors
        if (quizData == null || quizData.containsKey("error") || attempts >= 2) {
            Map<String, Object> endData = new HashMap<>();
            endData.put("sessionComplete", true);
            endData.put("message", "No more unique questions available!");
            return endData;
        }

        // Add time limit to the quiz data based on difficulty
        int difficulty = (int) quizData.getOrDefault("difficulty", 3);
        float timeLimit = getTimeLimitForDifficulty(difficulty);
        quizData.put("timeLimit", timeLimit);
        quizData.put("difficultyLevel", getDifficultyLabel(difficulty));

        timer.reset();
        timeExpired = false;
        pendingAutoSubmit = false;

        // Store the current quiz
        currentQuiz = quizData;
        currentQuestionCount++;

        return quizData;
    }

    private String getDifficultyLabel(int difficulty) {
        switch(difficulty) {
            case 1: return "Easy";
            case 2: return "Medium";
            case 3: return "Standard";
            case 4: return "Hard";
            case 5: return "Expert";
            default: return "Standard";
        }
    }

    private Map<String, Object> createDefaultQuiz() {
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", "contextual_sentence");
        quizData.put("question", "Fill in the blank: The ____ is a common English greeting.");
        quizData.put("answer", "HELLO");
        quizData.put("difficulty", 1);
        quizData.put("points", 5);
        return quizData;
    }

    public void startQuiz() {
        timer.reset();
        timer.setTimeLimit(calculateTimeLimitForDifficulty());
        timer.start();
        timeExpired = false;
        pendingAutoSubmit = false;
    }

    public float calculateTimeLimitForDifficulty(){
        if (currentQuiz == null) {
            return defaultTimeLimit;
        }
        int difficulty = (int) currentQuiz.getOrDefault("difficulty", 3);
        return getTimeLimitForDifficulty(difficulty);
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

        // No points for wrong answers
        int score = 0;

        if (isCorrect) {
            int difficulty = (int) currentQuiz.getOrDefault("difficulty", 3);
            score = calculateScore(difficulty, timeTaken);

            // Apply time penalty if time expired
            if (timeExpired) {
                score = 0; // No points if time expired
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("score", score);
        result.put("timeTaken", timeTaken);
        result.put("timeExpired", timeExpired);
        result.put("correctAnswer", correctAnswer);
        result.put("userAnswer", answer);
        result.put("questionsRemaining", maxQuestionsPerSession - currentQuestionCount);

        return result;
    }

    private int calculateScore(int difficulty, float timeTaken) {
        // Base score based on difficulty
        int baseScore = (int) Math.round(difficulty * 5); // Base score is 1.5 times the difficulty level

        // Time bonus - faster answers get more points
        float timeLimit = getTimeLimitForDifficulty(difficulty);
        float timeRatio = Math.min(1.0f, timeTaken / timeLimit);
        float timeBonus = 1.0f - (timeRatio * 0.5f); // Up to 50% bonus for fast answers

        return Math.round(baseScore * timeBonus);
    }

    private float getTimeLimitForDifficulty(int difficulty) {
        // Harder questions get more time (opposite of previous implementation)
        // Base time is defaultTimeLimit, add 5 seconds for each difficulty level
        return defaultTimeLimit + ((difficulty - 1) * 5);
    }

    @Override
    public void onTimerTick(float timeRemaining) {
        // Update UI with remaining time - handled by renderer
    }

    @Override
    public void onTimerComplete() {
        timeExpired = true;
        pendingAutoSubmit = true;
        // Auto-submit with empty answer will be handled in the controller
    }

    public boolean isPendingAutoSubmit() {
        return pendingAutoSubmit;
    }

    public void resetPendingAutoSubmit() {
        pendingAutoSubmit = false;
    }

    public void setDefaultTimeLimit(float seconds) {
        this.defaultTimeLimit = seconds;
    }

    public void setMaxQuestionsPerSession(int count) {
        this.maxQuestionsPerSession = count;
    }

    public void resetSession() {
        currentQuestionCount = 0;
        usedQuestions.clear();
    }

    public QuizTimer getTimer() {
        return timer;
    }

    public int getRemainingQuestions() {
        return maxQuestionsPerSession - currentQuestionCount;
    }

    public int getTotalQuestions() {
        return maxQuestionsPerSession;
    }
}