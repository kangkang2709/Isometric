package ctu.game.isometric.controller;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventManager {
    private Map<String, MapEvent> events = new HashMap<>();
    private Map<Integer, Boolean> defeatedEnemies = new HashMap<>();

    // Load events from the map
    public void loadEventsFromMap(IsometricMap map) {
        MapLayer objectLayer = map.getTiledMap().getLayers().get("object");
        if (objectLayer != null) {
            for (MapObject object : objectLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    int objGridX = (int) (rect.x / map.getTileWidth()) + 2;
                    int objGridY = (int) (rect.y / map.getTileHeight()) - 2;

                    MapProperties props = object.getProperties();
                    if (props.containsKey("event")) {
                        String eventType = getStringProperty(props, "event", "");
                        String eventId = props.containsKey("id") ?
                                getStringProperty(props, "id", "") :
                                "event_" + objGridX + "_" + objGridY;
                        boolean isOneTime = false; // Declare and initialize outside the block
                        if (props.containsKey("one_time"))
                            isOneTime = getBooleanProperty(props, "one_time", false);
                        events.put(eventId, new MapEvent(eventId, eventType, objGridX, objGridY, props,isOneTime));
                    }
                }
            }
        }
    }


    // Check for events at a position
    public MapEvent checkPositionEvents(float x, float y) {
        int gridX = (int) x;
        int gridY = (int) y;

        for (MapEvent event : events.values()) {
            if (event.getGridX() == gridX && event.getGridY() == gridY) {
                // For one-time events, check if already completed
                if (event.isOneTime() &&
                        event.isCompleted()) {
                    continue;
                }
                return event;
            }
        }
        return null;
    }


    // Mark an event as completed
    public void completeEvent(String eventId) {
        if (events.containsKey(eventId)) {
            events.get(eventId).setCompleted(true);
        }
    }

    // Record a defeated enemy
    public void recordDefeatedEnemy(int enemyId) {
        defeatedEnemies.put(enemyId, true);
    }

    // Check if an enemy has been defeated
    public boolean isEnemyDefeated(int enemyId) {
        return defeatedEnemies.getOrDefault(enemyId, false);
    }

    // Get save data for serialization
    public Map<String, Object> getSaveData() {
        Map<String, Object> saveData = new HashMap<>();

        // Save completed events
        List<String> completedEvents = events.values().stream()
                .filter(MapEvent::isCompleted)
                .map(MapEvent::getId)
                .collect(Collectors.toList());
        saveData.put("completedEvents", completedEvents);

        // Save defeated enemies
        saveData.put("defeatedEnemies", new ArrayList<>(defeatedEnemies.keySet()));

        return saveData;
    }

    // Load save data
    public void loadSaveData(Map<String, Object> saveData) {
        if (saveData.containsKey("completedEvents")) {
            List<String> completedEvents = (List<String>) saveData.get("completedEvents");
            for (String eventId : completedEvents) {
                if (events.containsKey(eventId)) {
                    events.get(eventId).setCompleted(true);
                }
            }
        }

        if (saveData.containsKey("defeatedEnemies")) {
            List<Integer> enemyIds = (List<Integer>) saveData.get("defeatedEnemies");
            for (Integer enemyId : enemyIds) {
                defeatedEnemies.put(enemyId, true);
            }
        }
    }



    public boolean getBooleanProperty(MapProperties props, String key, boolean defaultValue) {
        if (!props.containsKey(key)) {
            return defaultValue;
        }

        Object value = props.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        return defaultValue;
    }

    // Utility method to safely get string property value
    public String getStringProperty(MapProperties props, String key, String defaultValue) {
        if (!props.containsKey(key)) {
            return defaultValue;
        }

        Object value = props.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            return String.valueOf(value);
        }
    }

    // Utility method to safely get integer property value
    public int getIntProperty(MapProperties props, String key, int defaultValue) {
        if (!props.containsKey(key)) {
            return defaultValue;
        }

        Object value = props.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public Map<String, MapEvent> getEvents() {
        return events;
    }

    public MapEvent getEvent(String eventId) {
        return events.get(eventId); // Assuming `events` is a Map<String, MapEvent>
    }

    public void setEvents(Map<String, MapEvent> events) {
        this.events = events;
    }
}