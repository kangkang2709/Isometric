package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import ctu.game.isometric.model.game.Achievement;
import ctu.game.isometric.model.entity.Character;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AchievementManager implements Disposable {
    private ObjectMap<String, Achievement> achievements;
    private Array<Achievement> recentlyUnlocked;
    private GameController gameController;


    public AchievementManager(GameController gameController) {
        achievements = new ObjectMap<>();
        recentlyUnlocked = new Array<>();
        this.gameController = gameController;
        loadAchievements();
    }

    public void loadAchievements() {
        try {
            FileHandle file = Gdx.files.internal("game/achievements.json");
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);

            for (JsonValue achievementJson : root) {
                Achievement achievement = new Achievement();
                achievement.setId(achievementJson.getString("id"));
                achievement.setTitle(achievementJson.getString("title"));
                achievement.setDescription(achievementJson.getString("description"));
                achievement.setIconPath(achievementJson.getString("iconPath", "ui/default.png"));
                achievement.setType(Achievement.AchievementType.valueOf(achievementJson.getString("type")));
                achievement.setTargetValue(achievementJson.getInt("targetValue"));
                achievement.setCurrentValue(0);

                // Load target enemy if applicable
                if (achievementJson.has("targetEnemy")) {
                    achievement.setTargetEnemy(achievementJson.getString("targetEnemy"));
                }

                achievements.put(achievement.getId(), achievement);
            }

        } catch (Exception e) {
            Gdx.app.error("AchievementManager", "Error loading achievements", e);
        }
    }


    public void setProgressForCharater() {
        HashSet<Achievement> achievementSet = new HashSet<>();
        for (Achievement achievement : achievements.values()) {
            achievementSet.add(achievement);
        }
        gameController.getCharacter().setAchievements(achievementSet);
    }

    public Array<Achievement> getCharacterAchievements() {
        Array<Achievement> result = new Array<>();
        Character character = gameController.getCharacter();
        if (character == null) return result;

        for (Achievement achievement : character.getAchievements()) {
            result.add(achievement);
        }
        return result;
    }

    public void updateProgress(Achievement.AchievementType type, int value) {
                gameController.getCharacter().updateAchievements(type, value);
    }



    @Override
    public void dispose() {
        // Save any pending changes and release resources
//        saveProgress();
        achievements.clear();
        recentlyUnlocked.clear();
    }
}