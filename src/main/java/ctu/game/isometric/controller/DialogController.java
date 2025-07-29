package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.game.isometric.model.dialog.*;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.util.ItemLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DialogController {
    private StoryData storyData;
    private GameController gameController;

    private boolean dialogActive = false;
    private String currentArcId;
    private String currentSceneId;
    private int currentDialogIndex = 0;
    private Runnable onDialogFinishedAction;
    private Runnable onCanncelFinishedAction;
    // Flag to track if we're showing choices or dialogues
    private boolean showingChoices = false;

    private List<Choice> currentChoices = new ArrayList<>();
    private int selectedChoiceIndex = 0;
    boolean performAction = false;

    public DialogController(GameController gameController) {
        this.gameController = gameController;
        loadStoryData();
    }

    public boolean shouldRenderBackground() {
        return currentArcId != null && currentArcId.contains("_background");
    }

    private void loadStoryData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            storyData = mapper.readValue(Gdx.files.internal("story/arc1.json").reader(), StoryData.class);

            Gdx.app.log("Dialog", "Story data loaded successfully");
        } catch (IOException e) {
            Gdx.app.error("Dialog", "Failed to load story data", e);
        }
    }

    public void startDialog(String arcId, String sceneId) {
        if (storyData == null) return;

        Arc arc = findArc(arcId);
        if (arc == null) return;

        Scene scene = findScene(arc, sceneId);
        if (scene == null) return;

        currentArcId = arcId;
        currentSceneId = sceneId;
        currentDialogIndex = 0;
        dialogActive = true;
        showingChoices = false;  // Start in dialogue mode, not choice mode

        // Prepare choices but don't show them yet
        if (scene.getChoices() != null && !scene.getChoices().isEmpty()) {
            currentChoices = scene.getChoices();
            selectedChoiceIndex = 0;
        }

        Gdx.app.log("Dialog", "Started dialog: Arc=" + arcId + ", Scene=" + sceneId);
    }

    public boolean nextDialog() {
        if (!dialogActive) return false;

        // If we're already showing choices, do nothing when nextDialog is called
        if (showingChoices) {
            return true;
        }

        Arc arc = findArc(currentArcId);
        Scene scene = findScene(arc, currentSceneId);

        if (scene == null || scene.getDialogues() == null) {
            endDialog(); // Use endDialog to reset the state properly
            return false;
        }

        // Move to next dialog
        currentDialogIndex++;

        // If we've reached the end of the dialog
        if (currentDialogIndex >= scene.getDialogues().size()) {
            // If there are choices, show them
            if (scene.getChoices() != null && !scene.getChoices().isEmpty()) {
                showingChoices = true;  // Now switch to choice mode
                currentChoices = scene.getChoices(); // Ensure choices are set
                selectedChoiceIndex = 0; // Reset selected choice
                return true;
            } else {
                System.out.println("End of dialog, no choices available.");

                endDialog(); // Use endDialog to reset the state properly
                return false;
            }
        }

        return true;
    }


    public void showSimpleMessage(String message) {
        // Create a temporary dialog with the message
        Dialog tempDialog = new Dialog();
        tempDialog.setText(message);

        // Find or create the "system_messages" arc
        Arc arc = findArc("system_messages");
        if (arc == null) {
            arc = new Arc();
            arc.setId("system_messages");
            if (storyData.getArcs() == null) {
                storyData.setArcs(new ArrayList<>());
            }
            storyData.getArcs().add(arc);
        }

        // Find or create the "notification" scene
        Scene scene = findScene(arc, "notification");
        if (scene == null) {
            scene = new Scene();
            scene.setId("notification");
            if (arc.getScenes() == null) {
                arc.setScenes(new ArrayList<>());
            }
            arc.getScenes().add(scene);
        }

        // Update dialog
        List<Dialog> dialogues = new ArrayList<>();
        dialogues.add(tempDialog);
        scene.setDialogues(dialogues);

        // Set action to handle when the simple message is dismissed
        setOnDialogFinishedAction(() -> {
            Gdx.app.log("Dialog", "Simple message dismissed");
        });

        // Show the notification
        startDialog("system_messages", "notification");
        performAction = true;
    }


    public void showSimpleMessage(List<String> message) {
        // Create a temporary dialog with the message


        // Find or create the "system_messages" arc
        Arc arc = findArc("system_messages");
        if (arc == null) {
            arc = new Arc();
            arc.setId("system_messages");
            if (storyData.getArcs() == null) {
                storyData.setArcs(new ArrayList<>());
            }
            storyData.getArcs().add(arc);
        }

        // Find or create the "notification" scene
        Scene scene = findScene(arc, "notification");
        if (scene == null) {
            scene = new Scene();
            scene.setId("notification");
            if (arc.getScenes() == null) {
                arc.setScenes(new ArrayList<>());
            }
            arc.getScenes().add(scene);
        }

        // Update dialog


        List<Dialog> dialogues = new ArrayList<>();
        for (String msg : message) {
            Dialog tempDialog = new Dialog();
            tempDialog.setText(msg);
            dialogues.add(tempDialog);
        }

        scene.setDialogues(dialogues);

        // Set action to handle when the simple message is dismissed
        setOnDialogFinishedAction(() -> {
            Gdx.app.log("Dialog", "Simple message dismissed");
        });

        // Show the notification
        startDialog("system_messages", "notification");
        performAction = true;
    }


    public void showMessageWithChoices(String message, String actionChoiceText, String cancelChoiceText, Runnable onActionCompleted, Runnable onCancelCompleted) {
        // Create a temporary dialog with the message
        Dialog tempDialog = new Dialog();
        tempDialog.setText(message);

        // Find or create the "system_messages" arc
        Arc arc = findArc("system_messages");
        if (arc == null) {
            arc = new Arc();
            arc.setId("system_messages");
            if (storyData.getArcs() == null) {
                storyData.setArcs(new ArrayList<>());
            }
            storyData.getArcs().add(arc);
        }


        Scene scene2 = findScene(arc, "scene_end");
        if (scene2 == null) {
            scene2 = new Scene();
            scene2.setId("scene_end");
            if (arc.getScenes() == null) {
                arc.setScenes(new ArrayList<>());
            }
            arc.getScenes().add(scene2);
        }

        // Find or create the "choice_dialog" scene
        Scene scene = findScene(arc, "choice_dialog");
        if (scene == null) {
            scene = new Scene();
            scene.setId("choice_dialog");
            if (arc.getScenes() == null) {
                arc.setScenes(new ArrayList<>());
            }
            arc.getScenes().add(scene);
        }

        // Update dialog
        List<Dialog> dialogues = new ArrayList<>();
        dialogues.add(tempDialog);
        scene.setDialogues(dialogues);


        Dialog tempDialog2 = new Dialog();
        tempDialog2.setText("");
        List<Dialog> dialogues_end = new ArrayList<>();
        dialogues_end.add(tempDialog2);
        scene2.setDialogues(dialogues_end);


        // Create the two choices
        List<Choice> choices = new ArrayList<>();

        // Choice 1: Action with required item
        Choice actionChoice = new Choice();
        actionChoice.setText(actionChoiceText);
        actionChoice.setRequired_item(null); // No required item for this choice
        actionChoice.setNext_scene("scene_end");
        choices.add(actionChoice);

        // Choice 2: Cancel option
        Choice cancelChoice = new Choice();
        cancelChoice.setText(cancelChoiceText);
        cancelChoice.setNext_scene("scene_end"); // This will make performAction false
        choices.add(cancelChoice);

        // Set the choices for the scene
        scene.setChoices(choices);

        // Set action to perform when dialog is finished (only runs if the required item choice is selected)
        setOnDialogFinishedAction(onActionCompleted);
        setOnCanncelFinishedAction(onCancelCompleted);

        // Start the dialog
        startDialog("system_messages", "choice_dialog");
    }


    // After
    public void selectChoice(int index) {
        if (!dialogActive || !showingChoices || currentChoices.isEmpty()) {
            return;
        }

        if (index >= 0 && index < currentChoices.size()) {
            Choice choice = currentChoices.get(index);

            // Reset flags before processing the choice
            showingChoices = false;
            String itemName = choice.getReward_item();

            // Check if player has required item
            if (choice.getRequired_item() != null) {
                if (gameController != null && gameController.getCharacter() != null &&
                        gameController.getCharacter().hasItem(choice.getRequired_item())) {
                    performAction = true; // Only set to true when choice has required item

                    if (itemName != null) {

                        if (itemName.equalsIgnoreCase("Ruby Omega")) {
                            gameController.changeSaveMap("dungeon2");
                            gameController.getCharacter().setPosition(1, 20);
                        } else if (itemName.equalsIgnoreCase("Saphire Alpha")) {
                            gameController.changeSaveMap("frozen");
                            gameController.getCharacter().setPosition(7, 7);
                        } else if (itemName.equalsIgnoreCase("Emeral Delta")) {
                            gameController.changeSaveMap("forest");
                        } else
                            gameController.getCharacter().addItem(ItemLoader.getItemByName(itemName), 1);

                    }

                    // Move to the next scene if specified
                    if (choice.getNext_scene() != null) {
                        startDialog(currentArcId, choice.getNext_scene());
                    } else endDialog();
                } else {
                    // If player doesn't have the required item, redirect to scene_not_enough_item
                    startDialog(currentArcId, "scene_not_enough_item");
                }
            }
            else if (choice.getText().contains("[YES]")) {
                performAction = true;
                isCancelAction = false;

                if (itemName != null && !itemName.isEmpty()) {
                    Items item = ItemLoader.getItemByName(itemName);
                    if (item != null) {
                        if (item.getItemEffect().equals("N/A") && gameController.getCharacter().getItems().get(itemName) != null) {
                        } else {
                            gameController.getCharacter().addItem(item, 1);
                        }
                    }
                }
                if (choice.getNext_scene() != null) {
                    startDialog(currentArcId, choice.getNext_scene());
                } else {
                    endDialog();
                }
            } else if (choice.getText().contains("[NO]")) {
                performAction = false;
                isCancelAction = true;

                if (itemName != null && !itemName.isEmpty()) {
                    Items item = ItemLoader.getItemByName(itemName);
                    if (item != null) {
                        if (item.getItemEffect().equals("N/A") && gameController.getCharacter().getItems().get(itemName) != null) {
                        } else {
                            gameController.getCharacter().addItem(item, 1);
                        }
                    }
                }
                if (choice.getNext_scene() != null) {
                    startDialog(currentArcId, choice.getNext_scene());
                } else {
                    endDialog();
                }
            } else {
                if (itemName != null && !itemName.isEmpty()) {
                    Items item = ItemLoader.getItemByName(itemName);
                    if (item != null) {
                        if (item.getItemEffect().equals("N/A") && gameController.getCharacter().getItems().get(itemName) != null) {
                        } else {
                            gameController.getCharacter().addItem(item, 1);
                        }
                    }
                }
                // No item required, proceed normally
                if (choice.getNext_scene() != null && !choice.getNext_scene().isEmpty()) {
                    if (choice.getNext_scene().equals("scene_end")) {
                        performAction = false;
                        isCancelAction = false;
                    }
                    startDialog(currentArcId, choice.getNext_scene());
                } else {
                    endDialog();
                }
            }
        }
    }

    public void selectNextChoice() {
        if (showingChoices && !currentChoices.isEmpty()) {
            selectedChoiceIndex = (selectedChoiceIndex + 1) % currentChoices.size();
        }
    }

    public void selectPreviousChoice() {
        if (showingChoices && !currentChoices.isEmpty()) {
            selectedChoiceIndex = (selectedChoiceIndex - 1 + currentChoices.size()) % currentChoices.size();
        }
    }

    public Arc findArc(String arcId) {
        if (storyData == null || storyData.getArcs() == null) return null;

        for (Arc arc : storyData.getArcs()) {
            if (arc.getId().equals(arcId)) {
                return arc;
            }
        }
        return null;
    }

    public Scene findScene(Arc arc, String sceneId) {
        if (arc == null || arc.getScenes() == null) return null;

        for (Scene scene : arc.getScenes()) {
            if (scene.getId().equals(sceneId)) {
                return scene;
            }
        }
        return null;
    }

    // Getters for current dialog state
    public Dialog getCurrentDialog() {
        if (!dialogActive || showingChoices) return null;

        Arc arc = findArc(currentArcId);
        Scene scene = findScene(arc, currentSceneId);

        if (scene == null || scene.getDialogues() == null ||
                currentDialogIndex >= scene.getDialogues().size()) {
            return null;
        }

        return scene.getDialogues().get(currentDialogIndex);
    }

    public List<Choice> getCurrentChoices() {
        return showingChoices ? currentChoices : new ArrayList<>();
    }

    public int getSelectedChoiceIndex() {
        return selectedChoiceIndex;
    }

    public boolean isDialogActive() {
        return dialogActive;
    }

    public boolean hasChoices() {
        return showingChoices && !currentChoices.isEmpty();
    }

    private boolean isCancelAction = false;

    public void endDialog() {
        dialogActive = false;
        showingChoices = false;
        selectedChoiceIndex = 0;
        currentDialogIndex = 0;
        currentArcId = null;
        currentSceneId = null;

        gameController.changeBehaviorStateNPC();
        gameController.setState(GameState.EXPLORING);

        // Execute the callback if it exists and performAction is true
        if (onDialogFinishedAction != null && performAction) {
            onCanncelFinishedAction = null;
            onDialogFinishedAction.run();
            onDialogFinishedAction = null;
            performAction = false;
            isCancelAction = false; // Reset cancel action flag
        } else if (onCanncelFinishedAction != null && isCancelAction) {
            onDialogFinishedAction = null;
            onCanncelFinishedAction.run();
            onCanncelFinishedAction = null;
            performAction = false;
            isCancelAction = false;

        }

    }

    public void setPerformAction(boolean performAction) {
        this.performAction = performAction;
    }

    public String getCurrentSceneId() {
        return currentSceneId;
    }

    public void setCurrentSceneId(String currentSceneId) {
        this.currentSceneId = currentSceneId;
    }

    public String getCurrentArcId() {
        return currentArcId;
    }

    public void setCurrentArcId(String currentArcId) {
        this.currentArcId = currentArcId;
    }

    public void setOnDialogFinishedAction(Runnable action) {
        this.onDialogFinishedAction = action;
    }

    public Runnable getOnCanncelFinishedAction() {
        return onCanncelFinishedAction;
    }

    public void setOnCanncelFinishedAction(Runnable onCanncelFinishedAction) {
        this.onCanncelFinishedAction = onCanncelFinishedAction;
    }
}