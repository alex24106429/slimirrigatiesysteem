package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;

public class PlantViewController {

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
        System.out.println("Plant View Controller initialized.");
    }

    // Handler for the Start button
    @FXML
    private void handleStartButtonAction() {
        System.out.println("Start button clicked!");
        String enteredText = tijdTextField.getText();
        if (enteredText.isEmpty()) {
            System.out.println("Please enter a time.");
        } else {
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
