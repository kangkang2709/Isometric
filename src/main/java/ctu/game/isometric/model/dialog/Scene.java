package ctu.game.isometric.model.dialog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scene {
    private String id;
    private String background;
    private String music;
    private List<Dialog> dialogues;
    private List<String> sound_effects;
    private List<Choice> choices;
    private Map<String, Object> conditions;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getSound_effects() {
        return sound_effects;
    }

    public void setSound_effects(List<String> sound_effects) {
        this.sound_effects = sound_effects;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }

    public List<Dialog> getDialogues() {
        return dialogues;
    }

    public void setDialogues(List<Dialog> dialogues) {
        this.dialogues = dialogues;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }
}