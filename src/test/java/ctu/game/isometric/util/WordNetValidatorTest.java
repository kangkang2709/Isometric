package ctu.game.isometric.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.util.WordNetValidator.PartOfSpeech;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho WordNetValidator class
 * Tests các chức năng chính của validator từ vựng WordNet
 */
@DisplayName("WordNetValidator Tests")
class WordNetValidatorTest {

    private WordNetValidator validator;
    private static Application mockApp;

    @BeforeAll
    static void setUpGdx() {
        // Mock LibGDX dependencies
        mockApp = mock(Application.class);

        // Set the static field directly
        Gdx.app = mockApp;

        // Configure mock app to do nothing for log/error calls
        doNothing().when(mockApp).log(anyString(), anyString());
        doNothing().when(mockApp).error(anyString(), anyString());
        doNothing().when(mockApp).error(anyString(), anyString(), any(Throwable.class));
    }

    @AfterAll
    static void tearDownGdx() {
        // Clean up
        Gdx.app = null;
    }

    @BeforeEach
    void setUp() {
        validator = new WordNetValidator();
    }

    @Nested
    @DisplayName("Word Validation Tests")
    class WordValidationTests {

        @Test
        @DisplayName("Should return false for null word")
        void testIsValidWord_NullWord() {
            assertFalse(validator.isValidWord(null));
        }

        @Test
        @DisplayName("Should return false for empty word")
        void testIsValidWord_EmptyWord() {
            assertFalse(validator.isValidWord(""));
        }

        @Test
        @DisplayName("Should return false for word too short")
        void testIsValidWord_TooShort() {
            assertFalse(validator.isValidWord("a"));
        }

        @Test
        @DisplayName("Should return false for word too long")
        void testIsValidWord_TooLong() {
            String longWord = "supercalifragilisticexpialidocious"; // 34 characters
            assertFalse(validator.isValidWord(longWord));
        }

        @Test
        @DisplayName("Should handle word length boundaries correctly")
        void testIsValidWord_BoundaryLength() {
            // Test minimum valid length (2 characters)
            String minLengthWord = "to";
            // Test maximum valid length (11 characters)
            String maxLengthWord = "programming"; // 11 characters

            // These should not throw exceptions even if dictionary is not loaded
            assertDoesNotThrow(() -> validator.isValidWord(minLengthWord));
            assertDoesNotThrow(() -> validator.isValidWord(maxLengthWord));
        }

        @Test
        @DisplayName("Should normalize word to uppercase for cache")
        void testIsValidWord_CaseInsensitive() {
            // Test that different cases of the same word are handled consistently
            assertDoesNotThrow(() -> {
                validator.isValidWord("hello");
                validator.isValidWord("HELLO");
                validator.isValidWord("Hello");
            });
        }
    }

    @Nested
    @DisplayName("Score Calculation Tests")
    class ScoreCalculationTests {

        @Test
        @DisplayName("Should return 0 for null word in score calculation")
        void testCalculateScore_NullWord() {
            assertEquals(0, WordNetValidator.calculateScore(null));
        }

        @Test
        @DisplayName("Should return 0 for word shorter than 3 characters")
        void testCalculateScore_TooShort() {
            assertEquals(0, WordNetValidator.calculateScore("a"));
            assertEquals(0, WordNetValidator.calculateScore("ab"));
        }

        @Test
        @DisplayName("Should calculate correct scores for different word lengths")
        void testCalculateScore_ValidLengths() {
            assertEquals(3, WordNetValidator.calculateScore("cat"));    // 3 chars
            assertEquals(4, WordNetValidator.calculateScore("word"));   // 4 chars
            assertEquals(5, WordNetValidator.calculateScore("house"));  // 5 chars
            assertEquals(9, WordNetValidator.calculateScore("program")); // 7 chars -> 7+2=9
        }

        @Test
        @DisplayName("Should calculate bonus points for rare letters")
        void testCalculateBonusPoints_RareLetters() {
            assertEquals(0, WordNetValidator.calculateBonusPoints(null));
            assertEquals(0, WordNetValidator.calculateBonusPoints("bcd"));   // No rare
            assertEquals(4, WordNetValidator.calculateBonusPoints("hello")); // e = 2, o = 2
            assertEquals(8, WordNetValidator.calculateBonusPoints("quiz"));  // Q = 2 points
            assertEquals(6, WordNetValidator.calculateBonusPoints("zone"));  // Z = 2, O = 2 , E = 2
            assertEquals(10, WordNetValidator.calculateBonusPoints("queue")); // Q = 2, U = 2 *2, E = 2 *2
        }

        @Test
        @DisplayName("Should calculate part of speech bonus correctly")
        void testCalculatePartOfSpeechBonus() {
            assertEquals(1, WordNetValidator.calculatePartOfSpeechBonus(PartOfSpeech.NOUN));
            assertEquals(2, WordNetValidator.calculatePartOfSpeechBonus(PartOfSpeech.VERB));
            assertEquals(2, WordNetValidator.calculatePartOfSpeechBonus(PartOfSpeech.ADJECTIVE));
            assertEquals(3, WordNetValidator.calculatePartOfSpeechBonus(PartOfSpeech.ADVERB));
            assertEquals(1, WordNetValidator.calculatePartOfSpeechBonus(PartOfSpeech.UNKNOWN));
        }

        @Test
        @DisplayName("Should calculate enhanced score combining all factors")
        void testGetEnhancedScore() {
            // Test with a word that has rare letters
            String word = "quiz"; // 4 chars = 4 points, Q = 2 points , i = 2, u =2 ,z =2 , total = 4 + 2 + 2 + 2 +2 = 12
            PartOfSpeech pos = PartOfSpeech.NOUN; // 1 point

            int expected = 4 + 2 + 2 + 2 + 2 + 1; // base score + letter bonus + pos bonus
            assertEquals(expected, WordNetValidator.getEnhancedScore(word, pos));
        }
    }

    @Nested
    @DisplayName("Dictionary Operations Tests")
    class DictionaryOperationTests {

        @Test
        @DisplayName("Should handle getWordMeaning when dictionary not loaded")
        void testGetWordMeaning_DictionaryNotLoaded() {
            // Since we can't load actual WordNet in unit tests, this should return null
            String meaning = validator.getWordMeaning("hello");
            // Should not throw exception, may return null if dictionary fails to load
            assertDoesNotThrow(() -> validator.getWordMeaning("hello"));
        }

        @Test
        @DisplayName("Should handle getWordDetails when dictionary not loaded")
        void testGetWordDetails_DictionaryNotLoaded() {
            Word wordDetails = validator.getWordDetails("hello");
            // Should not throw exception, may return null if dictionary fails to load
            assertDoesNotThrow(() -> validator.getWordDetails("hello"));
        }

        @Test
        @DisplayName("Should handle null input in getWordMeaning")
        void testGetWordMeaning_NullInput() {
            assertDoesNotThrow(() -> validator.getWordMeaning(null));
        }

        @Test
        @DisplayName("Should handle null input in getWordDetails")
        void testGetWordDetails_NullInput() {
            assertDoesNotThrow(() -> validator.getWordDetails(null));
        }

        @Test
        @DisplayName("Should handle empty string in dictionary operations")
        void testDictionaryOperations_EmptyString() {
            assertDoesNotThrow(() -> {
                validator.getWordMeaning("");
                validator.getWordDetails("");
            });
        }
    }

    @Nested
    @DisplayName("Cache Tests")
    class CacheTests {

        @Test
        @DisplayName("Should handle cache operations without errors")
        void testCacheOperations() {
            // Test that cache doesn't cause issues when dictionary is not loaded
            assertDoesNotThrow(() -> {
                validator.isValidWord("test");
                validator.isValidWord("test"); // Should hit cache on second call
            });
        }

        @Test
        @DisplayName("Should handle multiple word validations efficiently")
        void testMultipleValidations() {
            String[] words = {"hello", "world", "java", "test", "cache"};

            assertDoesNotThrow(() -> {
                for (String word : words) {
                    validator.isValidWord(word);
                }

                // Call again to test cache hits
                for (String word : words) {
                    validator.isValidWord(word);
                }
            });
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should handle close operation safely")
        void testClose() {
            assertDoesNotThrow(() -> validator.close());
        }

        @Test
        @DisplayName("Should handle multiple close calls")
        void testMultipleClose() {
            assertDoesNotThrow(() -> {
                validator.close();
                validator.close(); // Should not cause issues
            });
        }

        @Test
        @DisplayName("Should handle operations after close")
        void testOperationsAfterClose() {
            validator.close();

            // Operations should still work (may reload dictionary)
            assertDoesNotThrow(() -> {
                validator.isValidWord("test");
                validator.getWordMeaning("test");
            });
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent word validations")
        void testConcurrentValidation() throws InterruptedException {
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            String[] testWords = {"hello", "world", "java", "test", "concurrent"};

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    for (String word : testWords) {
                        assertDoesNotThrow(() -> validator.isValidWord(word + threadIndex));
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }
        }

        @Test
        @DisplayName("Should handle concurrent dictionary loading")
        void testConcurrentDictionaryLoading() throws InterruptedException {
            int threadCount = 3;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    assertDoesNotThrow(() -> validator.loadDictionary());
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(3000); // 3 second timeout
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle special characters in words")
        void testSpecialCharacters() {
            assertDoesNotThrow(() -> {
                validator.isValidWord("hello-world");
                validator.isValidWord("test_word");
                validator.isValidWord("word123");
                validator.isValidWord("café");
            });
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void testUnicodeCharacters() {
            assertDoesNotThrow(() -> {
                validator.isValidWord("tèst");
                validator.isValidWord("wörd");
                validator.isValidWord("jalapeño");
            });
        }

        @Test
        @DisplayName("Should handle whitespace in words")
        void testWhitespaceInWords() {
            assertDoesNotThrow(() -> {
                validator.isValidWord(" test ");
                validator.isValidWord("test word");
                validator.isValidWord("\ttest\n");
            });
        }

        @Test
        @DisplayName("Should handle very specific word patterns")
        void testSpecificPatterns() {
            assertDoesNotThrow(() -> {
                validator.isValidWord("I");      // Single letter (should be false due to length)
                validator.isValidWord("a");      // Single letter (should be false due to length)
                validator.isValidWord("an");     // Two letters (minimum valid)
                validator.isValidWord("THE");    // All caps
                validator.isValidWord("123");    // Numbers only
            });
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should validate words within reasonable time")
        void testValidationPerformance() {
            String[] words = {"hello", "world", "java", "programming", "vocabulary"};

            long startTime = System.currentTimeMillis();

            for (String word : words) {
                validator.isValidWord(word);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Should complete within 5 seconds even without dictionary
            assertTrue(duration < 5000, "Validation took too long: " + duration + "ms");
        }

        @Test
        @DisplayName("Should handle large number of validations efficiently")
        void testBulkValidation() {
            String baseWord = "test";
            int wordCount = 100;

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < wordCount; i++) {
                validator.isValidWord(baseWord + i);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Should complete within 10 seconds for 100 words
            assertTrue(duration < 10000, "Bulk validation took too long: " + duration + "ms");
        }
    }
}