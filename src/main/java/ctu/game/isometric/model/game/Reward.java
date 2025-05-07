package ctu.game.isometric.model.game;

public class Reward {
    private int rewardID;
    private Items itemID; // Name of the item if the reward is an item
    private int amount; // Amount of the reward
    private String description; // Description of the reward

    public Reward() {
    }

    public Reward(int rewardID, Items itemID, int amount, String description) {
        this.rewardID = rewardID;
        this.itemID = itemID;
        this.amount = amount;
        this.description = description;
    }

    public int getRewardID() {
        return rewardID;
    }

    public void setRewardID(int rewardID) {
        this.rewardID = rewardID;
    }

    public Items getItemID() {
        return itemID;
    }

    public void setItemID(Items itemID) {
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
