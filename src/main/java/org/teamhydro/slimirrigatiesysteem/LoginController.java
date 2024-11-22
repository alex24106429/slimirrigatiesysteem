package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private Button forgotPasswordButton;

    @FXML
    private Button loginButton;

    @FXML
    private Stage currentStage;

    @FXML
    private AnchorPane invalidLoginOverlay;

    @FXML
    public void initialize() {
        System.out.println("Login Controller initialized.");
        invalidLoginOverlay.setVisible(false);
    }

    @FXML
    private void handleLoginButtonAction() throws IOException {
        // Retrieve the username and password entered in the TextFields
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.equals("") || password.equals("")) {
            invalidLoginOverlay.setVisible(true);
            return;
        }

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        // After a successful login, switch to the plant view and set the username
        switchToPlantView(username);
    }

    @FXML
    private void handleForgotPasswordButton() throws IOException {
        System.out.println("Forgot password clicked");
        switchToForgotPasswordView();
    }

    @FXML
    private void onDialogConfirm() throws IOException {
        invalidLoginOverlay.setVisible(false);
    }

    // Switch to the plant view and set the username
    private void switchToPlantView(String username) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("plant-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 400);

        // Get the current stage and switch the scene
        currentStage = (Stage) loginButton.getScene().getWindow();
        currentStage.setScene(scene);
        currentStage.setTitle("Plant - Slim Irrigatie Systeem");

        // Get the PlantViewController and set the username
        PlantViewController plantViewController = fxmlLoader.getController();
        plantViewController.setUsername(username);

        currentStage.show();
    }

    private void switchToForgotPasswordView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("passwordrecovery-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 400);

        currentStage = (Stage) loginButton.getScene().getWindow();
        currentStage.setScene(scene);
        currentStage.setTitle("Wachtwoord vergeten - Slim Irrigatie Systeem");
        currentStage.show();
    }
}
