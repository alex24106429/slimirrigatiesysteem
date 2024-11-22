package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

public class PlantViewController {

    @FXML
    private SplitMenuButton urenMenu;

    @FXML
    private TextField tijdTextField;

    @FXML
    private AnchorPane userPopout;

    @FXML
    private Label usernameText;

    // New setter method to update the username
    public void setUsername(String username) {
        usernameText.setText(username);
    }

    @FXML
    private void showUserPopout() {
        userPopout.setVisible(true);
    }

    @FXML
    private void hideUserPopout() {
        userPopout.setVisible(false);
    }

    @FXML
    private void logout() {
        hideUserPopout();
        System.out.println("logout() called");
    }

    @FXML
    public void initialize() {
        System.out.println("Plant View Controller initialized.");
        hideUserPopout();
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
}
