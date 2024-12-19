package org.teamhydro.slimirrigatiesysteem;

import com.sun.tools.javac.Main;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class PlantViewController {

    @FXML
    private SplitMenuButton urenMenu;

    @FXML
    private TextField tijdTextField;

    @FXML
    private TextField waterOutputField;

    @FXML
    private AnchorPane userPopout;

    @FXML
    private Label nameText;

    @FXML
    private Label emailText;

    @FXML
    private AnchorPane updateOverlay;

    @FXML
    private TextField plantSearchBar;

    @FXML
    private AnchorPane searchOverlay;

    @FXML
    private ListView searchListView;

    @FXML
    private Label plantNameLabel;

    @FXML
    private ProgressBar moistureBar;

    @FXML
    private Rectangle noPlantOverlay;

    @FXML
    private ImageView plantImage;

    @FXML
    private void showSearchDialog() {
        plantSearchBar.setText("");
        searchListView.getItems().clear();

        // Limit results to 10
        if (MainApplication.plants.length == 0) {
            MainApplication.fadeIn(searchOverlay, 200);
            return;
        }

        for (int i = 0; i < 10; i++) {
            if (MainApplication.plants[i] != null) {
                searchListView.getItems().add(MainApplication.plants[i].getName());
            }
        }

        MainApplication.fadeIn(searchOverlay, 200);
    }

    @FXML
    private void hideSearchDialog() {
        MainApplication.fadeOut(searchOverlay, 100);
    }

    @FXML
    private void onPlantSearchKeypress() {
        searchListView.getItems().clear();

        String text = plantSearchBar.getText().toLowerCase();

        // Filter plants based on search text and add matching plants to ListView
        int count = 0;
        for (int i = 0; i < MainApplication.plants.length; i++) {
            if (MainApplication.plants[i] != null && MainApplication.plants[i].getName().toLowerCase().contains(text)) {
                searchListView.getItems().add(MainApplication.plants[i].getName());
                count++;
            }
            if (count == 10) break;
        }
    }

    @FXML
    private void loadPlant(String name, String plantType, boolean useDays, int delay, int outputML, int minimumMoistureLevel, int currentMoistureLevel) {
        plantNameLabel.setText(name);

        if(useDays) {
            handleDagenMenuItemAction();
        } else {
            handleUrenMenuItemAction();
        }

        tijdTextField.setText(String.valueOf(delay));
        waterOutputField.setText(String.valueOf(outputML));

        moistureBar.setProgress((double) currentMoistureLevel / 1024);

//        plantImage.setImage(new Image("file:plantImages/" + plantType + ".png"));

        noPlantOverlay.setVisible(false);
    }

    @FXML
    private void onSearchConfirm() {
        String chosenPlantName = (String) searchListView.getSelectionModel().getSelectedItem();

        Plant chosenPlant = MainApplication.getPlantByName(chosenPlantName);

        loadPlant(chosenPlantName, chosenPlant.getPlantType(), chosenPlant.isUseDays(), chosenPlant.getDelay(), chosenPlant.getOutputML(), chosenPlant.getMinimumMoistureLevel(), chosenPlant.getCurrentMoistureLevel());

        hideSearchDialog();
    }

    @FXML
    private void showUserPopout() {
        MainApplication.fadeIn(userPopout, 200);
    }

    @FXML
    private void hideUserPopout() {
        MainApplication.fadeOut(userPopout, 100);
    }

    @FXML
    private void logout() throws IOException {
        // TODO
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "login-view.fxml");
    }

    @FXML
    private void openUsersettings() throws IOException {
        MainApplication.switchView((Stage) urenMenu.getScene().getWindow(), "userinfo-view.fxml");
    }

    @FXML
    public void initialize() {
        noPlantOverlay.setVisible(true);
        userPopout.setVisible(false);
        updateOverlay.setVisible(false);

        nameText.setText(MainApplication.getName());
        emailText.setText(MainApplication.getEmail());

//        MainApplication.fadeIn(updateOverlay, 200);

        plantNameLabel.setText("Selecteer een plant");

        System.out.println("Plant View Controller initialized.");
    }

    private boolean isValidAmount(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            int integer = Integer.parseInt(str);
            return integer > 0 && integer <= 365;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    // Handler for the Start button
    @FXML
    private void handleStartButtonAction() {
        String enteredTime = tijdTextField.getText();
        String enteredWaterOutput = waterOutputField.getText();

        if (enteredTime.isEmpty()) {
            System.out.println("Please enter a time.");
        } else {
            if(isValidAmount(enteredTime)) {
                // TODO
                System.out.println("Entered time: " + enteredTime + " " + urenMenu.getText());
            } else {
                System.out.println("Please enter a valid time.");
            }
        }

        if (enteredWaterOutput.isEmpty()) {
            System.out.println("Please enter the water output.");
        } else {
            if(isValidAmount(enteredWaterOutput)) {
                // TODO
                System.out.println("Entered water output: " + enteredWaterOutput + " mL");
            } else {
                System.out.println("Please enter a valid water output.");
            }
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

    @FXML
    private void onUpdateDialogCancel() {
        MainApplication.fadeOut(updateOverlay, 100);
    }

    @FXML
    private void onUpdateDialogConfirm() {
        MainApplication.fadeOut(updateOverlay, 100);
    }
}
