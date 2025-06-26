package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import ctu.game.isometric.util.WordNetValidator;
import java.util.*;
import java.util.regex.Pattern;

public class SymbolicQuizSystem {
    private static final int MAX_OPTIONS = 4;
    private static final int MIN_WORD_LENGTH_FOR_DIFFICULTY = 5;
    private static final int MAX_DIFFICULTY = 5;

    private final Set<String> learnedWords;
    private final Random random;
    private final WordNetValidator wordNetValidator;
    private final List<String> learnedWordsList;

    public SymbolicQuizSystem(Set<String> learnedWords, WordNetValidator wordNetValidator) {
        this.learnedWords = learnedWords;
        this.random = new Random();
        this.wordNetValidator = wordNetValidator;
        this.learnedWordsList = new ArrayList<>(learnedWords);
    }

    public Map<String, Object> generateMultipleChoiceQuiz() {
        if (learnedWords.isEmpty()) {
            return createErrorResponse("No words available");
        }

        String wordUpperCase = getRandomWord();
        String word = wordUpperCase.toLowerCase();
        Word details = wordNetValidator.getWordDetails(word);

        if (!isValidWordDetails(details)) {
            return createErrorResponse("No details available for word: " + word);
        }

        WordDefinition selectedDef = selectRandomDefinition(details);
        if (!isValidDefinition(selectedDef)) {
            return createErrorResponse("Definition is missing for word: " + word);
        }

        String definition = selectedDef.getDefinition().split(";")[0];
        String questionText = generateQuestionText(definition);
        List<String> options = generateOptions(wordUpperCase, selectedDef);

        if (options.size() < MAX_OPTIONS) {
            System.out.println("Warning: Could only generate " + options.size() + " options for word: " + word);
            // Don't return error, just use what we have
        }

        Collections.shuffle(options);

        int difficulty = calculateDifficulty(word,
                selectedDef.getSynonyms() != null ? selectedDef.getSynonyms().size() : 0,
                details.getDefinitions().size(),
                selectedDef.getExamples() != null ? selectedDef.getExamples().size() : 0);

        return createQuizResponse("multiple_choice", questionText, wordUpperCase, options, difficulty);
    }

    public Map<String, Object> generateContextualSentenceQuiz() {
        if (learnedWords.isEmpty()) {
            return createErrorResponse("No words available");
        }

        // Bước 1: Chọn từ ngẫu nhiên và lấy thông tin từ WordNet
        String wordUpperCase = getRandomWord();
        String word = wordUpperCase.toLowerCase();
        Word details = wordNetValidator.getWordDetails(word);

        if (!isValidWordDetails(details)) {
            return createErrorResponse("No details available for word: " + word);
        }

        // Bước 2: Lấy định nghĩa và từ đồng nghĩa
        WordDefinition selectedDef = selectRandomDefinition(details);
        if (!isValidDefinition(selectedDef)) {
            return createErrorResponse("Definition is missing for word: " + word);
        }

        String definition = selectedDef.getDefinition().split(";")[0];
        List<String> synonyms = selectedDef.getSynonyms() != null ? selectedDef.getSynonyms() : new ArrayList<>();
        List<String> examples = new ArrayList<>();

        // Bước 3: Thu thập tất cả ví dụ từ các định nghĩa
        for (WordDefinition def : details.getDefinitions()) {
            if (def.getExamples() != null) {
                examples.addAll(def.getExamples());
            }
        }

        // Bước 4: Chọn đáp án đúng (từ gốc hoặc từ đồng nghĩa)
        String correctAnswer = wordUpperCase;
        if (!synonyms.isEmpty() && random.nextBoolean()) {
            correctAnswer = synonyms.get(random.nextInt(synonyms.size())).toUpperCase();
        }

        // Bước 5: Sinh câu hỏi
        String sentence = generateContextualSentence(word, synonyms, examples, definition);

        if (sentence == null || sentence.isEmpty()) {
            return createErrorResponse("Could not generate contextual sentence for word: " + word);
        }

        // Bước 6: Tính độ khó
        int difficulty = calculateDifficulty(word, synonyms.size(), details.getDefinitions().size(), examples.size());

        return createQuizResponse("contextual_sentence", sentence, correctAnswer, null, difficulty);
    }

    private String generateContextualSentence(String word, List<String> synonyms, List<String> examples, String definition) {
        // Try to use examples first (50% chance if examples exist)
        if (!examples.isEmpty() && random.nextBoolean()) {
            String sentence = tryCreateSentenceFromExamples(word, synonyms, examples);
            if (sentence != null) {
                return sentence;
            }
        }

        // Fallback to definition-based sentences
        return createDefinitionBasedSentence(definition);
    }

    private String tryCreateSentenceFromExamples(String word, List<String> synonyms, List<String> examples) {
        // Try up to 3 examples
        int maxAttempts = Math.min(3, examples.size());

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String example = examples.get(random.nextInt(examples.size()));
            String originalSentence = example;

            // Try to replace the word or its synonyms with blank
            String modifiedSentence = replaceWordWithBlank(example, word, synonyms);

            if (!modifiedSentence.equals(originalSentence) && modifiedSentence.contains("____")) {
                return modifiedSentence;
            }
        }

        return null; // No suitable example found
    }

    private String replaceWordWithBlank(String sentence, String word, List<String> synonyms) {
        String result = sentence;

        // Try to replace synonyms first (they might be more natural in examples)
        for (String syn : synonyms) {
            String pattern = "(?i)\\b" + Pattern.quote(syn) + "\\b";
            if (result.matches(".*" + pattern + ".*")) {
                result = result.replaceAll(pattern, "____");
                return result;
            }
        }

        // Then try the original word
        String pattern = "(?i)\\b" + Pattern.quote(word) + "\\b";
        if (result.matches(".*" + pattern + ".*")) {
            result = result.replaceAll(pattern, "____");
            return result;
        }

        return sentence; // No replacement made
    }

    private String createDefinitionBasedSentence(String definition) {
        List<String> templates = Arrays.asList(
                "A word meaning \"" + definition + "\" is: ____.",
                "The term for \"" + definition + "\" is: ____.",
                "Fill in the blank: \"" + definition + "\" describes something that is ____.",
                "What word means \"" + definition + "\"? Answer: ____.",
                "Complete: Something described as \"" + definition + "\" is ____."
        );
        return templates.get(random.nextInt(templates.size()));
    }

    private List<String> generateOptions(String correctAnswer, WordDefinition selectedDef) {
        List<String> options = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        options.add(correctAnswer);
        usedWords.add(correctAnswer);

        // Try to add antonyms as distractors
        int addedAntonyms = addAntonymsAsDistractors(options, usedWords, selectedDef);
        System.out.println("Added " + addedAntonyms + " antonyms as distractors");

        // Try to add random learned words as distractors
        int addedRandom = addRandomDistractors(options, usedWords, correctAnswer, selectedDef);
        System.out.println("Added " + addedRandom + " random words as distractors");

        // Try to add synonyms from other words as distractors
        int addedSynonyms = addSynonymDistractors(options, usedWords, correctAnswer, selectedDef);
        System.out.println("Added " + addedSynonyms + " synonym distractors");

        // Only fill with placeholders if we really can't find real words
        if (options.size() < MAX_OPTIONS) {
            fillRemainingOptions(options, usedWords);
        }

        return options;
    }

    private int addAntonymsAsDistractors(List<String> options, Set<String> usedWords, WordDefinition selectedDef) {
        Set<String> antonyms = selectedDef.getAntonyms();
        if (antonyms == null || antonyms.isEmpty()) return 0;

        int added = 0;
        for (String antonym : antonyms) {
            if (options.size() >= MAX_OPTIONS) break;

            String antonymUpperCase = antonym.toUpperCase();
            // CHỈ thêm nếu antonym CÓ TRONG learned words
            if (!usedWords.contains(antonymUpperCase) && learnedWords.contains(antonymUpperCase)) {
                options.add(antonymUpperCase);
                usedWords.add(antonymUpperCase);
                added++;
            }
        }
        return added;
    }

    private int addRandomDistractors(List<String> options, Set<String> usedWords,
                                     String correctAnswer, WordDefinition selectedDef) {
        List<String> synonyms = selectedDef.getSynonyms() != null ? selectedDef.getSynonyms() : new ArrayList<>();

        int maxAttempts = Math.min(50, learnedWords.size()); // Increased attempts
        int attempts = 0;
        int added = 0;

        while (options.size() < MAX_OPTIONS && attempts < maxAttempts) {
            String distractor = getRandomWord();
            attempts++;

            if (isValidDistractor(distractor, correctAnswer, synonyms, usedWords)) {
                options.add(distractor);
                usedWords.add(distractor);
                added++;
            }
        }
        return added;
    }

    private int addSynonymDistractors(List<String> options, Set<String> usedWords,
                                      String correctAnswer, WordDefinition selectedDef) {
        int added = 0;
        int attempts = 0;
        int maxAttempts = Math.min(20, learnedWords.size());

        while (options.size() < MAX_OPTIONS && attempts < maxAttempts) {
            String randomWord = getRandomWord();
            attempts++;

            if (usedWords.contains(randomWord) || randomWord.equals(correctAnswer)) {
                continue;
            }

            // Get details of this random word and try to use its synonyms
            Word randomWordDetails = wordNetValidator.getWordDetails(randomWord.toLowerCase());
            if (randomWordDetails != null && randomWordDetails.getDefinitions() != null) {
                for (WordDefinition def : randomWordDetails.getDefinitions()) {
                    if (options.size() >= MAX_OPTIONS) break;

                    List<String> synonymsOfRandom = def.getSynonyms();
                    if (synonymsOfRandom != null) {
                        for (String syn : synonymsOfRandom) {
                            if (options.size() >= MAX_OPTIONS) break;

                            String synUpper = syn.toUpperCase();
                            if (!usedWords.contains(synUpper) &&
                                    learnedWords.contains(synUpper) &&
                                    !synUpper.equals(correctAnswer)) {
                                options.add(synUpper);
                                usedWords.add(synUpper);
                                added++;
                                break; // Only add one synonym per word
                            }
                        }
                    }
                }
            }
        }
        return added;
    }

    private boolean isValidDistractor(String distractor, String correctAnswer,
                                      List<String> synonyms, Set<String> usedWords) {
        return !usedWords.contains(distractor) &&
                !distractor.equalsIgnoreCase(correctAnswer) &&
                !synonyms.contains(distractor.toLowerCase());
    }

    private void fillRemainingOptions(List<String> options, Set<String> usedWords) {
        // Try to use some common English words as fallback before using placeholders
        List<String> fallbackWords = Arrays.asList(
                "HOUSE", "WATER", "LIGHT", "RIGHT", "SMALL", "LARGE", "WORLD", "PLACE",
                "THING", "WOMAN", "CHILD", "SCHOOL", "STATE", "FAMILY", "NEVER",
                "SYSTEM", "PROGRAM", "QUESTION", "WORK", "GOVERNMENT", "COMPANY"
        );

        for (String fallback : fallbackWords) {
            if (options.size() >= MAX_OPTIONS) break;
            if (!usedWords.contains(fallback)) {
                options.add(fallback);
                usedWords.add(fallback);
            }
        }

        // Only use numbered placeholders as last resort
        while (options.size() < MAX_OPTIONS) {
            String placeholder = "OPTION_" + (options.size());
            if (!usedWords.contains(placeholder)) {
                options.add(placeholder);
                usedWords.add(placeholder);
            }
        }
    }

    private String generateQuestionText(String definition) {
        List<String> templates = Arrays.asList(
                "Which word means \"" + definition + "\"?",
                "Select the word that is defined as \"" + definition + "\":",
                "Choose the correct word for this definition: \"" + definition + "\""
        );
        return templates.get(random.nextInt(templates.size()));
    }

    private boolean isValidWordDetails(Word details) {
        return details != null &&
                details.getDefinitions() != null &&
                !details.getDefinitions().isEmpty();
    }

    private boolean isValidDefinition(WordDefinition definition) {
        return definition != null &&
                definition.getDefinition() != null &&
                !definition.getDefinition().isEmpty();
    }

    private WordDefinition selectRandomDefinition(Word details) {
        int defIndex = random.nextInt(details.getDefinitions().size());
        return details.getDefinitions().get(defIndex);
    }

    private int calculateDifficulty(String word, int synonymCount, int definitionCount, int exampleCount) {
        int baseDifficulty = 1;
        if (synonymCount >= 3) baseDifficulty++;
        if (word.length() >= MIN_WORD_LENGTH_FOR_DIFFICULTY) baseDifficulty++;
        if (definitionCount >= 2) baseDifficulty++;
        if (exampleCount >= 2) baseDifficulty++;
        return Math.min(baseDifficulty, MAX_DIFFICULTY);
    }

    private Map<String, Object> createQuizResponse(String type, String question, String answer,
                                                   List<String> options, int difficulty) {
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", type);
        quizData.put("question", question);
        quizData.put("answer", answer);
        if (options != null) {
            quizData.put("options", options);
        }
        quizData.put("difficulty", difficulty);
        quizData.put("points", difficulty * 1.2);
        return quizData;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }

    private String getRandomWord() {
        return learnedWordsList.get(random.nextInt(learnedWordsList.size()));
    }
}