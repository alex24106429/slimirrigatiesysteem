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
    private AnchorPane userPopout;

    @FXML
    private Label usernameText;

    @FXML
    private AnchorPane updateOverlay;

    // New setter method to update the username
    public void setUsername(String username) {
        usernameText.setText(username);
    }

    @FXML
    private void showUserPopout() {
        MainApplication.fadeIn(userPopout, 200);
    }

    @FXML
    private void hideUserPopout() {
        MainApplication.fadeOut(userPopout, 200);
    }

    @FXML
    private void logout() throws IOException {
        hideUserPopout();
        System.out.println("logout() called");
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "login-view.fxml");
    }

    @FXML
    private void openUsersettings() throws IOException {
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "usersettings-view.fxml");
    }

    @FXML
    public void initialize() {
        System.out.println("Plant View Controller initialized.");
        hideUserPopout();
        MainApplication.fadeIn(updateOverlay, 200);
    }

    // Handler for the Start button
    @FXML
    private void handleStartButtonAction() {
        System.out.println("Start button clicked!");
        String enteredText = tijdTextField.getText();
        if (enteredText.isEmpty()) {
            System.out.println("Please enter a time.");
        } else {
            // TODO
            System.out.println("Entered time: " + enteredText + " " + urenMenu.getText());
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
    private void onDialogConfirm() {
        MainApplication.fadeOut(updateOverlay, 200);
    }
}
