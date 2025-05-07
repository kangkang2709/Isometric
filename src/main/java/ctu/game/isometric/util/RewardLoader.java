package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import ctu.game.isometric.model.game.Reward;

import java.util.HashMap;
import java.util.Map;

public class RewardLoader {
    private static Map<Integer, Reward> rewards = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue rewardsJson = jsonReader.parse(Gdx.files.internal("game/reward.json"));

            for (JsonValue rewardJson : rewardsJson) {
                Reward reward = new Reward();
                reward.setRewardID(rewardJson.getInt("rewardID"));
                reward.setItemID(ItemLoader.getItemById(rewardJson.getInt("itemID")));
                reward.setAmount(rewardJson.getInt("amount"));
                reward.setDescription(rewardJson.getString("description"));

                rewards.put(reward.getRewardID(), reward);
            }
            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("RewardLoader", "Error loading rewards", e);
        }
    }

    public static Reward getRewardById(int id) {
        if (!initialized) initialize();

        Reward template = rewards.get(id);
        if (template == null) {
            return createDefaultReward();
        }

        // Return a copy of the reward to prevent modifying the template
        Reward reward = new Reward();
        reward.setRewardID(template.getRewardID());
        reward.setItemID(template.getItemID());
        reward.setAmount(template.getAmount());
        reward.setDescription(template.getDescription());

        return reward;
    }

    private static Reward createDefaultReward() {
        Reward reward = new Reward();
        reward.setRewardID(0);
        reward.setItemID(ItemLoader.getItemById(1)); // Default to first item
        reward.setAmount(1);
        reward.setDescription("A basic reward.");
        return reward;
    }
}