package org.teamhydro.slimirrigatiesysteem;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArduinoController {
    private static SerialPort arduinoPort;
    private static final int BAUD_RATE = 4800;
    private static final int TIMEOUT = 6000;
    private static final int DATA_BITS = 8;
    private static String remainingData = "";
    private static String lastReceivedResponse = null;
    private static Map<String, String> arduinoNames = new HashMap<>();
    private static Long hasReceivedReadyAck = null;

    public static class ArduinoDevice {
        private final SerialPort port;
        private final String portName;
        private String deviceName;

        public ArduinoDevice(SerialPort port, String portName, String deviceName) {
            this.port = port;
            this.portName = portName;
            this.deviceName = deviceName;
        }

        public SerialPort getPort() { return port; }
        public String getPortName() { return portName; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String name) { this.deviceName = name; }
    }

    public static List<ArduinoDevice> findAllArduinoDevices() {
        List<ArduinoDevice> devices = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        
        for (SerialPort port : ports) {
            try {
                // Try to connect to this port
                port.setBaudRate(BAUD_RATE);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, TIMEOUT);
                port.setNumDataBits(DATA_BITS);
                port.setNumStopBits(SerialPort.ONE_STOP_BIT);
                port.setParity(SerialPort.NO_PARITY);

                if (port.openPort()) {
                    // Store current port
                    arduinoPort = port;
                    
                    // Try to get device name
                    String deviceName = "Koppel Arduino";
                    if (sendDataToArduino("getUnitName")) {
                        String response = getLastReceivedResponse();
                        if (response != null && !response.trim().isEmpty()) {
                            deviceName = response.substring(10); // "UNIT_NAME-" is 10 characters long
                            System.out.println("found Arduino: " + deviceName);
                        }
                    }
                    
                    devices.add(new ArduinoDevice(port, port.getSystemPortName(), deviceName));
                    
                    // Close this port so we can check others
                    port.closePort();
                }
            } catch (Exception e) {
                System.out.println("Error checking port " + port.getSystemPortName() + ": " + e.getMessage());
            }
        }
        
        // Clear the current port
        arduinoPort = null;
        return devices;
    }

    public static boolean connectToArduino(ArduinoDevice device) {
        try {
            // If we already have a connection, close it
            if (arduinoPort != null && arduinoPort.isOpen()) {
                arduinoPort.closePort();
            }

            // Set up the new connection
            arduinoPort = device.getPort();
            if (!arduinoPort.isOpen() && !arduinoPort.openPort()) {
                return false;
            }

            // Configure port settings
            arduinoPort.setBaudRate(BAUD_RATE);
            arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, TIMEOUT);
            arduinoPort.setNumDataBits(DATA_BITS);
            arduinoPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            arduinoPort.setParity(SerialPort.NO_PARITY);

            // Test connection
            return testArduinoConnectivity();
        } catch (Exception e) {
            System.out.println("Error connecting to Arduino: " + e.getMessage());
            return false;
        }
    }

    public static boolean setArduinoName(String newName) {
        if (arduinoPort == null || !arduinoPort.isOpen()) {
            return false;
        }

        return sendDataToArduino("setname-" + newName);
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

    public static SerialPort setupArduinoConnection() {
        // Find all available serial ports
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println("Found port: " + port.getSystemPortName());
        }

        // Open the (first) serial port
        SerialPort arduinoPort = SerialPort.getCommPort(ports[0].getSystemPortName());
        arduinoPort.setBaudRate(BAUD_RATE);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, TIMEOUT);
        arduinoPort.setNumDataBits(DATA_BITS);
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

    public static boolean testArduinoConnectivity() {
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

    public static boolean sendDataToArduino(String data) {
        if (arduinoPort == null || !arduinoPort.isOpen()) {
            System.out.println("Arduino port is not open");
            return false;
        }

        try {
            // Clear input buffer and remaining data
            clearInputBuffer();
            clearRemainingData();
            Thread.sleep(500);

            // Step 1: Check if we have received READY_ACK within the last 30 seconds
            long currentTime = System.currentTimeMillis();
            if (hasReceivedReadyAck != null && (currentTime - hasReceivedReadyAck < 30000)) {
                System.out.println("Device is assumed to be in a ready state.");
            } else {
                // Send READY and wait for acknowledgment
                System.out.println("Sending READY signal...");
                int readyAttempts = 3;
                boolean readyAcknowledged = false;

                while (readyAttempts > 0 && !readyAcknowledged) {
                    arduinoPort.getOutputStream().write("READY\n".getBytes());
                    arduinoPort.getOutputStream().flush();
                    Thread.sleep(500);

                    String response = waitForResponse(TIMEOUT);
                    System.out.println("READY response: " + response);
                    if (response != null && response.contains("READY_ACK")) {
                        readyAcknowledged = true;
                        hasReceivedReadyAck = System.currentTimeMillis(); // Update the timestamp
                        break;
                    }
                    readyAttempts--;
                    Thread.sleep(200);
                }

                if (!readyAcknowledged) {
                    System.out.println("Failed to establish ready handshake after 3 attempts");
                    return false;
                }
            }

            // Step 2: Send message with checksum after a delay
            System.out.println("Sending message with checksum after a delay (" + (TIMEOUT - (TIMEOUT / 3)) + "ms)");
            Thread.sleep(TIMEOUT - (TIMEOUT / 3));
            int checksum = calculateChecksum(data);
            String message = String.format("MSG:%d:%s\n", checksum, data);
            System.out.println("Sending message: " + message.trim());

            arduinoPort.getOutputStream().write(message.getBytes());
            arduinoPort.getOutputStream().flush();
            Thread.sleep(500);

            // Step 3: Wait for checksum acknowledgment
            String checksumResponse = waitForResponse(TIMEOUT);

            if ("CHECKSUM_OK".equals(checksumResponse)) {
                // Step 4: Wait for command response
                String commandResponse = waitForResponse(TIMEOUT);

                System.out.println("Command response: " + commandResponse);

                if (data.equals("fetch")) {
                    lastReceivedResponse = commandResponse;  // Store the response
                    return commandResponse != null && commandResponse.startsWith("{") && commandResponse.endsWith("}");
                }
                if (data.equals("ping")) {
                    lastReceivedResponse = commandResponse;
                    return commandResponse != null && commandResponse.contains("pong");
                }
                if (data.equals("getUnitName")) {
                    lastReceivedResponse = commandResponse;
                    return commandResponse != null && commandResponse.contains("UNIT_NAME-");
                }

                lastReceivedResponse = commandResponse;
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
            if (protocolMessage.contains("READY_ACK") || 
                protocolMessage.contains("CHECKSUM_OK") || 
                protocolMessage.contains("pong") || 
                protocolMessage.contains("Done") || 
                protocolMessage.startsWith("UNIT_NAME-") || 
                (protocolMessage.trim().startsWith("{") && protocolMessage.trim().endsWith("}"))) {
                
                return protocolMessage;
            }
            
            System.out.println("No valid protocol message found: " + protocolMessage);
            
            // Move to next potential message
            currentIndex = endMarker + 3;
        }
        return null;
    }

    private static void clearRemainingData() {
        remainingData = "";
    }

    public static String getLastReceivedResponse() {
        String response = lastReceivedResponse;
        lastReceivedResponse = null;  // Clear after reading
        return response;
    }

    public static boolean sendPlantConfig(Plant plant) {
        try {
            int maxRetries = 5;

            // Send each piece of configuration separately
            if (!sendDataWithRetry("pn-" + plant.getName(), maxRetries)) {
                System.out.println("Failed to send plant name after retries");
                return false;
            }

            if (!sendDataWithRetry("pt-" + plant.getType(), maxRetries)) {
                System.out.println("Failed to send plant type after retries");
                return false;
            }

            if (!sendDataWithRetry("ud-" + plant.getUseDays(), maxRetries)) {
                System.out.println("Failed to send use days setting after retries");
                return false;
            }

            if (!sendDataWithRetry("dt-" + plant.getDelay(), maxRetries)) {
                System.out.println("Failed to send delay time after retries");
                return false;
            }

            if (!sendDataWithRetry("om-" + plant.getOutputML(), maxRetries)) {
                System.out.println("Failed to send output ML after retries");
                return false;
            }

            if (!sendDataWithRetry("mm-" + plant.getMinimumMoistureLevel(), maxRetries)) {
                System.out.println("Failed to send minimum moisture level after retries");
                return false;
            }

            // Finally, tell Arduino to apply all settings
            if (!sendDataWithRetry("apply", maxRetries)) {
                System.out.println("Failed to apply settings after retries");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Error sending plant configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean sendDataWithRetry(String data, int maxRetries) throws InterruptedException {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.println("Sending data: " + data + " (attempt " + attempt + ")");
            if (sendDataToArduino(data)) {
                Thread.sleep(100); // Wait after successful send
                return true;
            }
            System.out.println("Attempt " + attempt + " failed to send: " + data);
            Thread.sleep(100); // Wait before retrying
        }
        return false; // All attempts failed
    }

    public static void closePort() {
        if (arduinoPort != null && arduinoPort.isOpen()) {
            arduinoPort.closePort();
        }
    }
}
