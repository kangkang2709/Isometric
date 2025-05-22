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
    private static IDictionary dictionary;
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
                String[] parts = gloss.split(";");
                if (parts.length > 1) {
                    List<String> examples = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String ex = parts[i].trim().replaceAll("^\"|\"$", ""); // Remove surrounding quotes
                        examples.add(ex);
                    }
                    definition.setExamples(examples);
                }

                // Get synonyms
                Set<String> synonyms = new LinkedHashSet<>();
                for (IWord synonym : synset.getWords()) {
                    if (!synonym.getLemma().equalsIgnoreCase(wordText)) {
                        synonyms.add(synonym.getLemma());
                    }
                }
                definition.setSynonyms(new ArrayList<>(synonyms));

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


    public enum PartOfSpeech {
        NOUN, VERB, ADJECTIVE, ADVERB, PRONOUN, PREPOSITION, CONJUNCTION, INTERJECTION, UNKNOWN
    }

    private static final Map<Character, Integer> LETTER_BONUS = new HashMap<>();
    private static final Map<PartOfSpeech, Integer> POS_BONUS = new HashMap<>();
    private static final String[] COMMON_WORDS = {"the", "be", "to", "of", "and", "a", "in", "that", "have", "it"};

    static {
        // Initialize letter bonuses
        LETTER_BONUS.put('Q', 3);
        LETTER_BONUS.put('Z', 3);
        LETTER_BONUS.put('X', 3);
        LETTER_BONUS.put('J', 2);
        LETTER_BONUS.put('K', 2);
        LETTER_BONUS.put('V', 1);
        LETTER_BONUS.put('W', 1);
        LETTER_BONUS.put('Y', 1);

        // Initialize part-of-speech bonuses
        POS_BONUS.put(PartOfSpeech.NOUN, 3);
        POS_BONUS.put(PartOfSpeech.VERB, 4);
        POS_BONUS.put(PartOfSpeech.ADJECTIVE, 2);
        POS_BONUS.put(PartOfSpeech.ADVERB, 3);
        POS_BONUS.put(PartOfSpeech.PRONOUN, 1);
        POS_BONUS.put(PartOfSpeech.PREPOSITION, 1);
        POS_BONUS.put(PartOfSpeech.CONJUNCTION, 1);
        POS_BONUS.put(PartOfSpeech.INTERJECTION, 2);
        POS_BONUS.put(PartOfSpeech.UNKNOWN, 0);

    }

    //tinh theo do dai tu
    public static int calculateScore(String word) {
        if (word == null || word.length() < 3) return 0;

        int length = word.length();
        if (length <= 8) {
            return switch (length) {
                case 3 -> 1;
                case 4 -> 2;
                case 5 -> 4;
                case 6 -> 6;
                case 7 -> 10;
                case 8 -> 15;
                default -> 0;
            };
        }
        return 20 + (length - 9) * 5;
    }
    //ki tu hiem
    public static int calculateBonusPoints(String word) {
        if (word == null) return 0;

        int bonus = 0;
        for (char c : word.toCharArray()) {
            bonus += LETTER_BONUS.getOrDefault(Character.toUpperCase(c), 0);
        }
        return bonus;
    }
    // loai tu
    public static int calculatePartOfSpeechBonus(PartOfSpeech pos) {
        return POS_BONUS.getOrDefault(pos, 0);
    }

    public static int getEnhancedScore(String word, PartOfSpeech pos,
                                       int synonymCount) {
        int baseScore = calculateScore(word);
        int letterBonus = calculateBonusPoints(word);
        int posBonus = calculatePartOfSpeechBonus(pos);
        int synonymBonus = getSynonymCountBonus(synonymCount);

        return baseScore + letterBonus + posBonus  + synonymBonus;
    }
    // tu hiem
    public static int getRarityBonus(double frequency) {
        if (frequency < 0.0001) return 10;
        if (frequency < 0.001) return 7;
        if (frequency < 0.01) return 5;
        if (frequency < 0.1) return 3;
        return 0;
    }
    // so tu dong nghia
    public static int getSynonymCountBonus(int synonymCount) {
        if (synonymCount == 0) return 5;
        if (synonymCount < 3) return 3;
        if (synonymCount < 7) return 1;
        return 0;
    }

    public static int getTotalScore(Word word) {
        if (word == null) return 0;

        String text = word.getTerm().trim();

        // Get part of speech from WordNet or word definitions
        PartOfSpeech pos = determinePartOfSpeech(text, word);
        // Estimate word frequency using WordNet
        // Count synonyms from WordNet
        int synonymCount = countSynonyms(text);

        // Calculate semantic distance using WordNet

        return getEnhancedScore(text, pos, synonymCount);
    }

    public static int getTotalScore(String word) {
        if (word == null) return 0;
        return calculateScore(word) + calculateBonusPoints(word);
    }


    /**
     * Calculates semantic uniqueness bonus based on distance from common words
     */
    public static int getSemanticUniquenessBonus(double distanceFromCommonWords) {
        if (distanceFromCommonWords > 0.8) return 8;
        if (distanceFromCommonWords > 0.6) return 5;
        if (distanceFromCommonWords > 0.4) return 3;
        return 0;
    }

    // loai tu POS
    private static PartOfSpeech determinePartOfSpeech(String text, Word word) {
        if (dictionary == null) {
            // Fallback if WordNet is not available
            if (!word.getDefinitions().isEmpty() && word.getDefinitions().get(0).getPartOfSpeech() != null) {
                try {
                    return PartOfSpeech.valueOf(word.getDefinitions().get(0).getPartOfSpeech().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore and use fallback
                }
            }
            return PartOfSpeech.UNKNOWN;
        }

        try {
            // Try to find the most common POS in WordNet
            Map<PartOfSpeech, Integer> posCount = new HashMap<>();
            for (POS pos : POS.values()) {
                IIndexWord idxWord = dictionary.getIndexWord(text, pos);
                if (idxWord != null) {
                    PartOfSpeech wordnetPos = mapWordNetPOS(pos);
                    posCount.put(wordnetPos, posCount.getOrDefault(wordnetPos, 0) + idxWord.getWordIDs().size());
                }
            }

            if (!posCount.isEmpty()) {
                return posCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get().getKey();
            }

            // Fallback to definitions
            if (!word.getDefinitions().isEmpty() && word.getDefinitions().get(0).getPartOfSpeech() != null) {
                try {
                    return PartOfSpeech.valueOf(word.getDefinitions().get(0).getPartOfSpeech().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignore and return UNKNOWN
                }
            }
        } catch (Exception e) {
            // Fallback on exceptions
        }

        return PartOfSpeech.UNKNOWN;
    }

    /**
     * Maps WordNet POS to our PartOfSpeech enum
     */
    private static PartOfSpeech mapWordNetPOS(POS pos) {
        if (pos == POS.NOUN) return PartOfSpeech.NOUN;
        if (pos == POS.VERB) return PartOfSpeech.VERB;
        if (pos == POS.ADJECTIVE) return PartOfSpeech.ADJECTIVE;
        if (pos == POS.ADVERB) return PartOfSpeech.ADVERB;
        return PartOfSpeech.UNKNOWN;
    }

    /**
     * Estimates word frequency based on WordNet data
     */
    //tan suat
//    private static double estimateWordFrequency(String word) {
//        if (dictionary == null) {
//            // Simple frequency estimation based on word length if WordNet is unavailable
//            // Longer words are generally less frequent
//            return Math.max(0.0001, 0.5 / Math.pow(word.length(), 1.5));
//        }
//
//        try {
//            double totalUsageCount = 0;
//            int senseCount = 0;
//
//            for (POS pos : POS.values()) {
//                IIndexWord idxWord = dictionary.getIndexWord(word, pos);
//                if (idxWord != null) {
//                    for (IWordID wordID : idxWord.getWordIDs()) {
//                        IWord iWord = dictionary.getWord(wordID);
//                        ISynset synset = iWord.getSynset();
//                        // Use tag count as a frequency indicator
//                        totalUsageCount++;
//                        senseCount++;
//                    }
//                }
//            }
//
//            if (senseCount > 0) {
//                // Normalize to a value between 0 and 1
//                // Words with more tag counts are more common
//                double normalizedFrequency = Math.min(1.0, totalUsageCount / 50000.0);
//                return Math.max(0.0001, normalizedFrequency);
//            }
//        } catch (Exception e) {
//            // Fallback on exception
//        }
//
//        // Fallback frequency estimation
//        return 0.01;
//    }

    /**
     * Counts synonyms for a word using WordNet
     */
    private static int countSynonyms(String word) {
        if (dictionary == null) {
            return 0;
        }

        try {
            Set<String> synonyms = new HashSet<>();

            for (POS pos : POS.values()) {
                IIndexWord idxWord = dictionary.getIndexWord(word, pos);
                if (idxWord != null) {
                    for (IWordID wordID : idxWord.getWordIDs()) {
                        IWord iWord = dictionary.getWord(wordID);
                        ISynset synset = iWord.getSynset();

                        // Add all words in the synset as synonyms
                        for (IWord syn : synset.getWords()) {
                            String lemma = syn.getLemma().replace('_', ' ');
                            if (!lemma.equalsIgnoreCase(word)) {
                                synonyms.add(lemma);
                            }
                        }
                    }
                }
            }

            return synonyms.size();
        } catch (Exception e) {
            // Fallback on exception
            return 0;
        }
    }



}