package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private Button loginButton;

    private Stage currentStage;

    // This method is called to initialize the controller
    @FXML
    public void initialize() {
        System.out.println("Login Controller initialized.");
    }

    // This method is called when the login button is clicked
    @FXML
    private void handleLoginButtonAction() throws IOException {
        // Retrieve the username and password entered in the TextFields
        String username = usernameField.getText();
        String password = passwordField.getText();

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        // Switch to the plant view after login
        switchToPlantView();
    }

    // Method to switch to the plant view (plant-view.fxml)
    private void switchToPlantView() throws IOException {
        // Load the plant view FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("plant-view.fxml"));
        Scene plantScene = new Scene(fxmlLoader.load(), 640, 400);

        // Get the current stage and switch the scene
        currentStage = (Stage) loginButton.getScene().getWindow();
        currentStage.setScene(plantScene);
        currentStage.setTitle("Plant - Slim Irrigatie Systeem");
        currentStage.show();
    }
}
