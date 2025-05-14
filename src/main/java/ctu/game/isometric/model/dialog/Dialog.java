package ctu.game.isometric.model.dialog;

public class Dialog {
    private String character;
    private String characterImage;
    private String text;

    // Getters and setters
    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getCharacterImage() {
        return characterImage;
    }

    public void setCharacterImage(String characterImage) {
        this.characterImage = characterImage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    // For convenience when accessing character name
    public String getCharacterName() {
        return character != null ? character : "???";
    }


}