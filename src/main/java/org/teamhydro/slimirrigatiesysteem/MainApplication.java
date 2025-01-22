package org.teamhydro.slimirrigatiesysteem;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.Random;

import com.fazecast.jSerialComm.SerialPort;

public class MainApplication extends Application {

    protected static Connection dbConn;
    protected static SerialPort arduinoPort;
    protected static int baudRate = 9600;
    protected static int timeout = 1000;
    protected static int dataBits = 8;
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

    public static SerialPort getArduinoSerialConnection() {
        if (arduinoPort != null && arduinoPort.isOpen()) return arduinoPort;

        try {
            arduinoPort = setupArduinoConnection();
            return arduinoPort;
        } catch (Exception e) {
            System.out.println("Error establishing Arduino connection: " + e.getMessage());
            return null;
        }
    }

    protected static void sendDataToArduino(String data) {
        try {
            arduinoPort.getOutputStream().write((data + "\n").getBytes());
            arduinoPort.getOutputStream().flush();
            System.out.println("Sent: " + data);

            // Wait for Arduino to process the data (e.g., 100 ms)
            Thread.sleep(100);
        } catch (Exception e) {
            System.out.println("Error sending data: " + e.getMessage());
        }
    }

    protected static String receiveDataFromArduino() {
        try {
            byte[] buffer = new byte[1024]; // Buffer to hold incoming data
            int numRead = arduinoPort.readBytes(buffer, buffer.length); // Read available bytes
            if (numRead > 0) {
                String receivedData = new String(buffer, 0, numRead).trim(); // Convert bytes to string
                System.out.println("Received: " + receivedData);
                return receivedData; // Return the received string
            } else {
                System.out.println("No data received.");
                return null; // No data was read
            }
        } catch (Exception e) {
            System.out.println("Error reading from Arduino: " + e.getMessage());
            return null;
        }
    }

    public static FXMLLoader switchView(String fxmlName) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlName));
        Parent root = fxmlLoader.load();
        setCurrentController(fxmlLoader.getController());
    
        // Define the scale factor as a variable
        double scaleFactor = 1.411764705882353; // 48 / 34 (icons are 48px, but scaled to 34px, making this the optimal factor for high-res icons)
    
        // Create a Scale transformation
        Scale scale = new Scale();
        scale.setX(scaleFactor);  // Scale X by the scale factor
        scale.setY(scaleFactor);  // Scale Y by the scale factor
    
        // Apply the transformation to the root node
        root.getTransforms().add(scale);
    
        // Adjust the scene size based on the scale factor
        Scene scene = new Scene(root, 640 * scaleFactor, 400 * scaleFactor);
    
        String title = switch (fxmlName) {
            case "login-view.fxml" -> "Inloggen";
            case "plant-view.fxml" -> "Plant";
            case "passwordrecovery-view.fxml" -> "Wachtwoord Herstellen";
            case "userinfo-view.fxml" -> "Uw Gegevens";
            case "create-plant-view.fxml" -> "Nieuwe plant";
            default -> "Onbekend";
        };
        globalStage.setTitle(title + " - Slim Irrigatie Systeem");
        globalStage.setScene(scene);
        globalStage.show();

        return fxmlLoader;
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

    public static SerialPort setupArduinoConnection() {
        // Find all available serial ports
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println("Found port: " + port.getSystemPortName());
        }

        // Open the (first) serial port
        SerialPort arduinoPort = SerialPort.getCommPort(ports[0].getSystemPortName());
        arduinoPort.setBaudRate(baudRate); // Match this to your Arduino code's baud rate
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        arduinoPort.setNumDataBits(dataBits);
        arduinoPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        arduinoPort.setParity(SerialPort.NO_PARITY);

        if (arduinoPort.openPort()) {
            System.out.println("Port opened successfully!");
        } else {
            System.out.println("Failed to open port.");
            return null;
        }

        return arduinoPort;
    }

    private static boolean testArduinoConnectivity() {
        try {
            // Wait for the Arduino to connect
            while (getArduinoSerialConnection() == null) {
                System.out.println("Waiting for Arduino connection...");

                //noinspection BusyWait
                Thread.sleep(5000);
            }

            // Test communication
            sendDataToArduino("ping");
            String response = receiveDataFromArduino();
            if ("pong".equals(response)) {
                System.out.println("Arduino responded correctly!");
            } else {
                System.out.println("Arduino did not respond correctly.");
                Thread.sleep(2000);
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Error establishing Arduino connection: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        globalStage = stage;
        switchView("login-view.fxml");
        stage.setResizable(false);
    }

    @Override
    public void stop() throws Exception {
        // Get the current controller and clean up if needed
        if (currentController instanceof PlantViewController) {
            ((PlantViewController) currentController).cleanup();
        }
        
        // Close the Arduino port
        if (arduinoPort != null && arduinoPort.isOpen()) {
            arduinoPort.closePort();
        }
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

        int currentAttempt = 0;
        boolean connected = false; // Variable to track the connection status

        // Attempt to connect to the Arduino, retry up to 5 times
        while (currentAttempt < 10 && !(connected = testArduinoConnectivity())) {
            currentAttempt++;
            System.out.println("Retrying connection attempt " + (currentAttempt + 1) + "...");
        }

        if (connected) {
            // Launch the JavaFX application if Arduino is connected
            launch(args);
        } else {
            System.out.println("Failed to connect to Arduino.");

            // Ensure the JavaFX toolkit is initialized only if needed
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> {
                    showAlert(AlertType.ERROR, "Fout", "Kan geen verbinding maken met de Arduino. Controleer de verbinding en probeer het opnieuw.");
                    System.exit(1);
                });
            } else {
                showAlert(AlertType.ERROR, "Fout", "Kan geen verbinding maken met de Arduino. Controleer de verbinding en probeer het opnieuw.");
                System.exit(1);
            }
        }
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

    @FXML
    public static void fadeOut(AnchorPane element, int time) {
        FadeTransition fadeOut = new FadeTransition();
        fadeOut.setNode(element);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDuration(Duration.millis(time));
        fadeOut.setOnFinished(event -> element.setVisible(false));

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
}