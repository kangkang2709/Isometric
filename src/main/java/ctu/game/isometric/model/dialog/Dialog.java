// src/main/java/ctu/game/flatformer/model/visualnovel/Dialog.java
package ctu.game.isometric.model.dialog;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Dialog {
    private String text;

    @JsonProperty("character")
    private String character;
    private String characterImage;

    public Dialog(String text, String characterName, String characterImage) {
        this.text = text;
        this.character = characterName;
        this.characterImage = characterImage;
    }

    public Dialog() {

    }

    public String getText() {
        return text;
    }

    public String getCharacterName() {
        return character;
    }

    public String getCharacterImage() {
        return characterImage;
    }
    public void setText(String text) {
        this.text = text;
    }
}