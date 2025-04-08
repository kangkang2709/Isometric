package ctu.game.isometric.controller;

import ctu.game.isometric.model.dialog.DialogModel;

public class DialogController {
    private DialogModel dialogModel;

    public DialogController(DialogModel dialogModel) {
        this.dialogModel = dialogModel;
    }

    public void showDialog(String text) {
        dialogModel.setText(text);
    }

    public void hideDialog() {
        dialogModel.hide();
    }
}