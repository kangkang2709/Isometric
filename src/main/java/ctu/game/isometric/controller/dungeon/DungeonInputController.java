package ctu.game.isometric.controller.dungeon;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import ctu.game.isometric.view.screen.LinearCaveScreen;

public class DungeonInputController extends InputAdapter {
    LinearCaveScreen linearCaveScreen;

    public DungeonInputController(LinearCaveScreen screen) {
        this.linearCaveScreen = screen;
    }

    @Override
    public boolean keyUp(int keycode) {
        return super.keyUp(keycode);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return super.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return super.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return super.scrolled(amountX, amountY);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
            linearCaveScreen.getGame().changeScreen("GAME");
            System.out.println("Escape key pressed, changing screen to GAME");
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F1) {
            linearCaveScreen.setPlayerHealth(100);
            System.out.println("Back key pressed, changing screen to GAME");
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F2) {
            linearCaveScreen.setPlayerHealth(0);
            System.out.println("Back key pressed, changing screen to GAME");
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F3) {
            linearCaveScreen.setPlayerMana(50);
            System.out.println("Back key pressed, changing screen to GAME");
            return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return super.keyTyped(character);
    }
}