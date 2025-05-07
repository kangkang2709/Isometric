package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import ctu.game.isometric.model.game.Items;

import java.util.HashMap;
import java.util.Map;

public class ItemLoader {
    private static Map<Integer, Items> items = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue itemsJson = jsonReader.parse(Gdx.files.internal("game/items.json"));

            for (JsonValue itemJson : itemsJson) {
                Items item = new Items();
                item.setItemID(itemJson.getInt("itemID"));
                item.setItemName(itemJson.getString("itemName"));
                item.setItemDescription(itemJson.getString("itemDescription"));
                item.setTexturePath(itemJson.getString("texturePath"));

                items.put(item.getItemID(), item);
            }
            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("ItemsLoader", "Error loading items", e);
        }
    }

    public static Items getItemById(int id) {
        if (!initialized) initialize();

        Items template = items.get(id);
        if (template == null) {
            return createDefaultItem();
        }

        // Return a copy of the item to prevent modifying the template
        Items item = new Items();
        item.setItemID(template.getItemID());
        item.setItemName(template.getItemName());
        item.setItemDescription(template.getItemDescription());
        item.setTexturePath(template.getTexturePath());

        return item;
    }

    private static Items createDefaultItem() {
        Items item = new Items();
        item.setItemID(0);
        item.setItemName("Unknown Item");
        item.setItemDescription("A mysterious item.");
        item.setTexturePath("items/default.png");
        return item;
    }
}