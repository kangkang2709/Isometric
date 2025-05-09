package ctu.game.isometric.util;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class WordNetExample {

    private IDictionary dictionary;

    public WordNetExample(String wordNetDictPath) throws IOException {
        // Construct the URL to the WordNet dictionary directory
        URL url = new File(wordNetDictPath).toURI().toURL();

        // Initialize the Dictionary
        dictionary = new Dictionary(url);

        // Open the dictionary
        dictionary.open();
    }

    public void showWordInfo(String word) {
        // Get all parts of speech for this word
        for (POS pos : POS.values()) {
            // Get the stem form of the word for this part of speech
            WordnetStemmer stemmer = new WordnetStemmer(dictionary);
            List<String> stems = stemmer.findStems(word, pos);

            for (String stem : stems) {
                // Look up the IDs of the word
                IIndexWord idxWord = dictionary.getIndexWord(stem, pos);
                if (idxWord == null) continue;

                System.out.println("Word: " + word + " (" + pos + ")");

                // Get all the word meanings (synsets)
                List<IWordID> wordIDs = idxWord.getWordIDs();
                for (IWordID wordID : wordIDs) {
                    // Get the synset
                    IWord iWord = dictionary.getWord(wordID);
                    ISynset synset = iWord.getSynset();

                    // Print out the definition and examples
                    System.out.println("Definition: " + synset.getGloss());

                    // Print synonyms
                    System.out.print("Synonyms: ");
                    for (IWord w : synset.getWords()) {
                        System.out.print(w.getLemma() + ", ");
                    }
                    System.out.println();
                    System.out.println("--------------------");
                }
            }
        }
    }

    public void close() {
        if (dictionary != null && dictionary.isOpen()) {
            dictionary.close();
        }
    }

    // Example usage
    public static void main(String[] args) {
        try {
            // Path to WordNet dictionary - adjust to your WordNet installation
            String wordNetPath = "src/main/resources/game/dict";

            WordNetExample wordNet = new WordNetExample(wordNetPath);
            wordNet.showWordInfo("game");
            wordNet.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}