package ctu.game.isometric.model.game;

import ctu.game.isometric.model.entity.Character;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameSave {
    private Character character;
    private Date saveDate;
    private String saveName;
    private String wordFilePath;
    private List<String> listIdCompletedEvents;
    private List<Integer> listIdDefeatedEnemies;


    public GameSave() {
        listIdCompletedEvents = new ArrayList<>();
        listIdDefeatedEnemies = new ArrayList<>();
    }

    // Getters and setters
    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Date getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(Date saveDate) {
        this.saveDate = saveDate;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getWordFilePath() {
        return wordFilePath;
    }

    public void setWordFilePath(String wordFilePath) {
        this.wordFilePath = wordFilePath;
    }

    public List<String> getListIdCompletedEvents() {
        return listIdCompletedEvents;
    }

    public void setListIdCompletedEvents(List<String> listIdCompletedEvents) {
        this.listIdCompletedEvents = listIdCompletedEvents;
    }

    public List<Integer> getListIdDefeatedEnemies() {
        return listIdDefeatedEnemies;
    }

    public void setListIdDefeatedEnemies(List<Integer> listIdDefeatedEnemies) {
        this.listIdDefeatedEnemies = listIdDefeatedEnemies;
    }
}