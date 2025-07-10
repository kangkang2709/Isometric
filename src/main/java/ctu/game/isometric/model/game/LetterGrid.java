package ctu.game.isometric.model.game;

import ctu.game.isometric.util.WordNetValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LetterGrid {
    private int GRID_SIZE = 5;
    private char[][] grid;
    private boolean[][] selectedCells;
    private List<int[]> currentSelection;

    // Letter frequencies based on English language
    private static final String LETTERS = "EEEEEEEEEEAAAAAARRRRRRIIIIIIOOOOOOTTTTTTNNNNNNSSSSSSLLLLUUUUUDDDGGBBCCMMPPFFHHVVWWYYKJXQZ";
    private Random random;

    public void toggleCellSelection(int x, int y) {
        if (canSelect(x, y)) {
            selectCell(x, y);
        } else {
            deselectCell(x,y);
        }
    }
    public boolean deselectCell(int x, int y) {
        if (x >= 0 && y >= 0 && x < GRID_SIZE && y < GRID_SIZE && selectedCells[y][x]) {
            selectedCells[y][x] = false;
            currentSelection.removeIf(pos -> pos[0] == x && pos[1] == y);
            return true;
        }
        return false;
    }
    public boolean isCellSelected(int x, int y) {
        return x >= 0 && y >= 0 && x < GRID_SIZE && y < GRID_SIZE && selectedCells[y][x];
    }

    public char getLetter(int x, int y) {
        if (x >= 0 && y >= 0 && x < GRID_SIZE && y < GRID_SIZE) {
            return grid[y][x];
        }
        throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + y + ")");
    }

    public void clearWord() {
        currentSelection.clear();
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                selectedCells[y][x] = false;
            }
        }
    }

    public LetterGrid() {
        grid = new char[GRID_SIZE][GRID_SIZE];
        selectedCells = new boolean[GRID_SIZE][GRID_SIZE];
        currentSelection = new ArrayList<>();
        random = new Random();
        regenerateGrid();
    }

    public void setGridSize(int size) {
        if (size >= 3 && size <= 10) { // Limit grid size to a reasonable range
            GRID_SIZE = size;
            grid = new char[GRID_SIZE][GRID_SIZE];
            selectedCells = new boolean[GRID_SIZE][GRID_SIZE];
            currentSelection.clear();
            regenerateGrid();
        } else {
            throw new IllegalArgumentException("Grid size must be between 1 and 10.");
        }
    }

    public int getGridSize() {
        return GRID_SIZE;
    }


    int vovelCount = 0;
    int vovelLimit = 4;

    public void regenerateGrid() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                grid[y][x] = getRandomLetter();
                selectedCells[y][x] = false;
            }
        }

        // Ensure there is at least one vowel in the grid
        if (!hasVowel()) {
            // Add a random vowel at a random position
            int randomX = random.nextInt(GRID_SIZE);
            int randomY = random.nextInt(GRID_SIZE);
            grid[randomY][randomX] = getRandomVowel();
        }

        currentSelection.clear();
    }

    private char getRandomLetter() {
        return LETTERS.charAt(random.nextInt(LETTERS.length()));
    }

    private boolean hasVowel() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (isVowel(grid[y][x])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isVowel(char c) {
        return "AEIOU".indexOf(c) != -1;
    }

    private char getRandomVowel() {
        String vowels = "AEIOU";
        return vowels.charAt(random.nextInt(vowels.length()));
    }

    public boolean canSelect(int x, int y) {
        // Only check if within grid bounds and not already selected
        return x >= 0 && y >= 0 && x < GRID_SIZE && y < GRID_SIZE && !selectedCells[y][x];
    }

    public void selectCell(int x, int y) {
        if (canSelect(x, y)) {
            selectedCells[y][x] = true;
            currentSelection.add(new int[]{x, y});
        }
    }

    public void deselectLastCell() {
        if (!currentSelection.isEmpty()) {
            int[] last = currentSelection.remove(currentSelection.size() - 1);
            selectedCells[last[1]][last[0]] = false;
        }
    }

    public void clearSelection() {
        for (int[] pos : currentSelection) {
            selectedCells[pos[1]][pos[0]] = false;
        }
        currentSelection.clear();
    }

    public String getCurrentWord() {
        StringBuilder word = new StringBuilder();
        for (int[] pos : currentSelection) {
            word.append(grid[pos[1]][pos[0]]);
        }
        return word.toString();
    }

    // Getters
    public char[][] getGrid() {
        return grid;
    }

    public boolean[][] getSelectedCells() {
        return selectedCells;
    }

    public List<int[]> getCurrentSelection() {
        return currentSelection;
    }
}