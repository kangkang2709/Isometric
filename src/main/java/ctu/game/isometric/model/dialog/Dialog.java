package ctu.game.isometric.model.dialog;

public class Dialog {
    private String character;
    private String characterImage;
    private String text;
    private String expression; // Add this missing field

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

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    // For convenience when accessing character name
    public String getCharacterName() {
        return character != null ? character : "???";
    }
}