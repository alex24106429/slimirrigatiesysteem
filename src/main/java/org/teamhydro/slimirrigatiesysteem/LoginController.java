package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

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
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.equals("") || password.equals("")) {
            MainApplication.fadeIn(invalidLoginOverlay, 200);
            return;
        }

        // TODO: Authenticate with the server
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);

        MainApplication.setEmail(email);

        switchToPlantView();
    }

    @FXML
    private void handleForgotPasswordButton() throws IOException {
        System.out.println("Forgot password clicked");
        switchToForgotPasswordView();
    }

    @FXML
    private void onDialogConfirm() {
        MainApplication.fadeOut(invalidLoginOverlay, 100);
    }

    // Switch to the plant view and set the username
    private void switchToPlantView() throws IOException {
        MainApplication.switchView("plant-view.fxml");
    }

    private void switchToForgotPasswordView() throws IOException {
        MainApplication.switchView("passwordrecovery-view.fxml");
    }
}
