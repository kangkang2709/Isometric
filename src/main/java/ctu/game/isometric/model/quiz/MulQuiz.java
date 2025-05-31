package ctu.game.isometric.model.quiz;

import java.util.Map;

public class MulQuiz  extends Quiz{
    private String[] options;

    public MulQuiz() {
        super();
    }

    public MulQuiz(String type, String question, String answer, int difficulty, float points, String[] options) {
        super(type, question, answer, difficulty, points);
        this.options = options;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> quizData = super.toMap();
        quizData.put("options", options);
        return quizData;
    }
}
