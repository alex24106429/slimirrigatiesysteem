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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.Scene;

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
    private Label moisturePercentage;

    @FXML
    private Label waterOutputLabel;

    @FXML
    private Label minMoistureLabel;

    @FXML
    private Label delayProgressText;

    @FXML
    private Rectangle noPlantOverlay;

    @FXML
    private ImageView plantImage;

    @FXML
    private ImageView loadingIcon;

    private Timeline refreshTimeline;
    private Timeline loadingAnimation;
    private Timeline countdownTimeline;
    private int loadingRotations = 0;
    private Plant currentPlant;
    private int currentCountdownValue = 0;
    private boolean alreadyRefreshing = false;

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
        System.out.println((int)currentMoistureLevel);
        System.out.println(minimumMoistureLevel);

        // Convert moisture level (0-1024) to percentage (0-100)
        double percentage = (currentMoistureLevel / 1024.0);
        moistureBar.setProgress(percentage); 
        moisturePercentage.setText((int)(percentage * 100) + "%");
        minMoistureLabel.setText((int)((minimumMoistureLevel / 1024.0) * 100) + "%");
        
        // Set water output label
        waterOutputLabel.setText(outputML + "mL");

        // Load plant image
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
        
        // Stop existing refresh timeline if it exists
        stopRefreshTimeline();
        
        // Store current plant reference
        currentPlant = MainApplication.getPlantByName(name);

        boolean success = refreshPlantData();
        if (!success) {
            MainApplication.showAlert(AlertType.ERROR, "Error", "Failed to refresh plant data");
        }

        // Create new refresh timeline
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(10), _ -> {
                refreshPlantData();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void startLoadingAnimation() {
        if (loadingAnimation != null) {
            loadingRotations = 0;
            return; // Animation already running
        }

        loadingIcon.setVisible(true);
        loadingRotations = 0;
        
        loadingAnimation = new Timeline(
            new KeyFrame(Duration.millis(50), _ -> {
                loadingIcon.setRotate((loadingIcon.getRotate() + 10) % 360);
                if (loadingIcon.getRotate() % 360 == 0) {
                    loadingRotations++;
                }
            })
        );
        loadingAnimation.setCycleCount(Timeline.INDEFINITE);
        loadingAnimation.play();
    }

    private void stopLoadingAnimation() {
        if (loadingAnimation != null) {
            Thread stopAnimationThread = new Thread(() -> {
                int attempts = 0;
                while (attempts < 10 && loadingRotations < 2) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    attempts++;
                }
                
                Platform.runLater(() -> {
                    loadingAnimation.stop();
                    loadingAnimation = null;
                    loadingIcon.setVisible(false); 
                    loadingIcon.setRotate(0);
                });
            });
            stopAnimationThread.setDaemon(true);
            stopAnimationThread.start();
        }
    }

    private boolean refreshPlantData() {
        if (currentPlant == null) {
            stopRefreshTimeline();
            return false;
        }

        if (alreadyRefreshing) {
            return true;
        }

        alreadyRefreshing = true;

        // Start loading animation on UI thread
        Platform.runLater(() -> startLoadingAnimation());

        // Create a new thread for Arduino communication
        Thread refreshThread = new Thread(() -> {
            try {
                Plant.RefreshResult result = currentPlant.refreshFromArduino();

                if (result.shouldReupload()) {
                    boolean success = ArduinoController.sendPlantConfig(currentPlant);
                    if (!success) {
                        Platform.runLater(() -> {
                            stopLoadingAnimation();
                            MainApplication.showAlert(AlertType.ERROR, "Error", "Failed to update Arduino configuration");
                        });
                        alreadyRefreshing = false;
                        return;
                    }
                }

                if (result.isSuccess()) {
                    // Update UI elements on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        // Get the current moisture level
                        double currentMoistureLevel = currentPlant.getCurrentMoistureLevel();
                        // Convert moisture level (0-1024) to percentage (0-100)
                        double percentage = (currentMoistureLevel / 1024.0);
                        moistureBar.setProgress(percentage);
                        moisturePercentage.setText((int)(percentage * 100) + "%");

                        // Update the delay progress text
                        updateDelayText(currentPlant);

                        // Stop loading animation
                        stopLoadingAnimation();
                    });
                } else {
                    Platform.runLater(() -> {
                        stopLoadingAnimation();
                        MainApplication.showAlert(AlertType.ERROR, "Error", "Failed to refresh plant data");
                    });
                }

                if (result.getNewPlantName() != null) {
                    Platform.runLater(() -> loadPlantFromName(result.getNewPlantName()));
                }
            } catch (Exception e) {
                System.out.println("Error in refresh: " + e.getMessage());
                Platform.runLater(() -> {
                    stopLoadingAnimation();
                    MainApplication.showAlert(AlertType.ERROR, "Error", "An error occurred while refreshing plant data");
                    alreadyRefreshing = false;
                });
            }
        });

        // Start the background thread
        refreshThread.setDaemon(true);
        refreshThread.start();

        alreadyRefreshing = false;
        return true;
    }

    private void startCountdown(int startValue) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        currentCountdownValue = startValue;
        updateCountdownDisplay();

        countdownTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), _ -> {
                if (currentCountdownValue > 0) {
                    currentCountdownValue--;
                    updateCountdownDisplay();
                }
            })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownDisplay() {
        if (currentPlant == null) return;

        String formattedTime;
        if (currentPlant.isUseDays()) {
            int days = currentCountdownValue / 86400;
            int hours = (currentCountdownValue % 86400) / 3600;
            int minutes = (currentCountdownValue % 3600) / 60;
            int seconds = currentCountdownValue % 60;
            
            if (days > 0) {
                formattedTime = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
            } else if (hours > 0) {
                formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                formattedTime = String.format("%02d:%02d", minutes, seconds);
            }
        } else {
            int hours = currentCountdownValue / 3600;
            int minutes = (currentCountdownValue % 3600) / 60;
            int seconds = currentCountdownValue % 60;
            
            if (hours > 0) {
                formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                formattedTime = String.format("%02d:%02d", minutes, seconds);
            }
        }
        
        int totalDelay = currentPlant.getDelay();
        String totalTime;
        if (currentPlant.isUseDays()) {
            // Convert days to seconds for display
            totalTime = totalDelay + "d";
        } else {
            // Convert hours to HH:mm format
            totalTime = String.format("%02d:%02d:00", totalDelay, 0);
        }
        
        Platform.runLater(() -> delayProgressText.setText(formattedTime + "/" + totalTime));
    }

    private void updateDelayText(Plant plant) {
        // Start or restart countdown with current delay value
        startCountdown(plant.getCurrentDelay());
    }

    private void stopRefreshTimeline() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        currentPlant = null;
    }

    // Update cleanup method
    public void cleanup() {
        stopRefreshTimeline();
        if (loadingAnimation != null) {
            loadingAnimation.stop();
            loadingAnimation = null;
        }
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
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
        // Delete user cache
        UserCache.clearCache();

        // Switch to login view
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
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    @FXML
    public void initialize() throws IOException {
        userPopout.setVisible(false);
        updateOverlay.setVisible(false);
        loadingIcon.setVisible(false);

        nameText.setText(MainApplication.getName());
        emailText.setText(MainApplication.getEmail());

        unselectPlant();

        // Only show plant selection if we're logged in (have a name)
        if (!MainApplication.getName().isEmpty()) {
            Platform.runLater(() -> {
                if (MainApplication.plants.length == 0) {
                    try {
                        openCreatePlantView();
                        MainApplication.showAlert(AlertType.INFORMATION, "Info", 
                            "Maak voor het gebruik eerst een plant aan.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (MainApplication.plants.length == 1) {
                    Plant firstPlant = MainApplication.plants[0];
                    loadPlantFromName(firstPlant.getName());
                } else {
                    showSearchDialog();
                }
            });
        }

        // Stop refresh timeline when switching views
        Platform.runLater(() -> {
            Scene scene = plantNameLabel.getScene();
            if (scene != null) {
                scene.windowProperty().addListener((_, _, newWindow) -> {
                    if (newWindow == null) {
                        stopRefreshTimeline();
                    }
                });
            }
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
}
