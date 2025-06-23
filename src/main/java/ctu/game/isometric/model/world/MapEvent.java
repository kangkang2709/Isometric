package ctu.game.isometric.model.world;

import com.badlogic.gdx.maps.MapProperties;

public class MapEvent {
    private String id;          // Unique identifier for the event
    private String eventType;   // Type of event (battle, dialog, etc.)
    private int gridX;
    private int gridY;
    private MapProperties properties;
    private boolean completed;  // Whether this event has been completed
    private boolean isOneTime;

    public MapEvent(String id, String eventType, int gridX, int gridY, MapProperties properties, boolean isOneTime) {
        this.id = id;
        this.eventType = eventType;
        this.gridX = gridX;
        this.gridY = gridY;
        this.properties = properties;
        this.completed = false;
        this.isOneTime = isOneTime;

    }

    public MapEvent(String id, String eventType, int gridX, int gridY,String name, String enemyIDOritemId) {
        this.id = id;
        this.eventType = eventType;
        this.gridX = gridX;
        this.gridY = gridY;

        MapProperties properties = new MapProperties();
        properties.put("walkable", true);
        properties.put("isOneTime", true);
        properties.put("id", "board_" + id);
        switch (eventType) {
            case "battle":
                properties.put("enemy", enemyIDOritemId);
                properties.put("enemyName", name);
                break;
            case "treasure":
                properties.put("amount", 1);
                properties.put("item", enemyIDOritemId);
                properties.put("itemName", name);
                break;
            default:
                properties.put("event", eventType);
                break;
        }

        this.properties = properties;

        this.completed = false;
        this.isOneTime = true;

    }

    public void updateEvent(String eventId) {
        this.completed = true; // Mark the event as completed
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public MapProperties getProperties() {
        return properties;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isOneTime() {
        return isOneTime;
    }

    public void setOneTime(boolean oneTime) {
        this.isOneTime = oneTime;
    }

    @Override
    public String toString() {
        return "MapEvent{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", gridX=" + gridX +
                ", gridY=" + gridY +
                ", properties=" + properties +
                ", completed=" + completed +
                ", isOneTime=" + isOneTime +
                '}';
    }
}