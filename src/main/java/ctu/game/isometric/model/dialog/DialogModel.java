package ctu.game.isometric.model.dialog;

public class DialogModel {
    private String currentText;
    private boolean isActive;

    public DialogModel() {
        this.currentText = "";
        this.isActive = false;
    }

    public String getCurrentText() { return currentText; }
    public boolean isActive() { return isActive; }

    public void setText(String text) {
        this.currentText = text;
        this.isActive = true;
    }

    public void hide() {
        this.isActive = false;
    }
}