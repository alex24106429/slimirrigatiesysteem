package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import org.json.JSONObject;

import java.io.IOException;

import static org.teamhydro.slimirrigatiesysteem.ApiController.storeUserData;

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
        invalidLoginOverlay.setVisible(false);
    }

    @FXML
    private void handleLoginButtonAction() throws Exception {
        // Retrieve the username and password entered the TextFields
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            MainApplication.fadeIn(invalidLoginOverlay, 200);
            return;
        }

        // Call the API to authenticate
        String response = ApiController.login(email, password);
        JSONObject jsonResponse = new JSONObject(response);

        // Get the token value
        String token = jsonResponse.getString("token");

        // Get user data (name, email, address)
        JSONObject user = jsonResponse.getJSONObject("user");
        String name = user.getString("name");
        String address = user.getString("address");

        // Store token and user data
        storeUserData(token, name, email, address);

        // Handle the response (You can adjust this logic based on the API response)
        if (response.contains("Invalid credentials") || response.isEmpty() || response.startsWith("Error")) {
            MainApplication.fadeIn(invalidLoginOverlay, 200);
        } else {
            // Store email or other data from the response, if needed
            switchToPlantView();
        }
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
