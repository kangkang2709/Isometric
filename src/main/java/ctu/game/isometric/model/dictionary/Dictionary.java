package ctu.game.isometric.model.dictionary;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Dictionary {
    private Set<Word> learnedWords;
    private Set<Word> newWords;

    public Dictionary() {
        this.learnedWords = new HashSet<>();
        this.newWords = new HashSet<>();
    }

    // Add a word to the dictionary as a new word
    public void addNewWord(Word word) {
        newWords.add(word);
    }



    // Mark a word as learned (move from new to learned)
    public boolean markWordAsLearned(String term) {
        Optional<Word> wordToMove = findWordInNewWords(term);
        if (wordToMove.isPresent()) {
            Word word = wordToMove.get();
            newWords.remove(word);
            learnedWords.add(word);
            return true;
        }
        return false;
    }

    // Find a word in either dictionary
    public Optional<Word> findWord(String term) {
        Optional<Word> inLearned = findWordInLearnedWords(term);
        return inLearned.isPresent() ? inLearned : findWordInNewWords(term);
    }

    // Find a word in learned words
    public Optional<Word> findWordInLearnedWords(String term) {
        return learnedWords.stream()
                .filter(word -> word.getTerm().equalsIgnoreCase(term))
                .findFirst();
    }

    // Find a word in new words
    public Optional<Word> findWordInNewWords(String term) {
        return newWords.stream()
                .filter(word -> word.getTerm().equalsIgnoreCase(term))
                .findFirst();
    }

    // Search for words containing a substring
    public Set<Word> searchWords(String query) {
        String lowercaseQuery = query.toLowerCase();
        return getAllWords().stream()
                .filter(word -> word.getTerm().toLowerCase().contains(lowercaseQuery))
                .collect(Collectors.toSet());
    }

    // Get all words (combined from both sets)
    public Set<Word> getAllWords() {
        Set<Word> allWords = new HashSet<>(learnedWords);
        allWords.addAll(newWords);
        return allWords;
    }

    // Getters and setters
    public Set<Word> getLearnedWords() {
        return learnedWords;
    }

    public void setLearnedWords(Set<Word> learnedWords) {
        this.learnedWords = learnedWords;
    }

    public Set<Word> getNewWords() {
        return newWords;
    }

    public void setNewWords(Set<Word> newWords) {
        this.newWords = newWords;
    }

    // Get statistics
    public int getTotalWordCount() {
        return learnedWords.size() + newWords.size();
    }

    public int getLearnedWordCount() {
        return learnedWords.size();
    }

    public int getNewWordCount() {
        return newWords.size();
    }
}