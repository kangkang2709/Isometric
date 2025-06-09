package ctu.game.isometric.model.game;

public class Reward {
    private int rewardID;
    private int itemID; // Name of the item if the reward is an item
    private int amount; // Amount of the reward
    private String description; // Description of the reward

    public Reward() {
    }



    public int getRewardID() {
        return rewardID;
    }

    public void setRewardID(int rewardID) {
        this.rewardID = rewardID;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
