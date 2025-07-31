package ctu.game.isometric.controller;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameSave;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GameSaveControllerTest {

    @Mock
    private FileHandle mockFileHandle;

    @Mock
    private FileHandle mockDirectory;

    @Mock
    private Application mockApplication;

    @TempDir
    Path tempDir;

    private GameSaveController gameSaveController;
    private Character testCharacter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock Gdx
        Gdx.app = mockApplication;

        // Create test character
        testCharacter = createTestCharacter();

        // Initialize controller
        gameSaveController = new GameSaveController();
    }

    private Character createTestCharacter() {
        Character character = new Character();
        character.setName("TestHero");
        character.setHealth(100);
        character.setMaxHealth(100);
        character.setMana(50);
        character.setMaxMana(50);
        character.setLevel(5);
        character.setExp(250);
        character.setScore(1000);
        character.setGridX(10);
        character.setGridY(15);
        character.setMapName("main");
        character.setWordFilePath("test_dictionary");

        // Add some test data
        List<String> flags = new ArrayList<>();
        flags.add("completed_tutorial");
        flags.add("has_sword");
        character.setFlags(flags);

        Set<String> learnedWords = new HashSet<>();
        learnedWords.add("apple");
        learnedWords.add("banana");
        character.setLearnedWords(learnedWords);

        Map<String, Integer> items = new HashMap<>();
        items.put("health_potion", 3);
        items.put("mana_potion", 1);
        character.setItems(items);

        return character;
    }

    @Test
    void testSaveGameSuccess() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            // Setup mocks
            FileHandle mockSaveDir = mock(FileHandle.class);
            FileHandle mockDictDir = mock(FileHandle.class);
            FileHandle mockSaveFile = mock(FileHandle.class);

            gdxMock.when(() -> Gdx.files.local("saves/dictionary/")).thenReturn(mockDictDir);
            gdxMock.when(() -> Gdx.files.local("saves/")).thenReturn(mockSaveDir);
            gdxMock.when(() -> Gdx.files.local(anyString())).thenReturn(mockSaveFile);

            when(mockDictDir.exists()).thenReturn(true);
            when(mockSaveDir.list()).thenReturn(new FileHandle[0]);
            doNothing().when(mockSaveFile).writeString(anyString(), eq(false));

            // Test save operation
            boolean result = gameSaveController.saveGame(testCharacter, "test_save", null);

            assertTrue(result, "Lưu game thành công");
            verify(mockSaveFile).writeString(anyString(), eq(false));
        }
    }

    @Test
    void testSaveGameWithEventManagers() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            // Setup mocks
            FileHandle mockSaveDir = mock(FileHandle.class);
            FileHandle mockDictDir = mock(FileHandle.class);
            FileHandle mockSaveFile = mock(FileHandle.class);

            gdxMock.when(() -> Gdx.files.local("saves/dictionary/")).thenReturn(mockDictDir);
            gdxMock.when(() -> Gdx.files.local("saves/")).thenReturn(mockSaveDir);
            gdxMock.when(() -> Gdx.files.local(anyString())).thenReturn(mockSaveFile);

            when(mockDictDir.exists()).thenReturn(true);
            when(mockSaveDir.list()).thenReturn(new FileHandle[0]);

            // Create mock event managers
            List<EventManager> eventManagers = new ArrayList<>();
            EventManager mockEventManager = mock(EventManager.class);
            when(mockEventManager.getMapName()).thenReturn("main");
            when(mockEventManager.getListIdCompletedEvents()).thenReturn(Arrays.asList("event1", "event2"));
            when(mockEventManager.getListIdDefeatedEnemies()).thenReturn(Arrays.asList(1, 2, 3));
            eventManagers.add(mockEventManager);

            boolean result = gameSaveController.saveGame(testCharacter, "test_save_with_events", eventManagers);

            assertTrue(result, "Lưu game với event managers thành công");
        }
    }

    @Test
    void testLoadGameSuccess() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            // Create test save data
            GameSave testSave = new GameSave();
            testSave.setCharacter(testCharacter);
            testSave.setSaveDate(new Date());
            testSave.setWordFilePath("saves/dictionary/test_dictionary.json");

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(testSave);

            FileHandle mockFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/test_save.json")).thenReturn(mockFile);
            when(mockFile.readString()).thenReturn(jsonData);

            GameSave loadedSave = gameSaveController.loadGame("test_save.json");

            assertNotNull(loadedSave, "Load game thành công");
            assertEquals("TestHero", loadedSave.getCharacter().getName());
            assertEquals(100, loadedSave.getCharacter().getHealth());
            assertEquals(5, loadedSave.getCharacter().getLevel());
        } catch (Exception e) {
            fail("Lỗi khi test load game: " + e.getMessage());
        }
    }

    @Test
    void testLoadGameWithEncryption() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            // Test với dữ liệu mã hóa giả định
            String encryptedData = "dGVzdEVuY3J5cHRlZERhdGE="; // Base64 encoded test data

            FileHandle mockFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/encrypted_save.json")).thenReturn(mockFile);
            when(mockFile.readString()).thenReturn(encryptedData);

            // Trong thực tế, việc này sẽ thất bại do decryption
            // Nhưng chúng ta test để đảm bảo hệ thống xử lý exception đúng cách
            GameSave result = gameSaveController.loadGame("encrypted_save.json");

            assertNull(result, "Load game với dữ liệu mã hóa không hợp lệ trả về null");
        }
    }

    @Test
    void testLoadGameInvalidFilename() {
        assertThrows(IllegalArgumentException.class, () -> {
            gameSaveController.loadGame(null);
        }, "Load game với filename null ném exception");

        assertThrows(IllegalArgumentException.class, () -> {
            gameSaveController.loadGame("");
        }, "Load game với filename rỗng ném exception");
    }

    @Test
    void testSaveLearnedWords() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/dictionary/test_dictionary.json")).thenReturn(mockFile);

            gameSaveController.saveLearnedWords(testCharacter);

            verify(mockFile).writeString(anyString(), eq(false));
        }
    }

    @Test
    void testSaveLearnedWordsNullPath() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            Character characterWithNullPath = new Character();
            characterWithNullPath.setWordFilePath(null);

            // Không nên ném exception, chỉ log error
            assertDoesNotThrow(() -> {
                gameSaveController.saveLearnedWords(characterWithNullPath);
            });
        }
    }

    @Test
    void testLoadLearnedWords() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            Set<String> expectedWords = Set.of("apple", "banana", "orange");
            ObjectMapper mapper = new ObjectMapper();
            String wordsJson = mapper.writeValueAsString(expectedWords);

            FileHandle mockFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/dictionary/test_dictionary.json")).thenReturn(mockFile);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.readString()).thenReturn(wordsJson);

            Set<String> loadedWords = gameSaveController.loadLearnedWords(testCharacter, "saves/dictionary/test_dictionary.json");

            assertEquals(expectedWords.size(), loadedWords.size());
            assertTrue(loadedWords.containsAll(expectedWords));
        } catch (Exception e) {
            fail("Lỗi khi test load learned words: " + e.getMessage());
        }
    }

    @Test
    void testLoadLearnedWordsFileNotExists() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/dictionary/nonexistent.json")).thenReturn(mockFile);
            when(mockFile.exists()).thenReturn(false);

            Set<String> result = gameSaveController.loadLearnedWords(testCharacter, "saves/dictionary/nonexistent.json");

            assertTrue(result.isEmpty(), "Load từ file không tồn tại trả về set rỗng");
        }
    }

    @Test
    void testGetSaveFiles() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockDir = mock(FileHandle.class);
            FileHandle[] mockFiles = {
                    createMockFileHandle("save1.json"),
                    createMockFileHandle("save2.json"),
                    createMockFileHandle("save3.json")
            };

            gdxMock.when(() -> Gdx.files.local("saves/")).thenReturn(mockDir);
            when(mockDir.list(".json")).thenReturn(mockFiles);

            String[] saveFiles = gameSaveController.getSaveFiles();

            assertEquals(3, saveFiles.length);
            assertEquals("save1.json", saveFiles[0]);
            assertEquals("save2.json", saveFiles[1]);
            assertEquals("save3.json", saveFiles[2]);
        }
    }

    @Test
    void testDeleteSave() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockSaveFile = mock(FileHandle.class);
            FileHandle mockDictFile = mock(FileHandle.class);

            gdxMock.when(() -> Gdx.files.local("saves/TestHero_save.json")).thenReturn(mockSaveFile);
            gdxMock.when(() -> Gdx.files.local("saves/dictionary/TestHero_dictionary.json")).thenReturn(mockDictFile);

            when(mockSaveFile.exists()).thenReturn(true);
            when(mockDictFile.exists()).thenReturn(true);

            boolean result = gameSaveController.deleteSave("TestHero_save.json");

            assertTrue(result, "Xóa save file thành công");
            verify(mockSaveFile).delete();
            verify(mockDictFile).delete();
        }
    }

    @Test
    void testDeleteSaveFileNotExists() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockSaveFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/nonexistent.json")).thenReturn(mockSaveFile);
            when(mockSaveFile.exists()).thenReturn(false);

            boolean result = gameSaveController.deleteSave("nonexistent.json");

            assertFalse(result, "Xóa file không tồn tại trả về false");
        }
    }

    @Test
    void testMaintainSaveLimit() {
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            // Tạo nhiều file save hơn giới hạn
            FileHandle[] mockFiles = new FileHandle[6]; // MAX_SAVE_FILES = 5
            for (int i = 0; i < 6; i++) {
                mockFiles[i] = createMockFileHandle("save" + i + ".json");
                when(mockFiles[i].lastModified()).thenReturn((long) i * 1000); // Thời gian khác nhau
            }

            FileHandle mockDir = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/")).thenReturn(mockDir);
            when(mockDir.list(".json")).thenReturn(mockFiles);

            // Test save game sẽ trigger maintain limit
            FileHandle mockDictDir = mock(FileHandle.class);
            FileHandle mockSaveFile = mock(FileHandle.class);
            gdxMock.when(() -> Gdx.files.local("saves/dictionary/")).thenReturn(mockDictDir);
            gdxMock.when(() -> Gdx.files.local(anyString())).thenReturn(mockSaveFile);
            when(mockDictDir.exists()).thenReturn(true);

            gameSaveController.saveGame(testCharacter, "new_save", null);

            // Verify oldest files were deleted
            verify(mockFiles[0]).delete(); // Oldest file
            verify(mockFiles[1]).delete(); // Second oldest file
        }
    }

    private FileHandle createMockFileHandle(String name) {
        FileHandle mockFile = mock(FileHandle.class);
        when(mockFile.name()).thenReturn(name);
        return mockFile;
    }

    @Test
    void testChecksumGeneration() {
        String testData = "test data for checksum";

        // Gọi method private thông qua reflection (nếu cần thiết)
        // Hoặc test thông qua public methods sử dụng checksum

        // Test save với checksum
        try (MockedStatic<Gdx> gdxMock = mockStatic(Gdx.class)) {
            FileHandle mockSaveDir = mock(FileHandle.class);
            FileHandle mockDictDir = mock(FileHandle.class);
            FileHandle mockSaveFile = mock(FileHandle.class);

            gdxMock.when(() -> Gdx.files.local("saves/dictionary/")).thenReturn(mockDictDir);
            gdxMock.when(() -> Gdx.files.local("saves/")).thenReturn(mockSaveDir);
            gdxMock.when(() -> Gdx.files.local(anyString())).thenReturn(mockSaveFile);

            when(mockDictDir.exists()).thenReturn(true);
            when(mockSaveDir.list()).thenReturn(new FileHandle[0]);

            boolean result = gameSaveController.saveGame(testCharacter, "checksum_test", null);

            assertTrue(result);
            assertNotNull(gameSaveController.getChecksum(), "Checksum được tạo sau khi save");
        }
    }
}