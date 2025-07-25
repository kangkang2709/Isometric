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
    private final Map<String, Integer> cache;
    private volatile boolean dictionaryLoaded = false;
    private static final int CACHE_SIZE = 100;

    // Filter parameters
    //change to 1
    private static final int MIN_WORD_LENGTH = 2;
    private static final int MAX_WORD_LENGTH = 11;

    public WordNetValidator() {
        // Thread-safe LRU cache
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, Integer>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
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


    public synchronized void loadDictionary() {
        if (dictionaryLoaded) return;

        try {
            File dictDir = new File("dict"); // dict nằm cùng thư mục với jar
            if (!dictDir.exists() || !dictDir.isDirectory()) {
                Gdx.app.error("WordNet", "Không tìm thấy thư mục dict bên cạnh jar");
                return;
            }

            URL url = dictDir.toURI().toURL();
            dictionary = new Dictionary(url);
            dictionary.open();
            dictionaryLoaded = true;
            Gdx.app.log("WordNetValidator", "WordNet dictionary loaded from external folder");

        } catch (IOException e) {
            Gdx.app.error("WordNetValidator", "Failed to load WordNet dictionary", e);
        }
    }

    //    for dev
//    public synchronized void loadDictionary() {
//        if (dictionaryLoaded) return;
//
//        try {
//            String wordNetPath = "src/main/resources/game/dict";
//            URL url = new File(wordNetPath).toURI().toURL();
//            dictionary = new Dictionary(url);
//            dictionary.open();
//            dictionaryLoaded = true;
//            Gdx.app.log("WordNetValidator", "WordNet dictionary loaded");
//        } catch (IOException e) {
//            Gdx.app.error("WordNetValidator", "Failed to load WordNet dictionary", e);
//        }
//    }


    public boolean isValidWord(String word) {
        if (word == null || word.length() < MIN_WORD_LENGTH || word.length() > MAX_WORD_LENGTH) {
            return false;
        }

        // Normalize word to uppercase once
        String upperWord = word.toUpperCase();

        // Check cache first
        Integer cachedResult = cache.get(upperWord);

        if (cachedResult != null) {
            return true;
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

        if (result)
            cache.put(upperWord, getTotalScore(word));

        return result;
    }

    private boolean searchWord(String word) {
        if (dictionary == null) {
            return false;
        }
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

        String normalizedWordText = wordText.toLowerCase();

        // Search in each part of speech
        for (POS pos : POS.values()) {
            IIndexWord indexWord = dictionary.getIndexWord(normalizedWordText, pos);
            if (indexWord == null) continue;

            List<IWordID> wordIDs = indexWord.getWordIDs();
            if (wordIDs == null || wordIDs.isEmpty()) continue;

            IWordID wordID = wordIDs.get(0);
            IWord iword = dictionary.getWord(wordID);
            if (iword == null) continue;

            // Get definition (before semicolon)
            String gloss = iword.getSynset().getGloss();
            String definition;
            if (gloss.contains(";")) {
                definition = gloss.substring(0, gloss.indexOf(";")).trim();
            } else {
                definition = gloss.trim();
            }

            // Return the first valid meaning we find
            return pos.toString() + ": " + definition;
        }

        // If no meaning found
        return null;
    }

    public Word getWordDetails(String wordText) {
        if (!dictionaryLoaded) {
            loadDictionary();
        }

        if (dictionary == null) {
            return null;
        }

        Word word = new Word(wordText);

        // Search in each part of speech
        for (POS pos : POS.values()) {
            IIndexWord indexWord = dictionary.getIndexWord(wordText.toLowerCase(), pos);
            if (indexWord == null || indexWord.getWordIDs() == null) continue;

            // Get all meanings for this part of speech
            for (IWordID wordID : indexWord.getWordIDs()) {
                IWord iword = dictionary.getWord(wordID);
                if (iword == null) continue;

                ISynset synset = iword.getSynset();
                if (synset == null) continue;

                WordDefinition definition = new WordDefinition();

                // Set part of speech
                definition.setPartOfSpeech(pos.toString());

                // Set definition
                definition.setDefinition(synset.getGloss());

                // Get examples if available (examples are in the gloss after ';')
                String gloss = synset.getGloss();
                String[] parts = gloss.split(";");
                if (parts.length > 0) {
                    definition.setDefinition(parts[0].trim());
                }
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

                // Get antonyms
                Set<String> antonyms = new LinkedHashSet<>();
                // Look for antonyms through word pointers
                for (IWordID antonymID : iword.getRelatedWords(Pointer.ANTONYM)) {
                    IWord antonym = dictionary.getWord(antonymID);
                    if (antonym != null) {
                        antonyms.add(antonym.getLemma());
                    }
                }
                definition.setAntonyms(antonyms);

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
        NOUN, VERB, ADJECTIVE, ADVERB, SENSE, UNKNOWN;
    }

    private static final Map<Character, Integer> LETTER_BONUS = new HashMap<>();
    private static final Map<PartOfSpeech, Integer> POS_BONUS = new HashMap<>();
//    private static final String[] COMMON_WORDS = {"the", "be", "to", "of", "and", "a", "in", "that", "have", "it"};

    static {
        // Initialize letter bonuses
        LETTER_BONUS.put('Q', 2);
        LETTER_BONUS.put('Z', 2);
        LETTER_BONUS.put('X', 2);
        LETTER_BONUS.put('J', 2);
        LETTER_BONUS.put('V', 2);
        LETTER_BONUS.put('W', 2);

        //vowels
        LETTER_BONUS.put('A', 2);
        LETTER_BONUS.put('I', 2);
        LETTER_BONUS.put('U', 2);
        LETTER_BONUS.put('E', 2);
        LETTER_BONUS.put('O', 2);

        // Initialize part-of-speech bonuses
        POS_BONUS.put(PartOfSpeech.NOUN, 1);
        POS_BONUS.put(PartOfSpeech.VERB, 2);
        POS_BONUS.put(PartOfSpeech.ADJECTIVE, 2);
        POS_BONUS.put(PartOfSpeech.ADVERB, 3);
        POS_BONUS.put(PartOfSpeech.UNKNOWN, 1);

    }

    //tinh theo do dai tu
    public static int calculateScore(String word) {
        if (word == null || word.length() < 3) return 0;


        System.out.println("Length of word: " + word.length());

        int length = word.length();

        if (length <= 5) {
            return switch (length) {
                case 3 -> 3;
                case 4 -> 4;
                case 5 -> 5;
                default -> 0;
            };
        }
        return length + 2;
    }

    //ki tu hiem
    public static int calculateBonusPoints(String word) {
        if (word == null) return 0;

        int bonus = 0;
        for (char c : word.toCharArray()) {
            bonus += LETTER_BONUS.getOrDefault(Character.toUpperCase(c), 0);
        }
        System.out.println("Bonus points for rare letters: " + bonus);
        return bonus;
    }

    // loai tu
    public static int calculatePartOfSpeechBonus(PartOfSpeech pos) {
        System.out.println(pos + " bonus: " + POS_BONUS.getOrDefault(pos, 0));
        return POS_BONUS.getOrDefault(pos, 0);
    }

    public static int getEnhancedScore(String word, PartOfSpeech pos) {
        int baseScore = calculateScore(word);
        int letterBonus = calculateBonusPoints(word);
        int posBonus = calculatePartOfSpeechBonus(pos);

//        return baseScore + letterBonus + posBonus;
        return baseScore + letterBonus + posBonus;
    }


    public int getTotalScore(Word word) {
        if (word == null) return 0;

        String text = word.getTerm().trim();

        if (cache.get(text.toUpperCase()) != null) {
            return cache.get(text.toUpperCase());
        }

        PartOfSpeech pos = determinePartOfSpeech(text);

        return getEnhancedScore(text, pos);
    }



    public int getTotalScore(String word) {
        if (cache.get(word.toUpperCase()) != null) {
            System.out.println("Cache hit for word: " + cache.get(word.toUpperCase()));
            return cache.get(word.toUpperCase());
        }

        PartOfSpeech pos = determinePartOfSpeech(word);

        return getEnhancedScore(word, pos);
    }

    // loai tu POS
    private static PartOfSpeech determinePartOfSpeech(String text) {
        if (dictionary == null) {
            // Fallback if WordNet is not available
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
        } catch (Exception e) {
            System.err.println("Error determining part of speech: " + e.getMessage());
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

    public void dispose() {
        if (dictionary != null && dictionary.isOpen()) {
            dictionary.close();
            dictionary = null;
            dictionaryLoaded = false;
        }
    }
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