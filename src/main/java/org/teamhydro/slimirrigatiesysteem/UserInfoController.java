package org.teamhydro.slimirrigatiesysteem;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.io.IOException;

public class UserInfoController {
    @FXML
    private TextField nameField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField emailField;

    @FXML
    public void initialize() {
        nameField.setText(MainApplication.getName());
        addressField.setText(MainApplication.getAddress());
        emailField.setText(MainApplication.getEmail());
        System.out.println("User info controller initialized.");
    }

    @FXML
    private void handleChangePasswordButton() throws IOException {
        MainApplication.switchView("passwordrecovery-view.fxml");
    }

    @FXML
    private void handleBackButton() throws IOException {
        MainApplication.switchView("plant-view.fxml");
    }

    @FXML
    private void handleSaveButton() throws IOException {
        String name = nameField.getText();
        String address = addressField.getText();
        String email = emailField.getText();

        try {
            MainApplication.updateUserInfo(name, address, email);
            MainApplication.switchView("plant-view.fxml");
        } catch (Exception e) {
            MainApplication.showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user info: " + e.getMessage());
        }
    }
}
