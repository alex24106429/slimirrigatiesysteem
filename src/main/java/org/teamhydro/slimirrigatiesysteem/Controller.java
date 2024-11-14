package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;

public class Controller {

    @FXML
    private Button startButton;

    @FXML
    private SplitMenuButton urenMenu;

    @FXML
    private TextField tijdTextField;

    @FXML
    private MenuItem urenMenuItem;

    @FXML
    private MenuItem dagenMenuItem;

    @FXML
    public void initialize() {
        // Initialize any necessary setup here
        System.out.println("Controller initialized.");
    }

    // Handler for the Start button
    @FXML
    private void handleStartButtonAction() {
        System.out.println("Start button clicked!");
        String enteredText = tijdTextField.getText();
        if (enteredText.isEmpty()) {
            System.out.println("Please enter a time.");
        } else {
            System.out.println("Entered time: " + enteredText);
        }
    }

    // Handlers for SplitMenuButton items
    @FXML
    private void handleUrenMenuItemAction() {
        tijdTextField.setPromptText("Enter hours");
        System.out.println("Uren selected.");
    }

    @FXML
    private void handleDagenMenuItemAction() {
        tijdTextField.setPromptText("Enter days");
        System.out.println("Dagen selected.");
    }
}
