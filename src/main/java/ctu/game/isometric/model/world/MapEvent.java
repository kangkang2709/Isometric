package ctu.game.isometric.model.world;

import com.badlogic.gdx.maps.MapProperties;

public class MapEvent {
    private String id;          // Unique identifier for the event
    private String eventType;   // Type of event (battle, dialog, etc.)
    private int gridX;
    private int gridY;
    private MapProperties properties;
    private boolean completed;  // Whether this event has been completed
    private boolean isOneTime; // Whether this event is a one-time event
    public MapEvent(String id, String eventType, int gridX, int gridY, MapProperties properties,boolean isOneTime) {
        this.id = id;
        this.eventType = eventType;
        this.gridX = gridX;
        this.gridY = gridY;
        this.properties = properties;
        this.completed = false;
        this.isOneTime = isOneTime;

    }

    // Getters and setters
    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public MapProperties getProperties() { return properties; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isOneTime() { return isOneTime; }
    public void setOneTime(boolean oneTime) { this.isOneTime = oneTime; }
}