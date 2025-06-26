package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.util.WordNetValidator;

import java.util.*;

public class TimedQuizSystem extends SymbolicQuizSystem implements QuizTimer.TimerCallback {
    private static final float DEFAULT_TIME_LIMIT = 30f;
    private static final int MAX_GENERATION_ATTEMPTS = 3;
    private static final int MAX_FALLBACK_ATTEMPTS = 2;

    private QuizTimer timer;
    private boolean timeExpired;
    private float defaultTimeLimit = DEFAULT_TIME_LIMIT;

    // Track session questions
    private int maxQuestionsPerSession = 5;
    private int currentQuestionCount = 0;
    private Map<String, Object> currentQuiz;

    // Auto-submit flag
    private boolean pendingAutoSubmit = false;

    // Static để giữ usedQuestions giữa các instance
    private static Set<String> globalUsedQuestions = new HashSet<>();
    private static final int MAX_USED_QUESTIONS_CACHE = 100;

    // Track common quiz usage
    private int commonMultipleChoiceUsedCount = 0;
    private int commonContextualUsedCount = 0;
    private static final int MAX_COMMON_QUIZ_PER_SESSION = 3;

    public TimedQuizSystem(Set<String> learnedWords, WordNetValidator wordNetValidator, int numberOfQuestions) {
        super(learnedWords, wordNetValidator);
        this.timer = new QuizTimer(defaultTimeLimit, this);
        this.timeExpired = false;
        this.maxQuestionsPerSession = numberOfQuestions;

        // Cleanup cache nếu quá lớn
        cleanupUsedQuestionsCache();

        System.out.println("TimedQuizSystem created. Global used questions: " + globalUsedQuestions.size());
        System.out.println("Available common multiple choice quizzes: " + CommonQuizBank.getAvailableMultipleChoiceQuizzesCount());
        System.out.println("Available common contextual quizzes: " + CommonQuizBank.getAvailableContextualQuizzesCount());
    }

    private void cleanupUsedQuestionsCache() {
        if (globalUsedQuestions.size() > MAX_USED_QUESTIONS_CACHE) {
            int keepSize = (int) (MAX_USED_QUESTIONS_CACHE * 0.6);
            List<String> questionsList = new ArrayList<>(globalUsedQuestions);
            globalUsedQuestions.clear();

            if (questionsList.size() > keepSize) {
                globalUsedQuestions.addAll(questionsList.subList(questionsList.size() - keepSize, questionsList.size()));
            } else {
                globalUsedQuestions.addAll(questionsList);
            }
            System.out.println("Cleaned up global used questions. Remaining: " + globalUsedQuestions.size());
        }
    }

    @Override
    public Map<String, Object> generateMultipleChoiceQuiz() {
        if (currentQuestionCount >= maxQuestionsPerSession) {
            return createSessionCompleteResponse("Quiz session complete!");
        }

        // Thử generate từ learned words trước
        Map<String, Object> quizData = generateUniqueQuizFromLearnedWords(true);

        if (quizData != null && !quizData.containsKey("error")) {
            setupQuizSession(quizData);
            return quizData;
        }

        // Fallback to common multiple choice quiz
        System.out.println("Falling back to common multiple choice quiz. Attempt: " + (commonMultipleChoiceUsedCount + 1));
        return generateCommonMultipleChoiceQuiz();
    }

    @Override
    public Map<String, Object> generateContextualSentenceQuiz() {
        if (currentQuestionCount >= maxQuestionsPerSession) {
            return createSessionCompleteResponse("Quiz session complete!");
        }

        // Thử generate từ learned words trước
        Map<String, Object> quizData = generateUniqueQuizFromLearnedWords(false);

        if (quizData != null && !quizData.containsKey("error")) {
            setupQuizSession(quizData);
            return quizData;
        }

        // Fallback to common contextual quiz
        System.out.println("Falling back to common contextual quiz. Attempt: " + (commonContextualUsedCount + 1));
        return generateCommonContextualQuiz();
    }

    private Map<String, Object> generateUniqueQuizFromLearnedWords(boolean isMultipleChoice) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                Map<String, Object> quizData;

                if (isMultipleChoice) {
                    quizData = super.generateMultipleChoiceQuiz();
                } else {
                    quizData = super.generateContextualSentenceQuiz();
                }

                if (quizData == null || quizData.containsKey("error")) {
                    String quizType = isMultipleChoice ? "multiple choice" : "contextual";
                    System.out.println(quizType + " generation attempt " + (attempt + 1) + " failed: " +
                            (quizData != null ? quizData.get("message") : "null quiz"));
                    continue;
                }

                String question = (String) quizData.get("question");
                if (question == null || question.trim().isEmpty()) {
                    System.out.println("Generated question is empty");
                    continue;
                }

                if (!globalUsedQuestions.contains(question)) {
                    globalUsedQuestions.add(question);
                    String quizType = isMultipleChoice ? "multiple choice" : "contextual";
                    System.out.println("Generated unique " + quizType + " question from learned words");
                    return quizData;
                } else {
                    System.out.println("Question already used: " + question.substring(0, Math.min(50, question.length())) + "...");
                }
            } catch (Exception e) {
                String quizType = isMultipleChoice ? "multiple choice" : "contextual";
                System.err.println("Error generating " + quizType + " quiz attempt " + (attempt + 1) + ": " + e.getMessage());
            }
        }

        return null;
    }

    private Map<String, Object> generateCommonMultipleChoiceQuiz() {
        try {
            CommonQuizBank.CommonQuiz commonQuiz = CommonQuizBank.getRandomMultipleChoiceQuiz();
            Map<String, Object> quizData = commonQuiz.toQuizMap();

            String question = (String) quizData.get("question");

            // Kiểm tra xem common quiz này đã dùng chưa
            for (int attempt = 0; attempt < MAX_FALLBACK_ATTEMPTS; attempt++) {
                if (!globalUsedQuestions.contains(question)) {
                    globalUsedQuestions.add(question);
                    commonMultipleChoiceUsedCount++;
                    setupQuizSession(quizData);
                    System.out.println("Using common multiple choice quiz: " + question.substring(0, Math.min(50, question.length())) + "...");
                    return quizData;
                }

                // Thử lấy common quiz khác
                commonQuiz = CommonQuizBank.getRandomMultipleChoiceQuiz();
                quizData = commonQuiz.toQuizMap();
                question = (String) quizData.get("question");
            }

            // Nếu vẫn trùng, force sử dụng
            if (CommonQuizBank.getAvailableMultipleChoiceQuizzesCount() == 0) {
                CommonQuizBank.resetUsedMultipleChoiceQuizzes();
                commonQuiz = CommonQuizBank.getRandomMultipleChoiceQuiz();
                quizData = commonQuiz.toQuizMap();
            }

            globalUsedQuestions.add((String) quizData.get("question"));
            commonMultipleChoiceUsedCount++;
            setupQuizSession(quizData);
            System.out.println("Force using common multiple choice quiz (potential duplicate)");
            return quizData;

        } catch (Exception e) {
            System.err.println("Error generating common multiple choice quiz: " + e.getMessage());
            return createEmergencyQuiz(true);
        }
    }

    private Map<String, Object> generateCommonContextualQuiz() {
        try {
            CommonQuizBank.CommonQuiz commonQuiz = CommonQuizBank.getRandomContextualQuiz();
            Map<String, Object> quizData = commonQuiz.toQuizMap();

            String question = (String) quizData.get("question");

            // Kiểm tra xem common quiz này đã dùng chưa
            for (int attempt = 0; attempt < MAX_FALLBACK_ATTEMPTS; attempt++) {
                if (!globalUsedQuestions.contains(question)) {
                    globalUsedQuestions.add(question);
                    commonContextualUsedCount++;
                    setupQuizSession(quizData);
                    System.out.println("Using common contextual quiz: " + question.substring(0, Math.min(50, question.length())) + "...");
                    return quizData;
                }

                // Thử lấy common quiz khác
                commonQuiz = CommonQuizBank.getRandomContextualQuiz();
                quizData = commonQuiz.toQuizMap();
                question = (String) quizData.get("question");
            }

            // Nếu vẫn trùng, force sử dụng
            if (CommonQuizBank.getAvailableContextualQuizzesCount() == 0) {
                CommonQuizBank.resetUsedContextualQuizzes();
                commonQuiz = CommonQuizBank.getRandomContextualQuiz();
                quizData = commonQuiz.toQuizMap();
            }

            globalUsedQuestions.add((String) quizData.get("question"));
            commonContextualUsedCount++;
            setupQuizSession(quizData);
            System.out.println("Force using common contextual quiz (potential duplicate)");
            return quizData;

        } catch (Exception e) {
            System.err.println("Error generating common contextual quiz: " + e.getMessage());
            return createEmergencyQuiz(false);
        }
    }

    private Map<String, Object> createEmergencyQuiz(boolean isMultipleChoice) {
        Map<String, Object> quizData = new HashMap<>();
        long timestamp = System.currentTimeMillis();

        if (isMultipleChoice) {
            quizData.put("type", "multiple_choice");
            quizData.put("question", "Emergency Multiple Choice Quiz: Which word means 'greeting'? (ID: " + timestamp + ")");
            quizData.put("answer", "HELLO");
            quizData.put("options", Arrays.asList("HELLO", "GOODBYE", "THANKS", "SORRY"));
        } else {
            quizData.put("type", "contextual_sentence");
            quizData.put("question", "Emergency Contextual Quiz: Fill in the blank: ____ is a common greeting. (ID: " + timestamp + ")");
            quizData.put("answer", "HELLO");
        }

        quizData.put("difficulty", 1);
        quizData.put("points", 5);
        quizData.put("timeLimit", defaultTimeLimit);
        quizData.put("difficultyLevel", "Easy");
        quizData.put("isEmergencyQuiz", true);

        setupQuizSession(quizData);
        System.out.println("Created emergency " + (isMultipleChoice ? "multiple choice" : "contextual") + " quiz");
        return quizData;
    }

    private void setupQuizSession(Map<String, Object> quizData) {
        int difficulty = (int) quizData.getOrDefault("difficulty", 3);
        float timeLimit = getTimeLimitForDifficulty(difficulty);

        if (!quizData.containsKey("timeLimit")) {
            quizData.put("timeLimit", timeLimit);
        }
        if (!quizData.containsKey("difficultyLevel")) {
            quizData.put("difficultyLevel", getDifficultyLabel(difficulty));
        }

        timer.reset();
        timeExpired = false;
        pendingAutoSubmit = false;

        currentQuiz = quizData;
        currentQuestionCount++;
    }

    private Map<String, Object> createSessionCompleteResponse(String message) {
        Map<String, Object> endData = new HashMap<>();
        endData.put("sessionComplete", true);
        endData.put("message", message);
        return endData;
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

    // Backward compatibility method
    public Map<String, Object> createCommonQuiz() {
        return generateCommonMultipleChoiceQuiz();
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
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "No active quiz");
            errorResult.put("correct", false);
            errorResult.put("score", 0);
            errorResult.put("timeTaken", timeTaken);
            return errorResult;
        }

        String correctAnswer = (String) currentQuiz.get("answer");
        String normalizedCorrectAnswer = correctAnswer.trim().toUpperCase();
        String normalizedUserAnswer = answer.trim().toUpperCase();
        boolean isCorrect = normalizedUserAnswer.equals(normalizedCorrectAnswer);

        int score = 0;
        if (isCorrect) {
            int difficulty = (int) currentQuiz.getOrDefault("difficulty", 3);
            score = calculateScore(difficulty, timeTaken);

            if (timeExpired) {
                score = 0;
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
        result.put("isCommonQuiz", currentQuiz.containsKey("isCommonQuiz"));

        return result;
    }

    private int calculateScore(int difficulty, float timeTaken) {
        int baseScore = (int) Math.round(difficulty * 5);
        float timeLimit = getTimeLimitForDifficulty(difficulty);
        float timeRatio = Math.min(1.0f, timeTaken / timeLimit);
        float timeBonus = 1.0f - (timeRatio * 0.5f);
        return Math.round(baseScore * timeBonus);
    }

    private float getTimeLimitForDifficulty(int difficulty) {
        return defaultTimeLimit + ((difficulty - 1) * 5);
    }

    @Override
    public void onTimerTick(float timeRemaining) {
        // Update UI with remaining time
    }

    @Override
    public void onTimerComplete() {
        timeExpired = true;
        pendingAutoSubmit = true;
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
        commonMultipleChoiceUsedCount = 0;
        commonContextualUsedCount = 0;
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

    // Static methods to manage global state
    public static void clearGlobalUsedQuestions() {
        globalUsedQuestions.clear();
        CommonQuizBank.resetAllUsedQuizzes();
        System.out.println("Cleared all used questions (global + all common types)");
    }

    public static int getUsedQuestionsCount() {
        return globalUsedQuestions.size();
    }

    public static Set<String> getGlobalUsedQuestions() {
        return new HashSet<>(globalUsedQuestions);
    }

    public int getCommonMultipleChoiceUsedCount() {
        return commonMultipleChoiceUsedCount;
    }

    public int getCommonContextualUsedCount() {
        return commonContextualUsedCount;
    }

    public static void resetAllCaches() {
        globalUsedQuestions.clear();
        CommonQuizBank.resetAllUsedQuizzes();
        System.out.println("Reset all quiz caches (including contextual)");
    }
}