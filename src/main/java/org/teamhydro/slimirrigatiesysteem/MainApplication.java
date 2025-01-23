package org.teamhydro.slimirrigatiesysteem;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadException;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.scene.Parent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
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
    private static String remainingData = "";  // Add this class field to store leftover data
    private static String lastReceivedResponse = null;  // Add this field

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

    protected static boolean sendDataToArduino(String data) {
        if (arduinoPort == null || !arduinoPort.isOpen()) {
            System.out.println("Arduino port is not open");
            return false;
        }
    
        try {
            // Clear input buffer and remaining data
            clearInputBuffer();
            clearRemainingData();
            Thread.sleep(500);
    
            // Step 1: Send READY and wait for acknowledgment
            System.out.println("Sending READY signal...");
            int readyAttempts = 3;
            boolean readyAcknowledged = false;
            
            while (readyAttempts > 0 && !readyAcknowledged) {
                arduinoPort.getOutputStream().write("READY\n".getBytes());
                arduinoPort.getOutputStream().flush();
                Thread.sleep(500);
                
                String response = waitForResponse(3000);
                System.out.println("READY response: " + response);
                if (response != null && response.contains("READY_ACK")) {
                    readyAcknowledged = true;
                    break;
                }
                readyAttempts--;
                Thread.sleep(200);
            }
            
            if (!readyAcknowledged) {
                System.out.println("Failed to establish ready handshake after 3 attempts");
                return false;
            }
    
            // Step 2: Send message with checksum
            Thread.sleep(200);
            int checksum = calculateChecksum(data);
            String message = String.format("MSG:%d:%s\n", checksum, data);
            System.out.println("Sending message: " + message.trim());
            
            arduinoPort.getOutputStream().write(message.getBytes());
            arduinoPort.getOutputStream().flush();
            Thread.sleep(500);
    
            // Step 3: Wait for checksum acknowledgment
            String checksumResponse = waitForResponse(3000);
            
            if ("CHECKSUM_OK".equals(checksumResponse)) {
                // Step 4: Wait for command response
                String commandResponse = waitForResponse(3000);
    
                if (data.equals("fetch")) {
                    lastReceivedResponse = commandResponse;  // Store the response
                    return commandResponse != null && commandResponse.startsWith("[{") && commandResponse.endsWith("}]");
                }
                if (data.equals("ping")) {
                    return commandResponse != null && commandResponse.contains("pong");
                }
                return commandResponse != null && commandResponse.contains("Done");
            }
    
            return false;
    
        } catch (Exception e) {
            System.out.println("Error in communication: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void clearInputBuffer() throws Exception {
        while (arduinoPort.bytesAvailable() > 0) {
            byte[] buffer = new byte[arduinoPort.bytesAvailable()];
            arduinoPort.readBytes(buffer, buffer.length);
        }
    }

    private static int calculateChecksum(String data) {
        int checksum = 0;
        for (char c : data.toCharArray()) {
            checksum += c;
        }
        return checksum;
    }

    private static String waitForResponse(int timeoutMs) {
        try {
            long startTime = System.currentTimeMillis();
            
            // First check if we have any remaining data from previous reads
            if (!remainingData.isEmpty()) {
                String result = processReceivedData(remainingData);
                if (result != null) {
                    // Clear the processed part from remainingData
                    int endMarker = remainingData.indexOf("###", remainingData.indexOf("###PROTOCOL:") + 12);
                    if (endMarker >= 0) {
                        remainingData = remainingData.substring(endMarker + 3).trim();
                    }
                    return result;
                }
            }
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (arduinoPort.bytesAvailable() > 0) {
                    Thread.sleep(100); // Allow more data to accumulate
                    
                    byte[] buffer = new byte[1024];
                    int numRead = arduinoPort.readBytes(buffer, buffer.length);
                    
                    if (numRead > 0) {
                        String received = remainingData + new String(buffer, 0, numRead).trim();
                        System.out.println("Raw received: " + received);
                        
                        String result = processReceivedData(received);
                        if (result != null) {
                            // Store any remaining data after the processed message
                            int endMarker = received.indexOf("###", received.indexOf("###PROTOCOL:") + 12);
                            if (endMarker >= 0) {
                                remainingData = received.substring(endMarker + 3).trim();
                            }
                            return result;
                        }
                        
                        // If no valid message was found, store all data as remaining
                        remainingData = received;
                    }
                }
                Thread.sleep(50);
            }
            
            System.out.println("Timeout reached without valid response");
            return null;
            
        } catch (Exception e) {
            System.out.println("Error waiting for response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String processReceivedData(String data) {
        int currentIndex = 0;
        while (true) {
            int startMarker = data.indexOf("###PROTOCOL:", currentIndex);
            if (startMarker < 0) break;
            
            int endMarker = data.indexOf("###", startMarker + 12);
            if (endMarker < 0) break;
            
            String protocolMessage = data.substring(startMarker + 12, endMarker);
            System.out.println("Found protocol message: " + protocolMessage);
            
            // Return immediately on valid protocol responses
            if (protocolMessage.matches(".*(READY_ACK|CHECKSUM_OK|pong|Done).*") || 
                (protocolMessage.trim().startsWith("[") && protocolMessage.trim().endsWith("]"))) {
                return protocolMessage;
            }
            
            // Move to next potential message
            currentIndex = endMarker + 3;
        }
        return null;
    }

    protected static String receiveDataFromArduino() {
        try {
            int noDataCount = 0;
            final int MAX_WAIT_CYCLES = 40;
            
            System.out.println("Waiting for Arduino response...");
            
            while (noDataCount < MAX_WAIT_CYCLES) {
                if (arduinoPort.bytesAvailable() > 0) {
                    noDataCount = 0;
                    
                    byte[] buffer = new byte[arduinoPort.bytesAvailable()];
                    int numRead = arduinoPort.readBytes(buffer, buffer.length);
                    
                    if (numRead > 0) {
                        String received = new String(buffer, 0, numRead);
                        
                        // Look for protocol messages
                        int startMarker = received.indexOf("###PROTOCOL:");
                        int endMarker = received.indexOf("###", startMarker + 12);
                        
                        if (startMarker >= 0 && endMarker >= 0) {
                            String protocolMessage = received.substring(startMarker + 12, endMarker);
                            return protocolMessage;
                        }
                    }
                    
                    Thread.sleep(50);
                } else {
                    noDataCount++;
                    Thread.sleep(100);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            System.out.println("Error reading from Arduino: " + e.getMessage());
            e.printStackTrace();
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

    protected static boolean testArduinoConnectivity() {
        try {
            // Wait for the Arduino to connect
            int attempts = 3;
            while (attempts > 0 && getArduinoSerialConnection() == null) {
                System.out.println("Waiting for Arduino connection... " + attempts + " attempts remaining");
                Thread.sleep(5000);
                attempts--;
            }
            if (attempts == 0) {
                System.out.println("Failed to establish Arduino connection after 3 attempts");
                return false;
            }

            // Clear any existing data
            while (arduinoPort.bytesAvailable() > 0) {
                arduinoPort.readBytes(new byte[arduinoPort.bytesAvailable()], arduinoPort.bytesAvailable());
            }

            // Test communication
            return sendDataToArduino("ping");
            
        } catch (Exception e) {
            System.out.println("Error establishing Arduino connection: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        globalStage = stage;
        
        switchView("loading-view.fxml");
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

    private static void clearRemainingData() {
        remainingData = "";
    }

    protected static boolean sendPlantConfig(Plant plant) {
        try {
            // Send each piece of configuration separately
            if (!sendDataToArduino("pn-" + plant.getName())) {
                System.out.println("Failed to send plant name");
                return false;
            }
            Thread.sleep(100);

            if (!sendDataToArduino("pt-" + plant.getType())) {
                System.out.println("Failed to send plant type");
                return false;
            }
            Thread.sleep(100);

            if (!sendDataToArduino("ud-" + plant.getUseDays())) {
                System.out.println("Failed to send use days setting");
                return false;
            }
            Thread.sleep(100);

            if (!sendDataToArduino("dt-" + plant.getDelay())) {
                System.out.println("Failed to send delay time");
                return false;
            }
            Thread.sleep(100);

            if (!sendDataToArduino("om-" + plant.getOutputML())) {
                System.out.println("Failed to send output ML");
                return false;
            }
            Thread.sleep(100);

            if (!sendDataToArduino("mm-" + plant.getMinimumMoistureLevel())) {
                System.out.println("Failed to send minimum moisture level");
                return false;
            }
            Thread.sleep(100);

            // Finally, tell Arduino to apply all settings
            if (!sendDataToArduino("apply")) {
                System.out.println("Failed to apply settings");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Error sending plant configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Add this method to get the last response
    protected static String getLastReceivedResponse() {
        String response = lastReceivedResponse;
        lastReceivedResponse = null;  // Clear after reading
        return response;
    }

    public static boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}