package ctu.game.isometric.controller.quiz;

import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import ctu.game.isometric.util.WordNetValidator;

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





    public Map<String, Object> generateContextualSentenceQuiz() {
        if (learnedWords.isEmpty()) {
            return createErrorResponse("No words available");
        }

        // Bước 1: Chọn từ ngẫu nhiên và lấy thông tin từ WordNet
        String wordUpperCase = getRandomWord(); // IS ALL UPPER CASE
        String word = wordUpperCase.toLowerCase(); // Normalize for dictionary lookup
        Word details = wordNetValidator.getWordDetails(word);

        if (details == null || details.getDefinitions().isEmpty()) {
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
            for (String syn : synonyms) {
                sentence = sentence.replaceAll("(?i)\\b" + syn + "\\b", "____");
            }
            sentence = sentence.replaceAll("(?i)\\b" + word + "\\b", "____");
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
        int difficulty = calculateDifficulty(word, synonyms.size(),definitionCount,examples.size()); // Ví dụ: từ càng hiếm, từ đồng nghĩa càng nhiều -> khó hơn

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

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }
}