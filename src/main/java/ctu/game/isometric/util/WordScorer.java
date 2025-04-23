package ctu.game.isometric.util;

public class WordScorer {
    public static int calculateScore(String word) {
        if (word == null) return 0;
        int length = word.length();

        // Minimum length is 3
        if (length < 3) return 0;

        // Basic scoring formula
        switch (length) {
            case 3: return 1;
            case 4: return 2;
            case 5: return 4;
            case 6: return 6;
            case 7: return 10;
            case 8: return 15;
            default: return 20 + (length - 9) * 5; // Words longer than 8 letters
        }
    }

    public static int calculateBonusPoints(String word) {
        int bonus = 0;
        for (char c : word.toCharArray()) {
            switch (c) {
                case 'Q': case 'Z': case 'X':
                    bonus += 3;
                    break;
                case 'J': case 'K':
                    bonus += 2;
                    break;
                case 'V': case 'W': case 'Y':
                    bonus += 1;
                    break;
            }
        }
        return bonus;
    }

    public static int getTotalScore(String word) {
        return calculateScore(word) + calculateBonusPoints(word);
    }
}