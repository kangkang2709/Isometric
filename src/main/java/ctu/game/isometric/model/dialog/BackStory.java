package ctu.game.isometric.model.dialog;

public class BackStory {
    private String characterName;
    private String chapter;
    private String text;

    public BackStory(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
