package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class PlantViewController {

    @FXML
    private SplitMenuButton urenMenu;

    @FXML
    private TextField tijdTextField;

    @FXML
    private TextField waterOutputField;

    @FXML
    private AnchorPane userPopout;

    @FXML
    private Label nameText;

    @FXML
    private Label emailText;

    @FXML
    private AnchorPane updateOverlay;

    @FXML
    private void showUserPopout() {
        MainApplication.fadeIn(userPopout, 200);
    }

    @FXML
    private void hideUserPopout() {
        MainApplication.fadeOut(userPopout, 100);
    }

    @FXML
    private void logout() throws IOException {
        // TODO
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "login-view.fxml");
    }

    @FXML
    private void openUsersettings() throws IOException {
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "userinfo-view.fxml");
    }

    @FXML
    public void initialize() {
        userPopout.setVisible(false);
        updateOverlay.setVisible(false);

        nameText.setText(MainApplication.getName());
        emailText.setText(MainApplication.getEmail());

        MainApplication.fadeIn(updateOverlay, 200);

        System.out.println("Plant View Controller initialized.");
    }

    private boolean isValidAmount(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            int integer = Integer.parseInt(str);
            return integer > 0 && integer <= 365;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    // Handler for the Start button
    @FXML
    private void handleStartButtonAction() {
        String enteredTime = tijdTextField.getText();
        String enteredWaterOutput = waterOutputField.getText();

        if (enteredTime.isEmpty()) {
            System.out.println("Please enter a time.");
        } else {
            if(isValidAmount(enteredTime)) {
                // TODO
                System.out.println("Entered time: " + enteredTime + " " + urenMenu.getText());
            } else {
                System.out.println("Please enter a valid time.");
            }
        }

        if (enteredWaterOutput.isEmpty()) {
            System.out.println("Please enter the water output.");
        } else {
            if(isValidAmount(enteredWaterOutput)) {
                // TODO
                System.out.println("Entered water output: " + enteredWaterOutput + " mL");
            } else {
                System.out.println("Please enter a valid water output.");
            }
        }
    }

    // Handlers for SplitMenuButton items
    @FXML
    private void handleUrenMenuItemAction() {
        urenMenu.setText("Uren");
    }

    @FXML
    private void handleDagenMenuItemAction() {
        urenMenu.setText("Dagen");
    }

    @FXML
    private void onUpdateDialogCancel() {
        MainApplication.fadeOut(updateOverlay, 100);
    }

    @FXML
    private void onUpdateDialogConfirm() {
        MainApplication.fadeOut(updateOverlay, 100);
    }
}
