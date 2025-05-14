package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.game.isometric.model.dialog.*;
import ctu.game.isometric.model.game.GameState;

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
    // Flag to track if we're showing choices or dialogues
    private boolean showingChoices = false;

    private List<Choice> currentChoices = new ArrayList<>();
    private int selectedChoiceIndex = 0;
    boolean performAction = false;

    public DialogController(GameController gameController) {
        this.gameController = gameController;
        loadStoryData();
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





    // After
    public void selectChoice(int index) {
        if (!dialogActive || !showingChoices || currentChoices.isEmpty()) return;

        if (index >= 0 && index < currentChoices.size()) {
            Choice choice = currentChoices.get(index);

            // Reset flags before processing the choice
            showingChoices = false;

            // Check if player has required item
            if (choice.getRequired_item() != null) {
                if (gameController.getCharacter().hasItem(choice.getRequired_item())) {
                    performAction = true; // Only set to true when choice has required item

                    // Move to the next scene if specified
                    if (choice.getNext_scene() != null) {
                        startDialog(currentArcId, choice.getNext_scene());
                    }
                    else endDialog();
                } else {
                    // If player doesn't have the required item, redirect to scene_not_enough_item
                    startDialog(currentArcId, "scene_not_enough_item");
                }
            } else {
                // No item required, proceed normally
                if (choice.getNext_scene() != null && choice.getNext_scene().equals("scene_end")) {
                    performAction = false;
                    startDialog(currentArcId, choice.getNext_scene());
                }
                else {
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

    private Arc findArc(String arcId) {
        if (storyData == null || storyData.getArcs() == null) return null;

        for (Arc arc : storyData.getArcs()) {
            if (arc.getId().equals(arcId)) {
                return arc;
            }
        }
        return null;
    }

    private Scene findScene(Arc arc, String sceneId) {
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



    public void endDialog() {
        dialogActive = false;
        showingChoices = false;
        selectedChoiceIndex = 0;
        currentDialogIndex = 0;
        currentArcId = null;
        currentSceneId = null;

        gameController.setState(GameState.EXPLORING);

        // Execute the callback if it exists and performAction is true
        if (onDialogFinishedAction != null && performAction == true) {
            onDialogFinishedAction.run();
            onDialogFinishedAction = null;
            performAction = false;
        }

    }

    public void setOnDialogFinishedAction(Runnable action) {
        this.onDialogFinishedAction = action;
    }
}