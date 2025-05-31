package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import ctu.game.isometric.util.WordNetValidator;
import org.lwjgl.system.CallbackI;

import java.util.*;
import java.util.regex.Pattern;

public class SymbolicQuizSystem {
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

        // Step 1: Select a random word and get its details
        String wordUpperCase = getRandomWord();
        String word = wordUpperCase.toLowerCase();
        Word details = wordNetValidator.getWordDetails(word);

        if (details == null || details.getDefinitions() == null || details.getDefinitions().isEmpty()) {
            return createErrorResponse("No details available for word: " + word);
        }

        // Step 2: Select a random definition
        Random rand = new Random();
        int defIndex = rand.nextInt(details.getDefinitions().size());
        WordDefinition selectedDef = details.getDefinitions().get(defIndex);

        if (selectedDef == null || selectedDef.getDefinition() == null || selectedDef.getDefinition().isEmpty()) {
            System.out.println("No definition found for word: " + word);
            return createErrorResponse("Definition is missing for word: " + word);
        }

        String definition = selectedDef.getDefinition().split(";")[0];
        List<String> synonyms = selectedDef.getSynonyms() != null ? selectedDef.getSynonyms() : new ArrayList<>();
        Set<String> antonyms = selectedDef.getAntonyms() != null ? new HashSet<>(selectedDef.getAntonyms()) : new HashSet<>();

        // Step 3: Create the question text
        List<String> templates = Arrays.asList(
                "Which word means \"" + definition + "\"?",
                "Select the word that is defined as \"" + definition + "\":",
                "Choose the correct word for this definition: \"" + definition + "\""
        );
        String questionText = templates.get(rand.nextInt(templates.size()));

        // Step 4: Create the options list
        List<String> options = new ArrayList<>();
        String correctAnswer = wordUpperCase;
        options.add(correctAnswer);

        Set<String> usedWords = new HashSet<>();
        usedWords.add(wordUpperCase);

        // Add antonyms as distractors
        if (antonyms != null && !antonyms.isEmpty()) {
            for (String antonym : antonyms) {
                if (options.size() >= 4) break;
                String antonymUpperCase = antonym.toUpperCase();
                if (!usedWords.contains(antonymUpperCase)) {
                    options.add(antonymUpperCase);
                    usedWords.add(antonymUpperCase);
                }

                Word antonymDetails = wordNetValidator.getWordDetails(antonym.toLowerCase());
                if (antonymDetails != null && antonymDetails.getDefinitions() != null && !antonymDetails.getDefinitions().isEmpty()) {
                    for (WordDefinition antonymDef : antonymDetails.getDefinitions()) {
                        if (antonymDef.getDefinition() == null || antonymDef.getDefinition().isEmpty()) continue;

                        if (antonymDef.getDefinition().equalsIgnoreCase(selectedDef.getDefinition())) {
                            continue;
                        }

                        List<String> antonymSynonyms = antonymDef.getSynonyms();
                        if (antonymSynonyms != null && !antonymSynonyms.isEmpty()) {
                            for (String syn : antonymSynonyms) {
                                if (options.size() >= 4) break;
                                String synUpperCase = syn.toUpperCase();
                                if (!usedWords.contains(synUpperCase)) {
                                    options.add(synUpperCase);
                                    usedWords.add(synUpperCase);
                                }
                            }
                        }
                    }
                }
            }
        }

        while (options.size() < 4) {
            if (usedWords.size() >= learnedWords.size()) break;

            String distractor = getRandomWord();
            if (usedWords.contains(distractor) || distractor.equalsIgnoreCase(word) || synonyms.contains(distractor.toLowerCase())) {
                continue;
            }

            Word distractorDetails = wordNetValidator.getWordDetails(distractor.toLowerCase());
            if (distractorDetails == null || distractorDetails.getDefinitions() == null || distractorDetails.getDefinitions().isEmpty()) {
                continue;
            }

            for (WordDefinition distractorDef : distractorDetails.getDefinitions()) {
                if (distractorDef.getDefinition() == null || distractorDef.getDefinition().isEmpty()) break;

                if (distractorDef.getDefinition().equalsIgnoreCase(selectedDef.getDefinition())) {
                    break;
                }

                List<String> distractorSynonyms = distractorDef.getSynonyms();
                if (distractorSynonyms != null && !distractorSynonyms.isEmpty()) {
                    for (String syn : distractorSynonyms) {
                        if (options.size() >= 4) break;
                        String synUpperCase = syn.toUpperCase();
                        if (!usedWords.contains(synUpperCase)) {
                            options.add(synUpperCase);
                            usedWords.add(synUpperCase);
                        }
                    }
                }

                Set<String> distractorAntonyms = distractorDef.getAntonyms();
                if (distractorAntonyms != null && !distractorAntonyms.isEmpty()) {
                    for (String ant : distractorAntonyms) {
                        if (options.size() >= 4) break;
                        String antUpperCase = ant.toUpperCase();
                        if (!usedWords.contains(antUpperCase)) {
                            options.add(antUpperCase);
                            usedWords.add(antUpperCase);
                        }
                    }
                }
            }
        }

        while (options.size() < 4) {
            String placeholder = "OPTION_" + options.size();
            if (!usedWords.contains(placeholder)) {
                options.add(placeholder);
                usedWords.add(placeholder);
            }
        }

        Collections.shuffle(options);

        // Step 5: Calculate difficulty
        int difficulty = calculateDifficulty(word, synonyms.size(), details.getDefinitions().size(),
                selectedDef.getExamples() != null ? selectedDef.getExamples().size() : 0);

        // Step 6: Return the quiz
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", "multiple_choice");
        quizData.put("question", questionText);
        quizData.put("answer", correctAnswer);
        quizData.put("options", options);
        quizData.put("difficulty", difficulty);
        quizData.put("points", difficulty * 1.2);

        return quizData;
    }

    private int calculateDifficulty2(String word, int synonymCount, int definitionCount, int exampleCount) {
        int lengthWeight = word.length() > 8 ? 2 : 1;
        return synonymCount + definitionCount + exampleCount + lengthWeight;
    }


    public Map<String, Object> generateContextualSentenceQuiz() {
        if (learnedWords.isEmpty()) {
            return createErrorResponse("No words available");
        }

        // Bước 1: Chọn từ ngẫu nhiên và lấy thông tin từ WordNet
        String wordUpperCase = getRandomWord(); // IS ALL UPPER CASE
        String word = wordUpperCase.toLowerCase(); // Normalize for dictionary lookup
        Word details = wordNetValidator.getWordDetails(word);

        if (details == null || details.getDefinitions() == null || details.getDefinitions().isEmpty()) {
            return createErrorResponse("No details available for word: " + word);
        }

        // Bước 2: Lấy danh sách từ đồng nghĩa (synonyms)
        int definitionCount = details.getDefinitions().size(); // Số lượng định nghĩa

        List<String> synonyms = details.getDefinitions().get(0).getSynonyms();// Giả sử WordNetValidator hỗ trợ lấy synonyms
        if (synonyms == null || synonyms.isEmpty()) {
            synonyms = new ArrayList<>();
            synonyms.add(word); // Nếu không có từ đồng nghĩa, chỉ dùng từ gốc
        }


        // Bước 3: Chọn đáp án đúng (từ gốc hoặc từ đồng nghĩa)
        String correctAnswer = random.nextBoolean() ? wordUpperCase : synonyms.get(random.nextInt(synonyms.size())).toUpperCase();

        // Bước 4: Lấy ví dụ và định nghĩa
        List<String> examples = new ArrayList<>();
        String definition = details.getDefinitions().get(0).getDefinition().split(";")[0]; // Lấy định nghĩa đầu tiên

        for (WordDefinition def : details.getDefinitions()) {
            if (def.getExamples() != null) {
                examples.addAll(def.getExamples());
            }
        }

        // Bước 5: Sinh câu hỏi
        String sentence;
        if (!examples.isEmpty() && random.nextBoolean()) { // 50% dùng ví dụ, 50% dùng định nghĩa
            // Dùng ví dụ: Thay thế từ gốc hoặc từ đồng nghĩa bằng "____"
            sentence = examples.get(random.nextInt(examples.size()));
            String originalSentence = sentence;

            // Try to replace synonyms first
            for (String syn : synonyms) {
                sentence = sentence.replaceAll("(?i)\\b" + Pattern.quote(syn) + "\\b", "____");
            }

            // Then try with the word itself
            sentence = sentence.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "____");

            // Check if any replacement happened, if not, use definition approach
            if (sentence.equals(originalSentence)) {
                // Fall back to definition approach
                List<String> templates = Arrays.asList(
                        "A word meaning - \"" + definition + "\" is: ____.",
                        "The term for - \"" + definition + "\" is: ____.",
                        "What is a word that means \"" + definition + "\"? Fill in: ____."
                );
                sentence = templates.get(random.nextInt(templates.size()));
            }
        } else {
            // Dùng định nghĩa: Tạo câu hỏi từ định nghĩa
            List<String> templates = Arrays.asList(
                    "A word meaning - \"" + definition + "\" is: ____.",
                    "The term for - \"" + definition + "\" is: ____.",
                    "What is a word that means \"" + definition + "\"? Fill in: ____."
            );
            sentence = templates.get(random.nextInt(templates.size()));
        }

        // Bước 6: Tính độ khó dựa trên từ và ngữ cảnh
        int difficulty = calculateDifficulty(word, synonyms.size(), definitionCount, examples.size()); // Ví dụ: từ càng hiếm, từ đồng nghĩa càng nhiều -> khó hơn

        // Bước 7: Tạo dữ liệu câu hỏi
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", "contextual_sentence");
        quizData.put("question", sentence);
        quizData.put("answer", correctAnswer); // Đáp án có thể là từ gốc hoặc từ đồng nghĩa
        quizData.put("difficulty", difficulty);
        quizData.put("points", difficulty * 1.2); // Điểm dựa trên độ khó

        return quizData;
    }


    private int calculateDifficulty(String word, int synonymCount,int definationCount, int exampleCount) {
        int baseDifficulty = 1; // Mặc định
        if (synonymCount >= 3) baseDifficulty++; // Nhiều từ đồng nghĩa -> khó hơn
        if (word.length() >= 5) baseDifficulty++;
        if (definationCount >=2 ) baseDifficulty++;// Từ dài -> khó hơn
        if (exampleCount >= 2) baseDifficulty++; // Nhiều ví dụ -> khó hơn
        return Math.min(baseDifficulty, 5); // Giới hạn độ khó tối đa là 5
    }

    private String getRandomWord() {
        return learnedWordsList.get(random.nextInt(learnedWordsList.size()));
    }


    private String getRandomWordFromDefination(String sentence) {
        // Extract words from the sentence, normalize to uppercase
        String[] words = sentence.split("\\W+");
        String word = words[random.nextInt(words.length)].toUpperCase();
        if (wordNetValidator.isValidWord(word)) {
            return word;
        }
        return null;
    }
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }
}