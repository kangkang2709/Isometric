package ctu.game.isometric.model.dictionary;

import java.util.ArrayList;
import java.util.List;

public class Word {
    private String term;
    private String pronunciation;
    private List<WordDefinition> definitions;

    public Word() {
        this.definitions = new ArrayList<>();
    }

    public Word(String term) {
        this.term = term;
        this.definitions = new ArrayList<>();
    }

    // Getters and setters
    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }

    public List<WordDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<WordDefinition> definitions) {
        this.definitions = definitions;
    }

    public void addDefinition(WordDefinition definition) {
        this.definitions.add(definition);
    }
}