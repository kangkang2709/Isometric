package ctu.game.isometric.model.game;

public class Tutorial implements Comparable<Tutorial> {
    private String id;
    private String title;
    private String text;
    private String image;
    private String tutorialType;
    private boolean isCompleted;

    public Tutorial(String id, String title, String text, String image, String tutorialType, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.image = image;
        this.tutorialType = tutorialType;
        this.isCompleted = isCompleted;
    }

    public Tutorial() {
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTutorialType() {
        return tutorialType;
    }

    public void setTutorialType(String tutorialType) {
        this.tutorialType = tutorialType;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    @Override
    public int compareTo(Tutorial other) {
        return this.id.compareTo(other.id);
    }
}