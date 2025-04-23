package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WordValidator {
    private Set<String> dictionary;

    public WordValidator() {
        dictionary = new HashSet<>();
        loadDictionary();
    }

    private void loadDictionary() {
        try {
            BufferedReader reader = new BufferedReader(Gdx.files.internal("data/dictionary_2.txt").reader());
            System.out.println("Loading dictionary from data/dictionary.txt");
            String line;
            while ((line = reader.readLine()) != null) {
                dictionary.add(line.trim().toUpperCase());
            }
            reader.close();
        } catch (IOException e) {
            Gdx.app.error("WordValidator", "Failed to load dictionary", e);
        }
    }

    public boolean isValidWord(String word) {
        if (word == null || word.length() < 3) {
            return false; // Words must be at least 3 letters
        }
        return dictionary.contains(word.toUpperCase());
    }
}