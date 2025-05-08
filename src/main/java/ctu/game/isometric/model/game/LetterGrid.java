package ctu.game.isometric.model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LetterGrid {
    private static final int GRID_SIZE = 5;
    private char[][] grid;
    private boolean[][] selectedCells;
    private List<int[]> currentSelection;

    // Letter frequencies based on English language
    private static final String LETTERS = "EEEEEEEEEEAAAAAARRRRRRIIIIIIOOOOOOTTTTTTNNNNNNSSSSSSLLLLUUUUUDDDGGBBCCMMPPFFHHVVWWYYKJXQZ";
    private Random random;

    public LetterGrid() {
        grid = new char[GRID_SIZE][GRID_SIZE];
        selectedCells = new boolean[GRID_SIZE][GRID_SIZE];
        currentSelection = new ArrayList<>();
        random = new Random();
        regenerateGrid();
    }

    public void regenerateGrid() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                grid[y][x] = getRandomLetter();
                selectedCells[y][x] = false;
            }
        }
        currentSelection.clear();
    }

    private char getRandomLetter() {
        return LETTERS.charAt(random.nextInt(LETTERS.length()));
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
    public char[][] getGrid() { return grid; }
    public boolean[][] getSelectedCells() { return selectedCells; }
    public List<int[]> getCurrentSelection() { return currentSelection; }
}