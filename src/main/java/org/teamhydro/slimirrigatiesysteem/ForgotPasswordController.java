package org.teamhydro.slimirrigatiesysteem;

import com.sun.tools.javac.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Button recoveryButton;

    @FXML
    private Stage currentStage;

    // This method is called to initialize the controller
    @FXML
    public void initialize() {
        System.out.println("Forgot Password Controller initialized.");
    }

    private void switchToLoginPage() throws IOException {
        MainApplication.switchView((Stage) recoveryButton.getScene().getWindow(), "login-view.fxml");
    }

    @FXML
    private void handleBackButton() throws IOException {
        switchToLoginPage();
    }

    @FXML
    private void handleRecoveryButtonAction() throws IOException {
        String email = emailField.getText();

        // TODO
        System.out.println("Email: " + email);

        switchToLoginPage();
    }
}
