package ctu.game.isometric.controller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

public class SettingsMenuController {
    private GameController gameController;
    private List<MenuItem> menuItems;
    private int selectedIndex;
    private BitmapFont font;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private float itemHeight = 50f;
    private float padding = 20f;

    public SettingsMenuController(GameController gameController) {
        this.gameController = gameController;
        this.menuItems = new ArrayList<>();
        this.selectedIndex = 0;
        this.font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Add settings menu items
        addMenuItem("Toggle Music", this::toggleMusic);
        addMenuItem("Adjust Volume", this::adjustVolume);
        addMenuItem("Back to Main Menu", gameController::returnToPreviousState);

        // Set menu dimensions
        menuWidth = 400f;
        menuHeight = (menuItems.size() * itemHeight) + (padding * 2);
        menuX = 400 - menuWidth / 2;
        menuY = 300 - menuHeight / 2;
    }

    private void toggleMusic() {
        System.out.println("Music toggled");
        // Add logic to toggle music on/off
    }

    private void adjustVolume() {
        System.out.println("Volume adjustment selected");
        // Add logic to adjust volume
    }

    public void addMenuItem(String text, Runnable action) {
        menuItems.add(new MenuItem(text, action));
    }

    public void selectNextItem() {
        selectedIndex = (selectedIndex + 1) % menuItems.size();
    }

    public void selectPreviousItem() {
        selectedIndex = (selectedIndex - 1 + menuItems.size()) % menuItems.size();
    }

    public void activateSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < menuItems.size()) {
            menuItems.get(selectedIndex).activate();
        }
    }

    public void render(SpriteBatch batch) {
        float y = menuY + menuHeight - padding - itemHeight / 2;

        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);

            if (i == selectedIndex) {
                font.setColor(Color.YELLOW);
            } else {
                font.setColor(Color.WHITE);
            }

            font.draw(batch, item.getText(), menuX + padding, y, menuWidth - padding * 2, Align.center, false);
            y -= itemHeight;
        }
    }

    public void dispose() {
        if (font != null) {
            font.dispose();
        }
    }

    private static class MenuItem {
        private String text;
        private Runnable action;

        public MenuItem(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }

        public String getText() {
            return text;
        }

        public void activate() {
            if (action != null) {
                action.run();
            }
        }
    }
}