package org.teamhydro.slimirrigatiesysteem;

import com.sun.tools.javac.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
        MainApplication.switchView((Stage) nameField.getScene().getWindow(), "passwordrecovery-view.fxml");
    }

    @FXML
    private void handleBackButton() throws IOException {
        MainApplication.switchView((Stage) nameField.getScene().getWindow(), "plant-view.fxml");
    }

    @FXML
    private void handleSaveButton() throws IOException {
        String name = nameField.getText();
        String address = addressField.getText();
        String email = emailField.getText();

        // TODO
        System.out.println(name);
        System.out.println(address);
        System.out.println(email);

        MainApplication.setName(name);
        MainApplication.setAddress(address);
        MainApplication.setEmail(email);

        MainApplication.switchView((Stage) nameField.getScene().getWindow(), "plant-view.fxml");
    }
}
