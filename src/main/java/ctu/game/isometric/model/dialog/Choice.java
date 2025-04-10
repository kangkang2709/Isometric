package ctu.game.isometric.model.dialog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {
    private String text;
    private String next_scene;
    private String required_item;
    private int sanity_change;

    // Getters and setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNextScene() {
        return next_scene;
    }

    public String getNext_scene() {
        return next_scene;
    }

    public void setNext_scene(String next_scene) {
        this.next_scene = next_scene;
    }

    public int getSanity_change() {
        return sanity_change;
    }

    public void setSanity_change(int sanity_change) {
        this.sanity_change = sanity_change;
    }

    public String getRequired_item() {
        return required_item;
    }

    public void setRequired_item(String required_item) {
        this.required_item = required_item;
    }

    public void setNextScene(String nextScene) {
        this.next_scene = nextScene;
    }

    public String getRequiredItem() {
        return required_item;
    }

    public void setRequiredItem(String requiredItem) {
        this.required_item = requiredItem;
    }
}