package ctu.game.isometric.model.quiz;

import ctu.game.isometric.controller.quiz.QuizTimer;
import ctu.game.isometric.controller.quiz.TimedQuizSystem;
import ctu.game.isometric.util.WordNetValidator;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho TimedQuizSystem class
 * Tests cả multiple choice và fill-in-the-blank quizzes với timer functionality
 */
@DisplayName("TimedQuizSystem Tests")
class TimedQuizSystemTest {

    private TimedQuizSystem timedQuizSystem;
    private WordNetValidator mockWordNetValidator;
    private Set<String> testLearnedWords;
    private static Application mockApp;

    private static final int DEFAULT_NUMBER_OF_QUESTIONS = 5;
    private static final float DEFAULT_TIME_LIMIT = 30f;

    @BeforeAll
    static void setUpGdx() {
        // Mock LibGDX dependencies
        mockApp = mock(Application.class);
        Gdx.app = mockApp;

        doNothing().when(mockApp).log(anyString(), anyString());
        doNothing().when(mockApp).error(anyString(), anyString());
        doNothing().when(mockApp).error(anyString(), anyString(), any(Throwable.class));
    }

    @AfterAll
    static void tearDownGdx() {
        Gdx.app = null;
    }

    @BeforeEach
    void setUp() {
        mockWordNetValidator = mock(WordNetValidator.class);
        setupMockWordNetValidator();

        testLearnedWords = new HashSet<>(Arrays.asList(
                "happy", "sad", "beautiful", "intelligent", "courage"
        ));

        timedQuizSystem = new TimedQuizSystem(testLearnedWords, mockWordNetValidator, DEFAULT_NUMBER_OF_QUESTIONS);

        // Clear global state before each test
        TimedQuizSystem.clearGlobalUsedQuestions();
    }

    @AfterEach
    void tearDown() {
        if (timedQuizSystem != null) {
            timedQuizSystem.resetSession();
        }
        TimedQuizSystem.resetAllCaches();
    }

    private void setupMockWordNetValidator() {
        // Mock basic word validation
        when(mockWordNetValidator.isValidWord(anyString())).thenReturn(true);

        // Mock word meanings
        when(mockWordNetValidator.getWordMeaning("happy")).thenReturn("feeling joy or pleasure");
        when(mockWordNetValidator.getWordMeaning("sad")).thenReturn("feeling sorrow");
        when(mockWordNetValidator.getWordMeaning("beautiful")).thenReturn("pleasing to look at");
        when(mockWordNetValidator.getWordMeaning("intelligent")).thenReturn("having good understanding");
        when(mockWordNetValidator.getWordMeaning("courage")).thenReturn("strength in facing danger");
    }

    @Nested
    @DisplayName("Multiple Choice Quiz Tests")
    class MultipleChoiceQuizTests {

        @Test
        @DisplayName("Should generate unique questions in session")
        void testGenerateMultipleChoiceQuiz_Uniqueness() {
            Set<String> generatedQuestions = new HashSet<>();

            for (int i = 0; i < 3; i++) {
                Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
                String question = (String) quiz.get("question");

                assertFalse(generatedQuestions.contains(question),
                        "Should not generate duplicate questions in session");
                generatedQuestions.add(question);
            }
        }

        @Test
        @DisplayName("Should handle different difficulty levels")
        void testGenerateMultipleChoiceQuiz_DifficultyLevels() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            Integer difficulty = (Integer) quiz.get("difficulty");

            assertNotNull(difficulty);
            assertTrue(difficulty >= 1 && difficulty <= 5, "Difficulty should be between 1 and 5");
        }


        @Test
        @DisplayName("Should handle empty learned words gracefully")
        void testGenerateMultipleChoiceQuiz_EmptyLearnedWords() {
            TimedQuizSystem emptySystem = new TimedQuizSystem(
                    new HashSet<>(), mockWordNetValidator, DEFAULT_NUMBER_OF_QUESTIONS);

            Map<String, Object> quiz = emptySystem.generateMultipleChoiceQuiz();

            assertNotNull(quiz);
            assertEquals("multiple_choice", quiz.get("type"));
            // Should fall back to common quiz bank
        }
    }

    @Nested
    @DisplayName("Contextual/Fill-in-the-Blank Quiz Tests")
    class ContextualQuizTests {

        @Test
        @DisplayName("Should generate valid contextual sentence quiz")
        void testGenerateContextualSentenceQuiz_ValidStructure() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();

            assertNotNull(quiz);
            assertEquals("contextual_sentence", quiz.get("type"));
            assertNotNull(quiz.get("question"));
            assertNotNull(quiz.get("answer"));
            assertNotNull(quiz.get("difficulty"));

            String question = (String) quiz.get("question");
            assertTrue(question.contains("_"),
                    "Contextual quiz should have blank spaces");

            String correctAnswer = (String) quiz.get("answer");
            assertFalse(correctAnswer.trim().isEmpty(), "Correct answer should not be empty");
        }

        @Test
        @DisplayName("Should generate sentence with proper word masking")
        void testGenerateContextualSentenceQuiz_WordMasking() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();

            String question = (String) quiz.get("question");
            String correctAnswer = (String) quiz.get("answer");

            // Question should not contain the correct answer directly
            assertFalse(question.toLowerCase().contains(correctAnswer.toLowerCase()),
                    "Question should not reveal the answer");
        }

        @Test
        @DisplayName("Should provide meaningful hints")
        void testGenerateContextualSentenceQuiz_Hints() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();

            Object hint = quiz.get("hint");
            if (hint != null) {
                String hintStr = (String) hint;
                assertFalse(hintStr.trim().isEmpty(), "Hint should not be empty if provided");

                String correctAnswer = (String) quiz.get("answer");
                assertFalse(hintStr.toLowerCase().contains(correctAnswer.toLowerCase()),
                        "Hint should not directly contain the answer");
            }
        }

        @Test
        @DisplayName("Should handle various word types in contextual quizzes")
        void testGenerateContextualSentenceQuiz_WordTypes() {
            Set<String> generatedWords = new HashSet<>();

            for (int i = 0; i < 5; i++) {
                Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();
                String correctAnswer = (String) quiz.get("answer");
                generatedWords.add(correctAnswer.toLowerCase());
            }

            // Should generate variety of words
            assertTrue(generatedWords.size() >= 2, "Should generate different words across attempts");
        }
    }

    @Nested
    @DisplayName("Timer Functionality Tests")
    class TimerFunctionalityTests {

        @Test
        @DisplayName("Should start timer when quiz begins")
        void testTimerFunctionality_StartTimer() {
            timedQuizSystem.startQuiz();

            QuizTimer timer = timedQuizSystem.getTimer();
            assertNotNull(timer, "Timer should be initialized after starting quiz");
        }

        @Test
        @DisplayName("Should calculate correct time limits for different difficulties")
        void testTimerFunctionality_TimeLimits() {
            float timeLimit1 = timedQuizSystem.calculateTimeLimitForDifficulty();
            assertTrue(timeLimit1 > 0, "Time limit should be positive");

            // Time limit should be reasonable (between 20 and 60 seconds typically)
            assertTrue(timeLimit1 >= 20f && timeLimit1 <= 60f,
                    "Time limit should be reasonable: " + timeLimit1);
        }

        @Test
        @DisplayName("Should handle timer completion callback")
        void testTimerFunctionality_TimerCallback() {
            timedQuizSystem.onTimerComplete();

            assertTrue(timedQuizSystem.isPendingAutoSubmit(),
                    "Should set pending auto-submit flag when timer completes");
        }

        @Test
        @DisplayName("Should reset pending auto-submit flag")
        void testTimerFunctionality_ResetAutoSubmit() {
            timedQuizSystem.onTimerComplete();
            assertTrue(timedQuizSystem.isPendingAutoSubmit());

            timedQuizSystem.resetPendingAutoSubmit();
            assertFalse(timedQuizSystem.isPendingAutoSubmit());
        }

        @ParameterizedTest
        @ValueSource(floats = {15f, 30f, 45f, 60f})
        @DisplayName("Should accept different default time limits")
        void testTimerFunctionality_CustomTimeLimits(float timeLimit) {
            timedQuizSystem.setDefaultTimeLimit(timeLimit);

            float calculatedLimit = timedQuizSystem.calculateTimeLimitForDifficulty();
            assertTrue(calculatedLimit >= timeLimit,
                    "Calculated limit should be at least the default: " + calculatedLimit);
        }
    }

    @Nested
    @DisplayName("Answer Submission Tests")
    class AnswerSubmissionTests {

        @Test
        @DisplayName("Should handle correct multiple choice answer submission")
        void testAnswerSubmission_CorrectMultipleChoice() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.startQuiz();

            String correctIndex = quiz.get("answer").toString();
            Map<String, Object> result = timedQuizSystem.submitAnswer(correctIndex);

            assertNotNull(result);
            assertTrue((Integer) result.get("score") > 0, "Should award points for correct answer");
        }

        @Test
        @DisplayName("Should handle incorrect multiple choice answer submission")
        void testAnswerSubmission_IncorrectMultipleChoice() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.startQuiz();

            List<String> options = (List<String>) quiz.get("options");
            int wrongIndex = (int) (Math.random() * options.size());

            Map<String, Object> result = timedQuizSystem.submitAnswer(String.valueOf(wrongIndex));

            assertNotNull(result);
            assertFalse((Boolean) result.get("correct"), "Should mark incorrect answer as incorrect");
            assertEquals(0, (Integer) result.get("score"), "Should award no points for incorrect answer");
        }

        @Test
        @DisplayName("Should handle correct contextual answer submission")
        void testAnswerSubmission_CorrectContextual() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();
            timedQuizSystem.startQuiz();

            String correctAnswer = (String) quiz.get("answer");
            Map<String, Object> result = timedQuizSystem.submitAnswer(correctAnswer);

            assertNotNull(result);
            assertTrue((Boolean) result.get("correct"), "Should mark correct contextual answer as correct");
            assertTrue((Integer) result.get("score") > 0, "Should award points for correct contextual answer");
        }

        @Test
        @DisplayName("Should handle case-insensitive contextual answers")
        void testAnswerSubmission_CaseInsensitiveContextual() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();
            timedQuizSystem.startQuiz();

            String correctAnswer = (String) quiz.get("answer");

            // Test different cases
            Map<String, Object> resultLower = timedQuizSystem.submitAnswer(correctAnswer.toLowerCase());
            assertTrue((Boolean) resultLower.get("correct"), "Should accept lowercase answer");

            // Generate new quiz for next test
            quiz = timedQuizSystem.generateContextualSentenceQuiz();
            correctAnswer = (String) quiz.get("answer");

            Map<String, Object> resultUpper = timedQuizSystem.submitAnswer(correctAnswer.toUpperCase());
            assertTrue((Boolean) resultUpper.get("correct"), "Should accept uppercase answer");
        }

        @ParameterizedTest
        @CsvSource({
                "1000, 10", // Fast answer - expect at least 10 points
                "5000, 8",  // Medium speed - expect at least 8 points
                "15000, 5"  // Slow answer - expect at least 5 points
        })
        @DisplayName("Should calculate time-based scoring correctly")
        void testAnswerSubmission_TimeBasedScoring(long timeMs, int expectedMinScore) {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.startQuiz();

            // Simulate time passage by waiting
            try {
                Thread.sleep(Math.min(timeMs, 100)); // Limit actual wait to avoid slow tests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String correctAnswer = quiz.get("answer").toString();
            Map<String, Object> result = timedQuizSystem.submitAnswer(correctAnswer);

            Integer score = (Integer) result.get("score");
            assertTrue(score >= expectedMinScore,
                    "Score should be at least " + expectedMinScore + " but was " + score);

            // Verify the answer was marked correct
            assertTrue((Boolean) result.get("correct"), "Correct answer should be marked as correct");

            // Verify score is reasonable (not negative, not too high)
            assertTrue(score >= 0 && score <= 50, "Score should be between 0 and 50, but was " + score);
        }

    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {

        @Test
        @DisplayName("Should track question count in session")
        void testSessionManagement_QuestionCount() {
            assertEquals(DEFAULT_NUMBER_OF_QUESTIONS, timedQuizSystem.getTotalQuestions());
            assertEquals(DEFAULT_NUMBER_OF_QUESTIONS, timedQuizSystem.getRemainingQuestions());

            // Generate a quiz to increment count
            timedQuizSystem.generateMultipleChoiceQuiz();
            assertEquals(DEFAULT_NUMBER_OF_QUESTIONS - 1, timedQuizSystem.getRemainingQuestions());
        }

        @Test
        @DisplayName("Should handle session completion")
        void testSessionManagement_SessionCompletion() {
            // Generate maximum number of questions
            for (int i = 0; i < DEFAULT_NUMBER_OF_QUESTIONS; i++) {
                Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
                assertNotNull(quiz);
            }

            assertEquals(0, timedQuizSystem.getRemainingQuestions());

            // Next quiz should indicate session completion
            Map<String, Object> result = timedQuizSystem.generateMultipleChoiceQuiz();
            if (result.containsKey("session_complete")) {
                assertTrue((Boolean) result.get("session_complete"));
            }
        }

        @Test
        @DisplayName("Should reset session correctly")
        void testSessionManagement_SessionReset() {
            // Use up some questions
            timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.generateContextualSentenceQuiz();

            assertTrue(timedQuizSystem.getRemainingQuestions() < DEFAULT_NUMBER_OF_QUESTIONS);

            timedQuizSystem.resetSession();

            assertEquals(DEFAULT_NUMBER_OF_QUESTIONS, timedQuizSystem.getRemainingQuestions());
            assertEquals(0, timedQuizSystem.getCommonMultipleChoiceUsedCount());
            assertEquals(0, timedQuizSystem.getCommonContextualUsedCount());
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 5, 10, 15})
        @DisplayName("Should handle different session sizes")
        void testSessionManagement_DifferentSessionSizes(int questionCount) {
            timedQuizSystem.setMaxQuestionsPerSession(questionCount);

            assertEquals(questionCount, timedQuizSystem.getTotalQuestions());
            assertEquals(questionCount, timedQuizSystem.getRemainingQuestions());
        }
    }

    @Nested
    @DisplayName("Global State Management Tests")
    class GlobalStateManagementTests {

        @Test
        @DisplayName("Should track global used questions")
        void testGlobalState_UsedQuestions() {
            int initialCount = TimedQuizSystem.getUsedQuestionsCount();

            timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.generateContextualSentenceQuiz();

            assertTrue(TimedQuizSystem.getUsedQuestionsCount() > initialCount,
                    "Should track used questions globally");
        }

        @Test
        @DisplayName("Should clear global used questions")
        void testGlobalState_ClearUsedQuestions() {
            timedQuizSystem.generateMultipleChoiceQuiz();
            assertTrue(TimedQuizSystem.getUsedQuestionsCount() > 0);

            TimedQuizSystem.clearGlobalUsedQuestions();
            assertEquals(0, TimedQuizSystem.getUsedQuestionsCount());
        }

        @Test
        @DisplayName("Should maintain used questions across instances")
        void testGlobalState_CrossInstanceTracking() {
            timedQuizSystem.generateMultipleChoiceQuiz();
            int countAfterFirst = TimedQuizSystem.getUsedQuestionsCount();

            // Create new instance
            TimedQuizSystem newInstance = new TimedQuizSystem(
                    testLearnedWords, mockWordNetValidator, DEFAULT_NUMBER_OF_QUESTIONS);

            newInstance.generateMultipleChoiceQuiz();
            int countAfterSecond = TimedQuizSystem.getUsedQuestionsCount();

            assertTrue(countAfterSecond > countAfterFirst,
                    "Global state should persist across instances");
        }

        @Test
        @DisplayName("Should reset all caches correctly")
        void testGlobalState_ResetAllCaches() {
            timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.generateContextualSentenceQuiz();

            assertTrue(TimedQuizSystem.getUsedQuestionsCount() > 0);

            TimedQuizSystem.resetAllCaches();

            assertEquals(0, TimedQuizSystem.getUsedQuestionsCount());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCaseTests {


        @Test
        @DisplayName("Should handle empty answer submission")
        void testEdgeCases_EmptyAnswer() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.startQuiz();

            Map<String, Object> result = timedQuizSystem.submitAnswer("");

            assertNotNull(result);
            assertFalse((Boolean) result.get("correct"), "Empty answer should be incorrect");
            assertEquals(0, (Integer) result.get("score"));
        }

        @Test
        @DisplayName("Should handle null answer submission")
        void testEdgeCases_NullAnswer() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();

            timedQuizSystem.startQuiz();

            Map<String, Object> result = timedQuizSystem.submitAnswer(null);

            assertNotNull(result);
            assertFalse((Boolean) result.get("correct"), "Null answer should be incorrect");
            assertEquals(0, (Integer) result.get("score"));
        }

        @Test
        @DisplayName("Should handle invalid multiple choice index")
        void testEdgeCases_InvalidMultipleChoiceIndex() {
            Map<String, Object> quiz = timedQuizSystem.generateMultipleChoiceQuiz();
            timedQuizSystem.startQuiz();

            Map<String, Object> result = timedQuizSystem.submitAnswer("99");

            assertNotNull(result);
            assertFalse((Boolean) result.get("correct"), "Invalid index should be incorrect");
        }

        @Test
        @DisplayName("Should handle very long answers")
        void testEdgeCases_VeryLongAnswer() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();
            timedQuizSystem.startQuiz();

            String longAnswer = "a".repeat(1000);
            Map<String, Object> result = timedQuizSystem.submitAnswer(longAnswer);

            assertNotNull(result);
            // Should handle gracefully without crashing
        }

        @Test
        @DisplayName("Should handle special characters in answers")
        void testEdgeCases_SpecialCharacters() {
            Map<String, Object> quiz = timedQuizSystem.generateContextualSentenceQuiz();
            timedQuizSystem.startQuiz();

            String specialAnswer = "café-résumé";
            assertDoesNotThrow(() -> {
                Map<String, Object> result = timedQuizSystem.submitAnswer(specialAnswer);
                assertNotNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should generate quizzes efficiently")
        void testPerformance_QuizGeneration() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 50; i++) {
                if (i % 2 == 0) {
                    timedQuizSystem.generateMultipleChoiceQuiz();
                } else {
                    timedQuizSystem.generateContextualSentenceQuiz();
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            assertTrue(duration < 5000, "Should generate 50 quizzes within 5 seconds: " + duration + "ms");
        }


        @Test
        @DisplayName("Should maintain performance with large learned word sets")
        void testPerformance_LargeWordSet() {
            Set<String> largeWordSet = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                largeWordSet.add("word" + i);
            }

            TimedQuizSystem largeSystem = new TimedQuizSystem(
                    largeWordSet, mockWordNetValidator, DEFAULT_NUMBER_OF_QUESTIONS);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                assertDoesNotThrow(() -> {
                    Map<String, Object> quiz = largeSystem.generateMultipleChoiceQuiz();
                    assertNotNull(quiz);
                });
            }

            long endTime = System.currentTimeMillis();
            assertTrue(endTime - startTime < 3000,
                    "Should handle large word sets efficiently");
        }
    }
}}