package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.sun.tools.javac.Main;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.dictionary.WordDefinition;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class WordNetValidator {
    private IDictionary dictionary;
    private final Map<String, Boolean> cache;
    private volatile boolean dictionaryLoaded = false;
    private static final int CACHE_SIZE = 100;

    // Filter parameters
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_WORD_LENGTH = 11;

    public WordNetValidator() {
        // Thread-safe LRU cache
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > CACHE_SIZE;
            }
        });
    }
//for build jar
//    public synchronized void loadDictionary() {
//        if (dictionaryLoaded) return;
//
//        try {
//            // Get the dictionary directory from local files
//            FileHandle dictDir = Gdx.files.local("dict");
//            URL dictionaryURL;
//
//            if (dictDir.exists() && dictDir.isDirectory()) {
//                dictionaryURL = dictDir.file().toURI().toURL();
//            } else {
//                Gdx.app.error("WordNetValidator", "Dictionary folder not found at: " + dictDir.path());
//                return;
//            }
//
//            // Create and open the dictionary
//            dictionary = new Dictionary(dictionaryURL);
//            dictionary.open();
//            dictionaryLoaded = true;
//            Gdx.app.log("WordNetValidator", "WordNet dictionary loaded successfully");
//
//        } catch (IOException e) {
//            Gdx.app.error("WordNetValidator", "Failed to load WordNet dictionary", e);
//        }
//    }
    
//    for dev
public synchronized void loadDictionary() {
    if (dictionaryLoaded) return;

    try {
        String wordNetPath = "src/main/resources/game/dict";
        URL url = new File(wordNetPath).toURI().toURL();
        dictionary = new Dictionary(url);
        dictionary.open();
        dictionaryLoaded = true;
        Gdx.app.log("WordNetValidator", "WordNet dictionary loaded");
    } catch (IOException e) {
        Gdx.app.error("WordNetValidator", "Failed to load WordNet dictionary", e);
    }
}



    public boolean isValidWord(String word) {
        if (word == null || word.length() < MIN_WORD_LENGTH || word.length() > MAX_WORD_LENGTH) {
            return false;
        }

        // Normalize word to uppercase once
        String upperWord = word.toUpperCase();

        // Check cache first
        Boolean cachedResult = cache.get(upperWord);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Load dictionary if not already loaded
        if (!dictionaryLoaded) {
            loadDictionary();
            if (!dictionaryLoaded) {
                return false;
            }
        }

        // Perform the search
        boolean result = searchWord(upperWord);
        cache.put(upperWord, result);

        return result;
    }

    private boolean searchWord(String word) {
        // Check if the word exists in any part of speech
        for (POS pos : POS.values()) {
            IIndexWord indexWord = dictionary.getIndexWord(word.toLowerCase(), pos);
            if (indexWord != null) {
                return true;
            }
        }
        return false;
    }

    public String getWordMeaning(String wordText) {
        if (!dictionaryLoaded) {
            loadDictionary();
            if (!dictionaryLoaded) {
                return null;
            }
        }

        StringBuilder meaning = new StringBuilder();
//        POS là một enum : {NOUN, VERB, ADJECTIVE, ADVERB}
        // Search in each part of speech
        for (POS pos : POS.values()) {
            IIndexWord indexWord = dictionary.getIndexWord(wordText.toLowerCase(), pos);
            if (indexWord == null) continue;

            // Get first meaning for this part of speech
            IWordID wordID = indexWord.getWordIDs().get(0);
            IWord iword = dictionary.getWord(wordID);

            // Add part of speech and main definition (before semicolon)
            String gloss = iword.getSynset().getGloss();
            String definition = gloss.contains(";") ?
                    gloss.substring(0, gloss.indexOf(";")).trim() :
                    gloss.trim();


            meaning.append(pos.toString())
                    .append(": ")
                    .append(definition)
                    .append("\n");
        }

        return meaning.toString().trim();
    }

    public Word getWordDetails(String wordText) {
        if (!dictionaryLoaded) {
            loadDictionary();
            if (!dictionaryLoaded) {
                return null;
            }
        }

        Word word = new Word(wordText);

        // Search in each part of speech
        for (POS pos : POS.values()) {
            IIndexWord indexWord = dictionary.getIndexWord(wordText.toLowerCase(), pos);
            if (indexWord == null) continue;

            // Get all meanings for this part of speech
            for (IWordID wordID : indexWord.getWordIDs()) {
                IWord iword = dictionary.getWord(wordID);
                ISynset synset = iword.getSynset();

                WordDefinition definition = new WordDefinition();

                // Set part of speech
                definition.setPartOfSpeech(pos.toString());

                // Set definition
                definition.setDefinition(iword.getSynset().getGloss());

                // Get examples if available (examples are in the gloss after ';')
                String gloss = iword.getSynset().getGloss();
                if (gloss.contains(";")) {
                    String[] parts = gloss.split(";");
                    if (parts.length > 1) {
                        List<String> examples = new ArrayList<>();
                        for (int i = 1; i < parts.length; i++) {
                            String ex = parts[i].trim();
                            if (ex.startsWith("\"") && ex.endsWith("\"")) {
                                ex = ex.substring(1, ex.length() - 1);
                            }
                            examples.add(ex);
                        }
                        definition.setExamples(examples);
                    }
                }

                // Get synonyms
                List<String> synonyms = new ArrayList<>();
                for (IWord synonym : synset.getWords()) {
                    if (!synonym.getLemma().equalsIgnoreCase(wordText)) {
                        synonyms.add(synonym.getLemma());
                    }
                }
                definition.setSynonyms(synonyms);

                word.addDefinition(definition);
            }
        }
        return word.getDefinitions().isEmpty() ? null : word;
    }

    public void close() {
        if (dictionary != null && dictionary.isOpen()) {
            dictionary.close();
        }
    }
}