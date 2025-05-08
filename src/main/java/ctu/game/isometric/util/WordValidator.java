package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class WordValidator {
    private ArrayTrieNode root;
    private Map<String, Boolean> cache;
    private boolean dictionaryLoaded = false;
    private static final int CACHE_SIZE = 100; // Reduced from 1000

    // Filter parameters
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_WORD_LENGTH = 11;

    public WordValidator() {
        root = new ArrayTrieNode();
        // LRU cache implementation
        cache = new LinkedHashMap<String, Boolean>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > CACHE_SIZE;
            }
        };
    }

    private void loadDictionary() {
        if (dictionaryLoaded) return;

        try {
            BufferedReader reader = new BufferedReader(Gdx.files.internal("data/dictionary_2.txt").reader());
            Gdx.app.log("WordValidator", "Loading dictionary");
            String line;
            int wordCount = 0;
            int skippedCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toUpperCase();
                // Only insert words of useful length for the game
                if (line.length() >= MIN_WORD_LENGTH && line.length() <= MAX_WORD_LENGTH) {
                    insertWord(line);
                    wordCount++;
                } else {
                    skippedCount++;
                }
            }
            reader.close();
            dictionaryLoaded = true;
            Gdx.app.log("WordValidator", "Dictionary loaded: " + wordCount +
                    " words, skipped " + skippedCount + " words");
        } catch (IOException e) {
            Gdx.app.error("WordValidator", "Failed to load dictionary", e);
            loadFallbackDictionary();
        }
    }

    private void loadFallbackDictionary() {
        String[] commonWords = {"THE", "AND", "THAT", "HAVE", "FOR", "NOT", "WITH", "YOU", "THIS", "BUT"};
        for (String word : commonWords) {
            insertWord(word);
        }
        dictionaryLoaded = true;
        Gdx.app.log("WordValidator", "Loaded fallback dictionary");
    }

    private void insertWord(String word) {
        ArrayTrieNode current = root;
        for (char c : word.toCharArray()) {
            int index = c - 'A';
            if (index < 0 || index >= 26) continue; // Skip non A-Z characters

            if (current.children[index] == null) {
                current.children[index] = new ArrayTrieNode();
            }
            current = current.children[index];
        }
        current.isEndOfWord = true;
    }

    public boolean isValidWord(String word) {
        if (word == null || word.length() < MIN_WORD_LENGTH) {
            return false;
        }

        String upperWord = word.toUpperCase();

        // Check cache first
        if (cache.containsKey(upperWord)) {
            return cache.get(upperWord);
        }

        // Load dictionary if not already loaded
        if (!dictionaryLoaded) {
            loadDictionary();
        }

        boolean result = searchWord(upperWord);
        cache.put(upperWord, result); // LRU cache will manage size

        return result;
    }

    private boolean searchWord(String word) {
        ArrayTrieNode current = root;
        for (char c : word.toCharArray()) {
            int index = c - 'A';
            if (index < 0 || index >= 26 || current.children[index] == null) {
                return false;
            }
            current = current.children[index];
        }
        return current.isEndOfWord;
    }

    // Memory-efficient Trie node using array instead of HashMap
    private static class ArrayTrieNode {
        ArrayTrieNode[] children;
        boolean isEndOfWord;

        public ArrayTrieNode() {
            // Fixed size array for A-Z (26 characters)
            children = new ArrayTrieNode[26];
            isEndOfWord = false;
        }
    }
}