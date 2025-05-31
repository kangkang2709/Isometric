package ctu.game.isometric.model.quiz;

import java.util.HashMap;
import java.util.Map;

public class Quiz {
    String type;
    String question;
    String answer;
    int difficulty;
    float points;

    public Quiz() {
    }

    public Quiz(String type, String question, String answer, int difficulty, float points) {
        this.type = type;
        this.question = question;
        this.answer = answer;
        this.difficulty = difficulty;
        this.points = points;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("type", type);
        quizData.put("question", question);
        quizData.put("answer", answer);
        quizData.put("difficulty", difficulty);
        quizData.put("points", points);
        return quizData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public float getPoints() {
        return points;
    }

    public void setPoints(float points) {
        this.points = points;
    }
}
