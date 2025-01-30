package org.teamhydro.slimirrigatiesysteem;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

public class SelectArduinoController {
    @FXML
    private Label emailText;

    @FXML
    private Label nameText;

    @FXML
    private Label plantNameLabel;

    @FXML
    private TextField searchBar;

    @FXML
    private ListView<ArduinoController.ArduinoDevice> searchListView;

    @FXML
    private AnchorPane searchOverlay;

    @FXML
    private AnchorPane userPopout;

    @FXML
    private ProgressIndicator refreshProgress;  // Add this to your FXML

    @FXML
    private StackPane listViewContainer;  // Add this to your FXML to contain both ListView and ProgressIndicator

    private List<ArduinoController.ArduinoDevice> allDevices;
    private volatile boolean isRefreshing = false;

    @FXML
    private void hideUserPopout() {
        MainApplication.fadeOut(userPopout, 100);
    }

    @FXML
    private void logout() throws IOException {
        MainApplication.switchView("login-view.fxml");
    }

    @FXML
    void onSearchKeypress(KeyEvent event) {
        searchListView.getItems().clear();
        String searchText = searchBar.getText().toLowerCase();

        // Filter devices based on search text
        for (ArduinoController.ArduinoDevice device : allDevices) {
            if (device.getDeviceName().toLowerCase().contains(searchText) ||
                device.getPortName().toLowerCase().contains(searchText)) {
                searchListView.getItems().add(device);
            }
        }
    }

    @FXML
    void onSearchConfirm(MouseEvent event) {
        ArduinoController.ArduinoDevice selectedDevice = searchListView.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) return;

        if ("Koppel Arduino".equals(selectedDevice.getDeviceName())) {
            handleNewArduino(selectedDevice);
        } else {
            connectToExistingArduino(selectedDevice);
        }
    }

    private void handleNewArduino(ArduinoController.ArduinoDevice device) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Arduino Naam");
        dialog.setHeaderText("Geef deze Arduino een naam");
        dialog.setContentText("Naam:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String newName = result.get().trim();
            if (newName.isEmpty()) {
                MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                    "Fout", "De naam mag niet leeg zijn.");
                return;
            }

            if (ArduinoController.connectToArduino(device)) {
                if (ArduinoController.setArduinoName(newName)) {
                    device.setDeviceName(newName);
                    proceedToCreatePlantView();
                } else {
                    MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                        "Fout", "Kon de naam niet instellen voor deze Arduino.");
                }
            } else {
                MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                    "Fout", "Kon geen verbinding maken met de Arduino.");
            }
        }
    }

    private void connectToExistingArduino(ArduinoController.ArduinoDevice device) {
        if (ArduinoController.connectToArduino(device)) {
            proceedToPlantView();
        } else {
            MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Fout", "Kon geen verbinding maken met de Arduino.");
        }
    }

    private void proceedToCreatePlantView() {
        try {
            MainApplication.switchView("create-plant-view.fxml");
        } catch (IOException e) {
            MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Fout", "Kon het plant overzicht niet laden.");
        }
    }

    private void proceedToPlantView() {
        try {
            MainApplication.switchView("plant-view.fxml");
        } catch (IOException e) {
            MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Fout", "Kon het plant overzicht niet laden.");
        }
    }

    @FXML
    void openUsersettings(ActionEvent event) throws IOException {
        MainApplication.switchView("userinfo-view.fxml");
    }

    @FXML
    void refreshButtonClicked(MouseEvent event) {
        // Prevent multiple simultaneous refreshes
        if (isRefreshing) {
            return;
        }
        refreshArduinoList();
    }

    private void refreshArduinoList() {
        isRefreshing = true;
        
        // Show progress indicator
        Platform.runLater(() -> {
            refreshProgress.setVisible(true);
            searchListView.setDisable(true);  // Disable list while refreshing
            searchBar.setDisable(true);       // Disable search while refreshing
        });

        Task<List<ArduinoController.ArduinoDevice>> refreshTask = new Task<>() {
            @Override
            protected List<ArduinoController.ArduinoDevice> call() {
                // Perform the Arduino device search in background
                return ArduinoController.findAllArduinoDevices();
            }
        };

        refreshTask.setOnSucceeded(event -> {
            // Update UI on success
            Platform.runLater(() -> {
                allDevices = refreshTask.getValue();
                searchListView.getItems().clear();
                searchListView.getItems().addAll(allDevices);
                
                // Hide progress indicator and re-enable controls
                refreshProgress.setVisible(false);
                searchListView.setDisable(false);
                searchBar.setDisable(false);
                isRefreshing = false;
            });
        });

        refreshTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                // Handle failure
                Throwable exception = refreshTask.getException();
                MainApplication.showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Fout", "Kon Arduino's niet vinden: " + exception.getMessage());
                
                // Hide progress indicator and re-enable controls
                refreshProgress.setVisible(false);
                searchListView.setDisable(false);
                searchBar.setDisable(false);
                isRefreshing = false;
            });
        });

        // Start the background task
        Thread refreshThread = new Thread(refreshTask);
        refreshThread.setDaemon(true);  // Make sure thread doesn't prevent app shutdown
        refreshThread.start();
    }

    @FXML
    private void showUserPopout() {
        MainApplication.fadeIn(userPopout, 200);
    }

    @FXML
    public void initialize() throws IOException {
        userPopout.setVisible(false);
        nameText.setText(MainApplication.getName());
        emailText.setText(MainApplication.getEmail());
        refreshProgress.setVisible(false);  // Hide progress indicator initially

        // Set up cell factory to display device names
        searchListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ArduinoController.ArduinoDevice device, boolean empty) {
                super.updateItem(device, empty);
                if (empty || device == null) {
                    setText(null);
                } else {
                    setText(device.getDeviceName() + " (" + device.getPortName() + ")");
                }
            }
        });

        // Initial refresh
        refreshArduinoList();
    }
}
