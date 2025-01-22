package org.teamhydro.slimirrigatiesysteem;

import javafx.application.Platform;
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
    private Label delayProgressText;

    @FXML
    private Rectangle noPlantOverlay;

    @FXML
    private ImageView plantImage;

    private volatile boolean isRunning = true;
    private Thread refreshThread;

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
    private void loadPlant(String name, String plantType, boolean useDays, int delay, int outputML, int minimumMoistureLevel, double currentMoistureLevel) {
        plantNameLabel.setText(name);

        moistureBar.setProgress(currentMoistureLevel / 1024);

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

        // Stop existing refresh thread if it exists
        stopRefreshThread();

        // Create new refresh thread
        refreshThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(5000);
                    Plant plant = MainApplication.getPlantByName(name);
                    if (plant == null) {
                        isRunning = false;
                        continue;
                    }
                    
                    boolean success = plant.refreshFromArduino();
                    if (success) {
                        Platform.runLater(() -> {
                            moistureBar.setProgress(plant.getCurrentMoistureLevel() / 1024);
                            updateDelayText(plant);
                        });
                    } else {
                        // Handle communication failure
                        Platform.runLater(() -> {
                            MainApplication.showAlert(AlertType.WARNING, 
                                "Communication Error", 
                                "Failed to update plant data from Arduino");
                        });
                        Thread.sleep(30000); // Wait longer before retrying
                    }
                } catch (InterruptedException e) {
                    isRunning = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    isRunning = false;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void stopRefreshThread() {
        isRunning = false;
        if (refreshThread != null) {
            refreshThread.interrupt();
            try {
                refreshThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Add this to handle cleanup
    public void cleanup() {
        stopRefreshThread();
    }

    @FXML
    private void loadPlantFromName(String plantName) {
        Plant chosenPlant = MainApplication.getPlantByName(plantName);

        assert chosenPlant != null;
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

    private void updateDelayText(Plant plant) {
        // Format delay time
        int currentDelay = plant.getCurrentDelay();
        
        String formattedTime;
        if (plant.isUseDays()) {
            int days = currentDelay / (24 * 3600);
            formattedTime = days + "d";
        } else {
            int hours = currentDelay / 3600;
            int minutes = (currentDelay % 3600) / 60;
            int seconds = currentDelay % 60;
            formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        
        delayProgressText.setText(formattedTime + "/" + 
            (plant.isUseDays() ? plant.getDelay() + "d" : "0h"));
    }
}
