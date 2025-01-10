package org.teamhydro.slimirrigatiesysteem;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.InputStream;

public class PlantViewController {
    @FXML
    private AnchorPane userPopout;

    @FXML
    private Label nameText;

    @FXML
    private Label emailText;

    @FXML
    private AnchorPane updateOverlay;

    @FXML
    private AnchorPane deleteOverlay;

    @FXML
    private TextField plantSearchBar;

    @FXML
    private AnchorPane searchOverlay;

    @FXML
    private ListView<String> searchListView;

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

        // Check if plants array is null or empty
        if (MainApplication.plants == null || MainApplication.plants.length == 0) {
            MainApplication.fadeIn(searchOverlay, 200);
            return;
        }

        for (Plant plant : MainApplication.plants) {
            if (plant != null) {
                searchListView.getItems().add(plant.getName());
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
        for (Plant plant : MainApplication.plants) {
            if (plant != null && plant.getName().toLowerCase().contains(text)) {
                searchListView.getItems().add(plant.getName());
            }
        }
    }

    @FXML
    private void loadPlant(String name, String plantType, boolean useDays, int delay, int outputML, int minimumMoistureLevel, int currentMoistureLevel) {
        plantNameLabel.setText(name);

        moistureBar.setProgress((double) currentMoistureLevel / 1024);

        // Corrected image loading
        String imagePath = "/org/teamhydro/slimirrigatiesysteem/plantImages/" + plantType + ".png";
        try (InputStream inputStream = getClass().getResourceAsStream(imagePath)) {
            if (inputStream != null) {
                Image image = new Image(inputStream);
                plantImage.setImage(image);
            } else {
                System.err.println("Image not found: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        noPlantOverlay.setVisible(false);
    }

    @FXML
    private void loadPlantFromName(String plantName) {
        Plant chosenPlant = MainApplication.getPlantByName(plantName);

        loadPlant(plantName, chosenPlant.getPlantType(), chosenPlant.isUseDays(), chosenPlant.getDelay(), chosenPlant.getOutputML(), chosenPlant.getMinimumMoistureLevel(), chosenPlant.getCurrentMoistureLevel());
    }

    @FXML
    private void onSearchConfirm() {
        String chosenPlantName = (String) searchListView.getSelectionModel().getSelectedItem();

        if(chosenPlantName != null) {
            loadPlantFromName(chosenPlantName);

            hideSearchDialog();
        }
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
        // TODO: Logout logic
        MainApplication.switchView("login-view.fxml");
    }

    @FXML
    private void openUsersettings() throws IOException {
        MainApplication.switchView("userinfo-view.fxml");
    }

    @FXML
    public void unselectPlant() {
        noPlantOverlay.setVisible(true);
        plantNameLabel.setText("Selecteer een plant");

    }

    @FXML
    public void initialize() throws IOException {
        userPopout.setVisible(false);
        updateOverlay.setVisible(false);

        nameText.setText(MainApplication.getName());
        emailText.setText(MainApplication.getEmail());

        unselectPlant();

        // Delay the call to openCreatePlantView until after the UI is fully initialized
        Platform.runLater(() -> {
            if (MainApplication.plants.length == 0) {
                try {
                    openCreatePlantView();
                    MainApplication.showAlert(AlertType.INFORMATION, "Info", "Maak voor het gebruik eerst een plant aan.");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(MainApplication.plants.length == 1) {
                Plant firstPlant = MainApplication.plants[0];
                loadPlantFromName(firstPlant.getName());
                return;
            }
            showSearchDialog();
        });

        System.out.println("Plant View Controller initialized.");
    }

    @FXML
    public void openCreatePlantView() throws IOException {
        MainApplication.switchView("create-plant-view.fxml");
    }

    @FXML
    private void onUpdateDialogCancel() {
        MainApplication.fadeOut(updateOverlay, 100);
    }

    @FXML
    private void onUpdateDialogConfirm() {
        MainApplication.fadeOut(updateOverlay, 100);
    }

    @FXML
    private void openDeleteDialog() {
        if(plantNameLabel.getText().equals("Selecteer een plant")) return;
        MainApplication.fadeIn(deleteOverlay, 200);
    }

    @FXML
    private void onDeleteDialogCancel() {
        MainApplication.fadeOut(deleteOverlay, 100);
    }

    @FXML
    private void onDeleteDialogConfirm() throws IOException {
        PlantRepository.deletePlant(plantNameLabel.getText());
        PlantRepository.refreshPlants();
        initialize();
        MainApplication.fadeOut(deleteOverlay, 100);
    }

    @FXML
    private void editPlant() throws IOException {
        String currentPlantName = plantNameLabel.getText();
        if(currentPlantName.equals("Selecteer een plant")) return;
        FXMLLoader loader = MainApplication.switchView("create-plant-view.fxml");
        CreatePlantController controller = loader.getController();
        controller.loadPlantData(currentPlantName);
    }

    @FXML
    private void syncWithMicroBit() {
        // TODO: Sync with the Micro:Bit
        MainApplication.showAlert(AlertType.INFORMATION, "Sync with Micro:Bit", "TODO");
    }
}
