package ctu.game.isometric.model.game;

public class Items {
    private int itemID;
    private String itemName;
    private String itemDescription;
    private String texturePath; // Path to the item's texture
    private String itemEffect; // Effect of the item when used (e.g., heal, buff)

    public int getItemID() {
        return itemID;
    }

    public Items() {
    }

    public Items(int itemID, String itemName, String itemDescription, String texturePath, String itemEffect) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.texturePath = texturePath;
        this.itemEffect = itemEffect;
    }

    public String getItemEffect() {
        return itemEffect;
    }

    public void setItemEffect(String itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }
}
