package org.teamhydro.slimirrigatiesysteem;

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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 400);

        currentStage = (Stage) recoveryButton.getScene().getWindow();
        currentStage.setScene(scene);
        currentStage.setTitle("Inloggen - Slim Irrigatie Systeem");
        currentStage.show();
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
