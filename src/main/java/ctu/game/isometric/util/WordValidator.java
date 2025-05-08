package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class WordValidator {
    private TrieNode root;
    private Map<String, Boolean> cache;
    private boolean dictionaryLoaded = false;
    private static final int CACHE_SIZE = 50;
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_WORD_LENGTH = 11;

    public WordValidator() {
        root = new TrieNode();
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
                // Only load useful words for the game
                if (line.length() >= MIN_WORD_LENGTH && line.length() <= MAX_WORD_LENGTH) {
                    insertWord(line);
                    wordCount++;
                } else {
                    skippedCount++;
                }

                // Process system events every 5000 words to keep game responsive
                if ((wordCount + skippedCount) % 5000 == 0) {
                    Gdx.app.debug("WordValidator", "Processed " + (wordCount + skippedCount) + " words");
                }
            }
            reader.close();
            dictionaryLoaded = true;
            Gdx.app.log("WordValidator", "Dictionary loaded: " + wordCount + " words, skipped " + skippedCount);

            // Force garbage collection after loading
            System.gc();
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
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            int index = c - 'A';
            if (index < 0 || index >= 26) continue;

            if (!current.hasChild(index)) {
                current.setChild(index);
            }
            current = current.getChild(index);
        }
        current.setEndOfWord();
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
        cache.put(upperWord, result);

        return result;
    }

    private boolean searchWord(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            int index = c - 'A';
            if (index < 0 || index >= 26 || !current.hasChild(index)) {
                return false;
            }
            current = current.getChild(index);
        }
        return current.isEndOfWord();
    }

    // Highly optimized memory-efficient Trie node using bit vectors
    private static class TrieNode {
        private int childrenBitmap; // Bit vector for child presence
        private int endOfWordFlag;  // Using bit 31 as the end-of-word flag
        private TrieNode[] children;

        public TrieNode() {
            childrenBitmap = 0;
            endOfWordFlag = 0;
            children = null; // Lazy allocation
        }

        public boolean hasChild(int index) {
            return (childrenBitmap & (1 << index)) != 0;
        }

        public void setChild(int index) {
            // Set bit in bitmap
            childrenBitmap |= (1 << index);

            // Create children array on demand
            if (children == null) {
                // Count bits to determine array size (population count)
                children = new TrieNode[1];
            } else if (Integer.bitCount(childrenBitmap) > children.length) {
                // Expand array only when needed
                TrieNode[] newChildren = new TrieNode[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                children = newChildren;
            }

            // Find position in sparse array
            int pos = Integer.bitCount(childrenBitmap & ((1 << index) - 1));

            // Add new node
            if (children[pos] == null) {
                children[pos] = new TrieNode();
            }
        }

        public TrieNode getChild(int index) {
            if (!hasChild(index)) return null;

            // Find position in sparse array
            int pos = Integer.bitCount(childrenBitmap & ((1 << index) - 1));
            return children[pos];
        }

        public void setEndOfWord() {
            endOfWordFlag = 1;
        }

        public boolean isEndOfWord() {
            return endOfWordFlag == 1;
        }
    }
}