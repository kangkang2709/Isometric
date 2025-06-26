package ctu.game.isometric.controller.quiz;

import java.util.*;

public class CommonQuizBank {
    private static final List<CommonQuiz> COMMON_MULTIPLE_CHOICE_QUIZZES = new ArrayList<>();
    private static final List<CommonQuiz> COMMON_CONTEXTUAL_QUIZZES = new ArrayList<>();
    private static final Random random = new Random();
    private static Set<Integer> usedMultipleChoiceQuizzes = new HashSet<>();
    private static Set<Integer> usedContextualQuizzes = new HashSet<>();

    static {
        initializeCommonQuizzes();
    }

    private static void initializeCommonQuizzes() {
        initializeMultipleChoiceQuizzes();
        initializeContextualQuizzes();
    }

    private static void initializeMultipleChoiceQuizzes() {
        // Basic English vocabulary quizzes
        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "Which word means 'a greeting used when meeting someone'?",
                "HELLO",
                Arrays.asList("HELLO", "GOODBYE", "THANKS", "SORRY"),
                1
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "What do you say when you leave someone?",
                "GOODBYE",
                Arrays.asList("HELLO", "GOODBYE", "THANKS", "MORNING"),
                1
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "Which word expresses gratitude?",
                "THANKS",
                Arrays.asList("PLEASE", "SORRY", "THANKS", "WELCOME"),
                1
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "What word is used to apologize?",
                "SORRY",
                Arrays.asList("HAPPY", "SORRY", "ANGRY", "TIRED"),
                1
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "Which word means 'large in size'?",
                "BIG",
                Arrays.asList("SMALL", "BIG", "TINY", "SHORT"),
                2
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "What is the opposite of 'hot'?",
                "COLD",
                Arrays.asList("WARM", "COOL", "COLD", "FREEZE"),
                2
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "Which word means 'very pleased or satisfied'?",
                "HAPPY",
                Arrays.asList("SAD", "ANGRY", "HAPPY", "WORRIED"),
                2
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "What do you call the meal eaten in the morning?",
                "BREAKFAST",
                Arrays.asList("LUNCH", "DINNER", "BREAKFAST", "SNACK"),
                2
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "Which word describes moving at high speed?",
                "FAST",
                Arrays.asList("SLOW", "FAST", "STOP", "WALK"),
                2
        ));

        COMMON_MULTIPLE_CHOICE_QUIZZES.add(new CommonQuiz(
                "What is a young dog called?",
                "PUPPY",
                Arrays.asList("KITTEN", "PUPPY", "CALF", "LAMB"),
                3
        ));

        // Add more multiple choice quizzes...
        System.out.println("Initialized " + COMMON_MULTIPLE_CHOICE_QUIZZES.size() + " common multiple choice quizzes");
    }

    private static void initializeContextualQuizzes() {
        // Easy level contextual quizzes (difficulty 1)
        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in the blank: ____ morning! How are you today?",
                "GOOD",
                null, // No options for contextual
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete the sentence: I say ____ when I meet someone for the first time.",
                "HELLO",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in the blank: When someone helps me, I say ____.",
                "THANKS",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: When I make a mistake, I say ____.",
                "SORRY",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: When I leave, I say ____.",
                "GOODBYE",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: The opposite of small is ____.",
                "BIG",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: The color of the sun is ____.",
                "YELLOW",
                null,
                1
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: I use my ____ to see things.",
                "EYES",
                null,
                1
        ));

        // Medium level contextual quizzes (difficulty 2)
        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in the blank: When the weather is not hot, it is ____.",
                "COLD",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: When I feel joy and satisfaction, I am ____.",
                "HAPPY",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: The first meal of the day is called ____.",
                "BREAKFAST",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: When someone moves quickly, they are ____.",
                "FAST",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: A baby cat is called a ____.",
                "KITTEN",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: When it rains, ____ falls from the sky.",
                "WATER",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: The king of the jungle is the ____.",
                "LION",
                null,
                2
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: You mix red and white to get ____.",
                "PINK",
                null,
                2
        ));

        // Standard level contextual quizzes (difficulty 3)
        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: A baby dog is called a ____.",
                "PUPPY",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: Something extremely large can be described as ____.",
                "HUGE",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: A person who educates students is a ____.",
                "TEACHER",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: Someone with great knowledge is ____.",
                "SMART",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: Your father's father is your ____.",
                "GRANDFATHER",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: Having a lot of money means being ____.",
                "RICH",
                null,
                3
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: There are ____ hours in a day.",
                "TWENTY-FOUR",
                null,
                3
        ));

        // Hard level contextual quizzes (difficulty 4)
        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: The past tense of 'go' is ____.",
                "WENT",
                null,
                4
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: To make something clean, you ____ it.",
                "WASH",
                null,
                4
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: The leader of a country is often called the ____.",
                "PRESIDENT",
                null,
                4
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: When you carefully examine something, you ____ it.",
                "INVESTIGATE",
                null,
                4
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: Frozen rain that falls in winter is called ____.",
                "SNOW",
                null,
                4
        ));

        // Expert level contextual quizzes (difficulty 5)
        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: Someone who shows bravery and fearlessness is ____.",
                "COURAGEOUS",
                null,
                5
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: A person who writes books professionally is an ____.",
                "AUTHOR",
                null,
                5
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Fill in: To look into something systematically means to ____ it.",
                "INVESTIGATE",
                null,
                5
        ));

        COMMON_CONTEXTUAL_QUIZZES.add(new CommonQuiz(
                "Complete: Someone who acts with great courage can be called ____.",
                "BRAVE",
                null,
                5
        ));

        System.out.println("Initialized " + COMMON_CONTEXTUAL_QUIZZES.size() + " common contextual quizzes");
    }

    // Multiple Choice Quiz Methods
    public static CommonQuiz getRandomMultipleChoiceQuiz() {
        return getRandomQuizFromList(COMMON_MULTIPLE_CHOICE_QUIZZES, usedMultipleChoiceQuizzes, "multiple choice");
    }

    public static CommonQuiz getMultipleChoiceQuizByDifficulty(int difficulty) {
        return getQuizByDifficultyFromList(COMMON_MULTIPLE_CHOICE_QUIZZES, usedMultipleChoiceQuizzes, difficulty, "multiple choice");
    }

    // Contextual Quiz Methods
    public static CommonQuiz getRandomContextualQuiz() {
        return getRandomQuizFromList(COMMON_CONTEXTUAL_QUIZZES, usedContextualQuizzes, "contextual");
    }

    public static CommonQuiz getContextualQuizByDifficulty(int difficulty) {
        return getQuizByDifficultyFromList(COMMON_CONTEXTUAL_QUIZZES, usedContextualQuizzes, difficulty, "contextual");
    }

    // Generic helper methods
    private static CommonQuiz getRandomQuizFromList(List<CommonQuiz> quizList, Set<Integer> usedQuizzes, String type) {
        if (quizList.isEmpty()) {
            return getDefaultContextualQuiz();
        }

        // Reset if all quizzes used
        if (usedQuizzes.size() >= quizList.size()) {
            usedQuizzes.clear();
            System.out.println("Reset " + type + " quiz usage - all questions used");
        }

        // Find available quizzes
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < quizList.size(); i++) {
            if (!usedQuizzes.contains(i)) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            usedQuizzes.clear();
            availableIndices.add(0);
        }

        int selectedIndex = availableIndices.get(random.nextInt(availableIndices.size()));
        usedQuizzes.add(selectedIndex);

        System.out.println("Selected " + type + " quiz " + selectedIndex + ". Used: " + usedQuizzes.size() + "/" + quizList.size());

        return quizList.get(selectedIndex);
    }

    private static CommonQuiz getQuizByDifficultyFromList(List<CommonQuiz> quizList, Set<Integer> usedQuizzes, int difficulty, String type) {
        List<CommonQuiz> filteredQuizzes = new ArrayList<>();
        for (int i = 0; i < quizList.size(); i++) {
            if (!usedQuizzes.contains(i) && quizList.get(i).getDifficulty() == difficulty) {
                filteredQuizzes.add(quizList.get(i));
            }
        }

        if (filteredQuizzes.isEmpty()) {
            return getRandomQuizFromList(quizList, usedQuizzes, type);
        }

        return filteredQuizzes.get(random.nextInt(filteredQuizzes.size()));
    }

    private static CommonQuiz getDefaultContextualQuiz() {
        return new CommonQuiz(
                "Fill in the blank: ____ is a common greeting.",
                "HELLO",
                null,
                1
        );
    }

    // Reset methods
    public static void resetUsedMultipleChoiceQuizzes() {
        usedMultipleChoiceQuizzes.clear();
        System.out.println("Reset all used multiple choice quizzes");
    }

    public static void resetUsedContextualQuizzes() {
        usedContextualQuizzes.clear();
        System.out.println("Reset all used contextual quizzes");
    }

    public static void resetAllUsedQuizzes() {
        usedMultipleChoiceQuizzes.clear();
        usedContextualQuizzes.clear();
        System.out.println("Reset all used quizzes (both types)");
    }

    // Statistics methods
    public static int getTotalMultipleChoiceQuizzes() {
        return COMMON_MULTIPLE_CHOICE_QUIZZES.size();
    }

    public static int getTotalContextualQuizzes() {
        return COMMON_CONTEXTUAL_QUIZZES.size();
    }

    public static int getUsedMultipleChoiceQuizzesCount() {
        return usedMultipleChoiceQuizzes.size();
    }

    public static int getUsedContextualQuizzesCount() {
        return usedContextualQuizzes.size();
    }

    public static int getAvailableMultipleChoiceQuizzesCount() {
        return COMMON_MULTIPLE_CHOICE_QUIZZES.size() - usedMultipleChoiceQuizzes.size();
    }

    public static int getAvailableContextualQuizzesCount() {
        return COMMON_CONTEXTUAL_QUIZZES.size() - usedContextualQuizzes.size();
    }

    // Backward compatibility methods
    public static CommonQuiz getRandomCommonQuiz() {
        return getRandomMultipleChoiceQuiz();
    }

    public static CommonQuiz getCommonQuizByDifficulty(int difficulty) {
        return getMultipleChoiceQuizByDifficulty(difficulty);
    }

    public static void resetUsedQuizzes() {
        resetUsedMultipleChoiceQuizzes();
    }

    public static int getTotalQuizzes() {
        return getTotalMultipleChoiceQuizzes();
    }

    public static int getUsedQuizzesCount() {
        return getUsedMultipleChoiceQuizzesCount();
    }

    public static int getAvailableQuizzesCount() {
        return getAvailableMultipleChoiceQuizzesCount();
    }

    // Inner class cho common quiz
    public static class CommonQuiz {
        private final String question;
        private final String answer;
        private final List<String> options; // null for contextual quizzes
        private final int difficulty;

        public CommonQuiz(String question, String answer, List<String> options, int difficulty) {
            this.question = question;
            this.answer = answer;
            this.options = options != null ? new ArrayList<>(options) : null;
            this.difficulty = difficulty;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public List<String> getOptions() { return options != null ? new ArrayList<>(options) : null; }
        public int getDifficulty() { return difficulty; }
        public boolean isMultipleChoice() { return options != null; }
        public boolean isContextual() { return options == null; }

        public Map<String, Object> toQuizMap() {
            Map<String, Object> quizData = new HashMap<>();
            quizData.put("type", isMultipleChoice() ? "multiple_choice" : "contextual_sentence");
            quizData.put("question", question);
            quizData.put("answer", answer);

            if (options != null) {
                quizData.put("options", new ArrayList<>(options));
            }

            quizData.put("difficulty", difficulty);
            quizData.put("points", difficulty * 5);
            quizData.put("timeLimit", 30f + ((difficulty - 1) * 5));
            quizData.put("difficultyLevel", getDifficultyLabel(difficulty));
            quizData.put("isCommonQuiz", true);
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
    }
}