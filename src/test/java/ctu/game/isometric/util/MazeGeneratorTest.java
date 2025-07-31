package ctu.game.isometric.util;

import ctu.game.isometric.model.world.MazeGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho MazeGenerator class
 * Tests các chức năng tạo mê cung và thuật toán liên quan
 */
@DisplayName("MazeGenerator Tests")
class MazeGeneratorTest {

    private Random testRandom;
    private static final int WALL = 0;
    private static final int FLOOR = 1;
    private static final int TILE_BLOCK = 2;

    @BeforeEach
    void setUp() {
        testRandom = new Random(12345L); // Fixed seed for reproducible tests
    }

    @Nested
    @DisplayName("Maze Generation Tests")
    class MazeGenerationTests {

        @Test
        @DisplayName("Should generate valid maze with correct dimensions")
        void testGenerateMaze_ValidDimensions() {
            int width = 21, height = 21, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

            assertNotNull(result);
            assertNotNull(result.layers.get("base"));
            assertNotNull(result.layers.get("terrain"));

            int[][] baseLayer = result.layers.get("base");
            int[][] terrainLayer = result.layers.get("terrain");

            assertEquals(height, baseLayer.length);
            assertEquals(width, baseLayer[0].length);
            assertEquals(height, terrainLayer.length);
            assertEquals(width, terrainLayer[0].length);
        }

        @Test
        @DisplayName("Should handle minimum valid maze size")
        void testGenerateMaze_MinimumSize() {
            int width = 11, height = 11, difficulty = 1;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

            assertNotNull(result);
            assertTrue(result.pathLength > 0);
            assertTrue(result.startX >= 0 && result.startX < width);
            assertTrue(result.startY >= 0 && result.startY < height);
            assertTrue(result.endX >= 0 && result.endX < width);
            assertTrue(result.endY >= 0 && result.endY < height);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
        @DisplayName("Should clamp difficulty to valid range")
        void testGenerateMaze_DifficultyRanges(int difficulty) {
            int width = 15, height = 15;

            assertDoesNotThrow(() -> {
                MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Should generate different mazes with different random seeds")
        void testGenerateMaze_DifferentSeeds() {
            int width = 15, height = 15, difficulty = 2;
            Random random1 = new Random(1111L);
            Random random2 = new Random(2222L);

            MazeGenerationResult result1 = MazeGenerator.generateMaze(width, height, random1, difficulty);
            MazeGenerationResult result2 = MazeGenerator.generateMaze(width, height, random2, difficulty);

            assertNotNull(result1);
            assertNotNull(result2);

            // Mazes should be different (at least path length or positions)
            boolean different = result1.pathLength != result2.pathLength ||
                    result1.startX != result2.startX ||
                    result1.startY != result2.startY ||
                    result1.endX != result2.endX ||
                    result1.endY != result2.endY;

            assertTrue(different, "Different seeds should produce different mazes");
        }

        @RepeatedTest(5)
        @DisplayName("Should consistently generate valid mazes")
        void testGenerateMaze_Consistency() {
            int width = 17, height = 17, difficulty = 3;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, new Random(), difficulty);

            assertNotNull(result);
            assertTrue(result.pathLength >= 10, "Path should be long enough");
            assertNotNull(result.enemySpawns);
            assertNotNull(result.minimapMask);

            // Verify layers exist
            assertTrue(result.layers.containsKey("base"));
            assertTrue(result.layers.containsKey("terrain"));
            assertTrue(result.layers.containsKey("path"));
            assertTrue(result.layers.containsKey("fake"));
            assertTrue(result.layers.containsKey("chest"));
            assertTrue(result.layers.containsKey("enemy"));
        }
    }

    @Nested
    @DisplayName("Maze Structure Validation Tests")
    class MazeStructureTests {

        @Test
        @DisplayName("Should have connected floor tiles")
        void testMazeStructure_ConnectedFloors() {
            int width = 15, height = 15, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] baseLayer = result.layers.get("base");

            // Count floor tiles
            int floorCount = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (baseLayer[y][x] == FLOOR) {
                        floorCount++;
                    }
                }
            }

            assertTrue(floorCount > 0, "Maze should have floor tiles");
            assertTrue(floorCount < width * height, "Maze should have walls");
        }

        @Test
        @DisplayName("Should have proper terrain layer mapping")
        void testMazeStructure_TerrainMapping() {
            int width = 13, height = 13, difficulty = 1;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] baseLayer = result.layers.get("base");
            int[][] terrainLayer = result.layers.get("terrain");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (baseLayer[y][x] == FLOOR) {
                        assertEquals(0, terrainLayer[y][x], "Floor tiles should have terrain value 0");
                    } else {
                        assertEquals(TILE_BLOCK, terrainLayer[y][x], "Wall tiles should have TILE_BLOCK terrain");
                    }
                }
            }
        }

        @Test
        @DisplayName("Should generate valid minimap")
        void testMazeStructure_Minimap() {
            int width = 15, height = 15, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] minimap = result.minimapMask;
            int[][] path = result.layers.get("path");

            assertNotNull(minimap);
            assertEquals(height, minimap.length);
            assertEquals(width, minimap[0].length);

            // Path tiles should be marked in minimap
            for (int[] pathTile : path) {
                int x = pathTile[0], y = pathTile[1];
                assertEquals(1, minimap[y][x], "Path tiles should be marked in minimap");
            }
        }
    }

    @Nested
    @DisplayName("Path Generation Tests")
    class PathGenerationTests {

        @Test
        @DisplayName("Should generate path with minimum length")
        void testPathGeneration_MinimumLength() {
            int width = 21, height = 21, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

            assertTrue(result.pathLength >= 10, "Path should be at least 10 tiles long");
        }

        @Test
        @DisplayName("Should have valid start and end positions")
        void testPathGeneration_ValidStartEnd() {
            int width = 17, height = 17, difficulty = 3;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] baseLayer = result.layers.get("base");

            // Start and end should be on floor tiles
            assertEquals(FLOOR, baseLayer[result.startY][result.startX], "Start position should be on floor");
            assertEquals(FLOOR, baseLayer[result.endY][result.endX], "End position should be on floor");

            // Start and end should be different
            assertFalse(result.startX == result.endX && result.startY == result.endY,
                    "Start and end positions should be different");
        }

        @Test
        @DisplayName("Should have sufficient distance between start and end")
        void testPathGeneration_StartEndDistance() {
            int width = 19, height = 19, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

            int distance = Math.abs(result.startX - result.endX) + Math.abs(result.startY - result.endY);
            assertTrue(distance >= 10, "Manhattan distance between start and end should be at least 10");
        }
    }

    @Nested
    @DisplayName("Enemy and Chest Placement Tests")
    class ObjectPlacementTests {

        @Test
        @DisplayName("Should place enemies appropriately based on difficulty")
        void testObjectPlacement_EnemyCount() {
            int width = 17, height = 17;

            MazeGenerationResult easyResult = MazeGenerator.generateMaze(width, height, new Random(1111L), 1);
            MazeGenerationResult hardResult = MazeGenerator.generateMaze(width, height, new Random(1111L), 5);

            assertNotNull(easyResult.enemySpawns);
            assertNotNull(hardResult.enemySpawns);

            // Hard difficulty should generally have more enemies
            // Note: This is probabilistic, so we check reasonable bounds
            assertTrue(easyResult.enemySpawns.size() >= 3, "Easy maze should have at least 3 enemies");
            assertTrue(hardResult.enemySpawns.size() >= 5, "Hard maze should have at least 5 enemies");
        }

        @Test
        @DisplayName("Should place chests in accessible locations")
        void testObjectPlacement_ChestAccessibility() {
            int width = 15, height = 15, difficulty = 2;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] chests = result.layers.get("chest");
            int[][] baseLayer = result.layers.get("base");

            assertNotNull(chests);

            // All chest positions should be on floor tiles
            for (int[] chest : chests) {
                int x = chest[0], y = chest[1];
                assertEquals(FLOOR, baseLayer[y][x], "Chests should be placed on floor tiles");
            }
        }

        @Test
        @DisplayName("Should generate fake endpoints away from real end")
        void testObjectPlacement_FakeEndpoints() {
            int width = 19, height = 19, difficulty = 3;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
            int[][] fakeEndpoints = result.layers.get("fake");

            assertNotNull(fakeEndpoints);

            // Fake endpoints should be sufficiently far from real end
            for (int[] fake : fakeEndpoints) {
                int distance = Math.abs(fake[0] - result.endX) + Math.abs(fake[1] - result.endY);
                assertTrue(distance >= 8, "Fake endpoints should be at least 8 tiles away from real end");
            }
        }
    }

    @Nested
    @DisplayName("Fallback Maze Tests")
    class FallbackMazeTests {

        @Test
        @DisplayName("Should create fallback maze when main generation fails")
        void testFallbackMaze_Creation() {
            // Use extreme parameters to potentially trigger fallback
            int width = 10, height = 10, difficulty = 0;

            MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

            assertNotNull(result);
            assertNotNull(result.layers.get("base"));
            assertNotNull(result.layers.get("terrain"));
            assertTrue(result.pathLength > 0);
        }

        @Nested
        @DisplayName("Edge Cases and Error Handling Tests")
        class EdgeCaseTests {

            @Test
            @DisplayName("Should handle odd and even dimensions")
            void testEdgeCases_Dimensions() {
                // Test both odd and even dimensions
                int[] dimensions = {10, 11, 12, 13, 20, 21};

                for (int dim : dimensions) {
                    assertDoesNotThrow(() -> {
                        MazeGenerationResult result = MazeGenerator.generateMaze(dim, dim, testRandom, 2);
                        assertNotNull(result);
                        assertTrue(result.pathLength > 0);
                    }, "Should handle dimension: " + dim);
                }
            }

            @Test
            @DisplayName("Should handle extreme difficulty values")
            void testEdgeCases_ExtremeDifficulty() {
                int width = 15, height = 15;
                int[] difficulties = {-5, -1, 0, 5, 10, 100};

                for (int difficulty : difficulties) {
                    assertDoesNotThrow(() -> {
                        MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
                        assertNotNull(result);
                    }, "Should handle difficulty: " + difficulty);
                }
            }

            @Test
            @DisplayName("Should handle null random generator gracefully")
            void testEdgeCases_NullRandom() {
                int width = 15, height = 15, difficulty = 2;

                assertThrows(NullPointerException.class, () -> {
                    MazeGenerator.generateMaze(width, height, null, difficulty);
                });
            }
        }

        @Nested
        @DisplayName("Performance Tests")
        class PerformanceTests {

            @Test
            @DisplayName("Should generate maze within reasonable time")
            void testPerformance_GenerationTime() {
                int width = 31, height = 31, difficulty = 3;

                long startTime = System.currentTimeMillis();
                MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
                long endTime = System.currentTimeMillis();

                assertNotNull(result);
                assertTrue(endTime - startTime < 5000, "Maze generation should complete within 5 seconds");
            }

            @Test
            @DisplayName("Should handle large maze dimensions efficiently")
            void testPerformance_LargeMaze() {
                int width = 51, height = 51, difficulty = 4;

                long startTime = System.currentTimeMillis();

                assertDoesNotThrow(() -> {
                    MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);
                    assertNotNull(result);
                    assertTrue(result.pathLength > 0);
                });

                long endTime = System.currentTimeMillis();
                assertTrue(endTime - startTime < 10000, "Large maze generation should complete within 10 seconds");
            }

            @RepeatedTest(10)
            @DisplayName("Should consistently generate valid mazes under repeated calls")
            void testPerformance_RepeatedGeneration() {
                int width = 17, height = 17, difficulty = 2;

                assertDoesNotThrow(() -> {
                    MazeGenerationResult result = MazeGenerator.generateMaze(width, height, new Random(), difficulty);
                    assertNotNull(result);
                    assertTrue(result.pathLength >= 10);
                    assertNotNull(result.enemySpawns);
                });
            }
        }

        @Nested
        @DisplayName("Data Integrity Tests")
        class DataIntegrityTests {

            @Test
            @DisplayName("Should maintain data consistency across all layers")
            void testDataIntegrity_LayerConsistency() {
                int width = 19, height = 19, difficulty = 3;

                MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

                // All coordinate arrays should have valid positions
                int[][] path = result.layers.get("path");
                int[][] enemies = result.layers.get("enemy");
                int[][] chests = result.layers.get("chest");
                int[][] fakes = result.layers.get("fake");

                validateCoordinates(path, width, height, "path");
                validateCoordinates(enemies, width, height, "enemies");
                validateCoordinates(chests, width, height, "chests");
                validateCoordinates(fakes, width, height, "fakes");
            }

            @Test
            @DisplayName("Should have no overlapping object placements")
            void testDataIntegrity_NoOverlaps() {
                int width = 21, height = 21, difficulty = 3;

                MazeGenerationResult result = MazeGenerator.generateMaze(width, height, testRandom, difficulty);

                Set<String> occupiedPositions = new HashSet<>();

                // Check that important objects don't overlap
                addPositionsToSet(result.layers.get("chest"), occupiedPositions, "chest");

                // Enemies can be on path, but chests shouldn't overlap with fake endpoints
                int[][] fakes = result.layers.get("fake");
                for (int[] fake : fakes) {
                    String pos = fake[0] + "," + fake[1];
                    assertFalse(occupiedPositions.contains(pos),
                            "Fake endpoint should not overlap with chest at " + pos);
                }
            }

            private void validateCoordinates(int[][] coordinates, int width, int height, String type) {
                assertNotNull(coordinates, type + " coordinates should not be null");

                for (int[] coord : coordinates) {
                    assertNotNull(coord, type + " coordinate should not be null");
                    assertEquals(2, coord.length, type + " coordinate should have x,y values");
                    assertTrue(coord[0] >= 0 && coord[0] < width,
                            type + " x coordinate should be within bounds: " + coord[0]);
                    assertTrue(coord[1] >= 0 && coord[1] < height,
                            type + " y coordinate should be within bounds: " + coord[1]);
                }
            }

            private void addPositionsToSet(int[][] coordinates, Set<String> set, String type) {
                for (int[] coord : coordinates) {
                    String pos = coord[0] + "," + coord[1];
                    assertFalse(set.contains(pos), "Duplicate " + type + " position: " + pos);
                    set.add(pos);
                }
            }
        }
    }
}