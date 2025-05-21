package ctu.game.isometric.model.entity;

public class Enemy {
    private int enemyID;
    private String enemyName;
    private String enemyDescription;
    private String texturePath;
    private float health;
    private int attackPower;
    private int rewardID; // ID of the reward given upon defeat
    private int x,y;
    public Enemy() {
    }

    public Enemy(int enemyID, String enemyDescription, String enemyName, String texturePath, int health, int attackPower, int rewardID) {
        this.enemyID = enemyID;
        this.enemyDescription = enemyDescription;
        this.enemyName = enemyName;
        this.texturePath = texturePath;
        this.health = health;
        this.attackPower = attackPower;
        this.rewardID = rewardID;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getEnemyID() {
        return enemyID;
    }

    public void setEnemyID(int enemyID) {
        this.enemyID = enemyID;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public void setEnemyName(String enemyName) {
        this.enemyName = enemyName;
    }

    public String getEnemyDescription() {
        return enemyDescription;
    }

    public void setEnemyDescription(String enemyDescription) {
        this.enemyDescription = enemyDescription;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public void setAttackPower(int attackPower) {
        this.attackPower = attackPower;
    }

    public int getRewardID() {
        return rewardID;
    }

    public void setRewardID(int rewardID) {
        this.rewardID = rewardID;
    }
}
