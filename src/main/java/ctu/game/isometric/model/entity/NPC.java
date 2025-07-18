package ctu.game.isometric.model.entity;

public class NPC {
    int npcID;
    String npcName;
    String npcDescription;
    String arcId;
    String sceneId;
    String npcImage;
    String npcBackStory;
    String behaviorState;
    // Additional attributes can be added as needed
    int xPosition;
    int yPosition;

    String mapName;

    public NPC() {
        this.behaviorState = "idle";
    }

    public NPC(int npcID, String npcName, String npcDescription, String arcId, String sceneId, String npcImage, String npcBackStory, String behaviorState, int xPosition, int yPosition) {
        this.npcID = npcID;
        this.npcName = npcName;
        this.npcDescription = npcDescription;
        this.arcId = arcId;
        this.sceneId = sceneId;
        this.npcImage = npcImage;
        this.npcBackStory = npcBackStory;
        this.behaviorState = behaviorState;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public int getyPosition() {
        return yPosition;
    }

    public void setyPosition(int yPosition) {
        this.yPosition = yPosition;
    }

    public int getxPosition() {
        return xPosition;
    }

    public void setxPosition(int xPosition) {
        this.xPosition = xPosition;
    }

    public void interact() {
        // Logic for interaction with the NPC
    }

    public String getNpcImage() {
        return npcImage;
    }

    public void setNpcImage(String npcImage) {
        this.npcImage = npcImage;
    }

    public String getNpcBackStory() {
        return npcBackStory;
    }

    public void setNpcBackStory(String npcBackStory) {
        this.npcBackStory = npcBackStory;
    }

    public String getBehaviorState() {
        return behaviorState;
    }

    public void setBehaviorState(String behaviorState) {
        this.behaviorState = behaviorState;
    }




    public int getXPosition() {
        return xPosition;
    }

    public void setXPosition(int xPosition) {
        this.xPosition = xPosition;
    }

    public int getYPosition() {
        return yPosition;
    }

    public void setYPosition(int yPosition) {
        this.yPosition = yPosition;
    }



    public int getNpcID() {
        return npcID;
    }

//    public void interact(Character player, String action) {
//        // Check if NPC has quests for the player
//        for (Quest quest : quests) {
//            if (!player.hasQuest(quest) && !quest.isCompleted()) {
//                // Offer a new quest
//                return;
//            } else if (player.hasQuest(quest) && !quest.isCompleted()) {
//                // Check if the quest can be completed
//                return;
//            }
//        }
//
//
//    }


    public void setNpcID(int npcID) {
        this.npcID = npcID;
    }

    public String getNpcName() {
        return npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    public String getNpcDescription() {
        return npcDescription;
    }

    public void setNpcDescription(String npcDescription) {
        this.npcDescription = npcDescription;
    }

    public String getArcId() {
        return arcId;
    }

    public void setArcId(String arcId) {
        this.arcId = arcId;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }
}