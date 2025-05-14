package ctu.game.isometric.model.dialog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {
    private String text;
    private String next_scene;
    private String required_item;
    private String reward_item;
    // Getters and setters

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNext_scene() {
        return next_scene;
    }

    public void setNext_scene(String next_scene) {
        this.next_scene = next_scene;
    }

    public String getRequired_item() {
        return required_item;
    }

    public void setRequired_item(String required_item) {
        this.required_item = required_item;
    }

    public String getReward_item() {
        return reward_item;
    }

    public void setReward_item(String reward_item) {
        this.reward_item = reward_item;
    }
}