package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    // This method is called to initialize the controller
    @FXML
    public void initialize() {
        System.out.println("Forgot Password Controller initialized.");
    }

    private void switchToLoginPage() throws IOException {
        MainApplication.switchView("login-view.fxml");
    }

    @FXML
    private void handleBackButton() throws IOException {
        switchToLoginPage();
    }

    @FXML
    private void handleRecoveryButtonAction() throws IOException {
        String email = emailField.getText();

        // TODO: Send recovery email
        System.out.println("Email: " + email);

        switchToLoginPage();
    }
}
