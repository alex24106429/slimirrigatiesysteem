package org.teamhydro.slimirrigatiesysteem;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MainApplication extends Application {

    protected static Connection dbConn;
    private static Object currentController;

    public static Connection getDatabaseConnection() {
        if (dbConn != null) return dbConn;

        try {
            // Get the local sqlite database
            String url = "jdbc:sqlite:slimirrigatiesysteem.db";
            dbConn = DriverManager.getConnection(url);
            return dbConn;
        } catch (Exception e) {
            // Print the exception
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static FXMLLoader switchView(String fxmlName) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlName));
        Parent root = fxmlLoader.load();
    
        // Define the scale factor as a variable
        double scaleFactor = 1.411764705882353; // 48 / 34 (icons are 48px, but scaled to 34px, making this the optimal factor for high-res icons)
    
        // Create a Scale transformation
        Scale scale = new Scale();
        scale.setX(scaleFactor);  // Scale X by the scale factor
        scale.setY(scaleFactor);  // Scale Y by the scale factor
    
        // Apply the transformation to the root node
        root.getTransforms().add(scale);

        // Set the application icon
        globalStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("/org/teamhydro/slimirrigatiesysteem/icons/droplet.png")));
    
        // Adjust the scene size based on the scale factor
        Scene scene = new Scene(root, 640 * scaleFactor, 400 * scaleFactor);
        
        String title = getTitleForView(fxmlName);
        globalStage.setTitle(title + " - Slim Irrigatie Systeem");
        globalStage.setScene(scene);
        globalStage.show();

        return fxmlLoader;
    }

    private static String getTitleForView(String fxmlName) {
        return switch (fxmlName) {
            case "login-view.fxml" -> "Inloggen";
            case "plant-view.fxml" -> "Plant";
            case "passwordrecovery-view.fxml" -> "Wachtwoord Herstellen";
            case "userinfo-view.fxml" -> "Uw Gegevens";
            case "create-plant-view.fxml" -> "Nieuwe plant";
            case "loading-view.fxml" -> "Verbinding maken";
            case "select-arduino-view.fxml" -> "Arduino selecteren";
            default -> "Onbekend";
        };
    }

    public static Stage globalStage;

    private static void setupLocalDatabase() {
        // Check if the connection is available
        if (getDatabaseConnection() == null) return;

        // Create the database
        String createPlantsTable = """
                    CREATE TABLE IF NOT EXISTS plants (
                        PlantId INTEGER PRIMARY KEY AUTOINCREMENT,
                        Name TEXT NOT NULL
                    )
                """;

        String createPlantConfigsTable = """
                    CREATE TABLE IF NOT EXISTS plant_configs (
                        PlantId INTEGER PRIMARY KEY,
                        PlantType TEXT NOT NULL,
                        UseDays BOOLEAN NOT NULL,
                        Delay INTEGER NOT NULL,
                        OutputML INTEGER NOT NULL,
                        MinimumMoistureLevel INTEGER NOT NULL,
                        CurrentMoistureLevel DOUBLE NOT NULL
                    )
                """;

        try (PreparedStatement statement = Objects.requireNonNull(getDatabaseConnection()).prepareStatement(createPlantsTable)) {
            statement.executeUpdate();
        } catch (Exception e) {
            // Print the exception
            System.out.println(e.getMessage());
        }

        try (PreparedStatement statement = Objects.requireNonNull(getDatabaseConnection()).prepareStatement(createPlantConfigsTable)) {
            statement.executeUpdate();
        } catch (Exception e) {
            // Print the exception
            System.out.println(e.getMessage());
        }

        // Print a success message
        System.out.println("Database setup complete.");
    }

    @Override
    public void start(Stage stage) throws IOException {
        globalStage = stage;
        stage.setResizable(false);
        
        // First switch to loading view
        switchView("loading-view.fxml");
        
        // Start initialization process
        Thread initThread = new Thread(() -> {
            try {
                // Get the loading controller instance
                LoadingViewController loadingController = LoadingViewController.getInstance();
                
                // Start looking for Arduino devices
                loadingController.startOperation(
                    "Zoeken naar Arduino apparaten",
                    "Even geduld tijdens het zoeken...",
                    () -> {
                        try {
                            start(stage);
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                loadingController.operationFailed(
                                    "Fout opgetreden",
                                    "Er is een fout opgetreden bij het herstarten van de applicatie"
                                );
                            });
                        }
                    }
                );
                
                // Give UI time to update
                Thread.sleep(500);
                
                // Look for Arduino devices
                List<ArduinoController.ArduinoDevice> devices = ArduinoController.findAllArduinoDevices();
                
                if (devices.isEmpty()) {
                    loadingController.operationFailed(
                        "Geen Arduino's gevonden",
                        "Sluit een Arduino aan en probeer het opnieuw"
                    );
                } else {
                    loadingController.updateProgress(
                        "Arduino's gevonden",
                        String.format("%d Arduino(s) gevonden", devices.size())
                    );
                    Thread.sleep(1000); // Show success message briefly
                    loadingController.operationSucceeded("select-arduino-view.fxml");
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    LoadingViewController.getInstance().operationFailed(
                        "Fout opgetreden",
                        "Er is een fout opgetreden tijdens het zoeken naar Arduino's"
                    );
                });
            }
        });
        
        initThread.setDaemon(true);
        initThread.start();
    }

    @Override
    public void stop() throws Exception {
        // Get the current controller and clean up if needed
        if (currentController instanceof PlantViewController) {
            ((PlantViewController) currentController).cleanup();
        }
        
        // Close the Arduino port
        ArduinoController.closePort();
        super.stop();
    }

    public static int getRandomInt(int min, int max) {
        // Create a Random object
        Random rand = new Random();

        // Generate a random integer between min and max (inclusive)
        return rand.nextInt(max - min + 1) + min;
    }

    static Plant[] plants = Objects.requireNonNull(PlantRepository.getAllPlants()).toArray(new Plant[0]);

    public static Plant getPlantByName(String name) {
        for(Plant plant: plants) {
            if (plant.getName().equals(name)) return plant;
        }

        return null;
    }

    public static void main(String[] args) {
        // Set up the local database
        setupLocalDatabase();

        // Launch the JavaFX application with the loading screen
        launch(args);
    }

    public static String getName() {
        return ApiController.getStoredName();
    }

    public static String getEmail() {
        return ApiController.getStoredEmail();
    }

    public static String getAddress() {
        return ApiController.getStoredAddress();
    }

    public static void updateUserInfo(String name, String address, String email) throws Exception {
        ApiController.storeUserData(ApiController.getStoredToken(), name, address, email);
        ApiController.updateUserInfo(name, address, email);
    }

    @FXML
    public static void fadeOut(AnchorPane element, int time) {
        FadeTransition fadeOut = new FadeTransition();
        fadeOut.setNode(element);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDuration(Duration.millis(time));
        fadeOut.setOnFinished(_ -> element.setVisible(false));

        fadeOut.play();
    }

    @FXML
    public static void fadeIn(AnchorPane element, int time) {
        element.setVisible(true);

        FadeTransition fadeIn = new FadeTransition();
        fadeIn.setNode(element);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setDuration(Duration.millis(time));

        fadeIn.play();
    }

    public static void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void setCurrentController(Object controller) {
        currentController = controller;
    }

    public static boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}