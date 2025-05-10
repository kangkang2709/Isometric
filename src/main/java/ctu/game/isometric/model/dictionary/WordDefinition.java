package ctu.game.isometric.model.dictionary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordDefinition {
    private String partOfSpeech;
    private String definition;
    private List<String> examples;
    private List<String> synonyms;
    private Set<String> antonyms;

    public WordDefinition() {
        this.examples = new ArrayList<>();
        this.synonyms = new ArrayList<>();
        this.antonyms = new HashSet<>();
    }

    // Getters and setters
    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }


}