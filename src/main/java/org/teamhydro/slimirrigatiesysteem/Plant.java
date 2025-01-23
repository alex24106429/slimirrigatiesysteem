package org.teamhydro.slimirrigatiesysteem;

import org.json.JSONObject;

import java.util.Objects;

public class Plant {
    private int plantId;
    private String name;
    private String plantType;
    private boolean useDays;
    private int totalDelayMs;
    private int currentDelay;
    private int outputML;
    private int minimumMoistureLevel;
    private int currentMoistureLevel;

    // Constructor
    public Plant(String name, String plantType, boolean useDays, int delay, int outputML, int minimumMoistureLevel, int currentMoistureLevel) {
        this.plantId = MainApplication.getRandomInt(0, Integer.MAX_VALUE - 1);
        this.name = name;
        this.plantType = plantType; // "african_violet", "hedgehog_cactus", "monstera", "pothos", "zz_plant", "aloe_vera", "jasmine", "orchid", "rosemary", "english_ivy", "lavender", "peace_lily", "snake_plant", "fiddle_leaf_fig", "mexican_fence_post_cactus", "philodendron", "spider_plant"
        this.useDays = useDays;
        this.totalDelayMs = delay;
        this.outputML = outputML;
        this.minimumMoistureLevel = minimumMoistureLevel;
        this.currentMoistureLevel = currentMoistureLevel;
    }

    public class RefreshResult {
        private final boolean success;
        private final String newPlantName;
        private final boolean shouldReupload;

        public RefreshResult(boolean success, String newPlantName) {
            this(success, newPlantName, false);
        }

        public RefreshResult(boolean success, String newPlantName, boolean shouldReupload) {
            this.success = success;
            this.newPlantName = newPlantName;
            this.shouldReupload = shouldReupload;
        }

        public boolean isSuccess() { return success; }
        public String getNewPlantName() { return newPlantName; }
        public boolean shouldReupload() { return shouldReupload; }
    }

    public RefreshResult refreshFromArduino() {
        try {
            System.out.println("Starting Arduino refresh...");
            
            if (!MainApplication.sendDataToArduino("fetch")) {
                System.out.println("Failed to send fetch command");
                return new RefreshResult(false, null);
            }
            
            String response = MainApplication.getLastReceivedResponse();
            System.out.println("Received response: " + response);
            
            if (response == null || !response.startsWith("[{") || !response.endsWith("}]")) {
                System.out.println("Invalid response format");
                return new RefreshResult(false, null);
            }

            response = response.substring(1, response.length() - 1);
            JSONObject jsonResponse = new JSONObject(response);
            
            // Check if Arduino has different plant
            if (jsonResponse.has("plantName")) {
                String arduinoPlantName = jsonResponse.getString("plantName");
                if (!arduinoPlantName.equals(this.name)) {
                    // Check if the Arduino plant name is empty
                    if (!arduinoPlantName.isEmpty()) {
                        // Check if the Arduino plant name exists in the list of plants
                        if (MainApplication.getPlantByName(arduinoPlantName) != null) {
                            // Ask user if they want to switch to the new plant
                            if (MainApplication.showConfirmationDialog("Plant mismatch detected. Do you want to switch to the new plant?")) {
                                // Return the new plant name
                                return new RefreshResult(true, arduinoPlantName);
                            }
                        }
                    }

                    System.out.println("Plant mismatch detected. Updating Arduino...");
                    return new RefreshResult(true, null, true);
                }
            }

            // Update only if values exist and are valid
            if (jsonResponse.has("currentDelay")) {
                String delayStr = jsonResponse.getString("currentDelay");
                try {
                    this.currentDelay = Integer.parseInt(delayStr);
                    System.out.println("Updated currentDelay to: " + this.currentDelay);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid currentDelay value: " + delayStr);
                }
            }

            if (jsonResponse.has("shouldUseDays")) {
                String useDaysStr = jsonResponse.getString("shouldUseDays");
                this.useDays = Boolean.parseBoolean(useDaysStr);
                System.out.println("Updated useDays to: " + this.useDays);
            }

            if (jsonResponse.has("moistureLevel")) {
                String moistureStr = jsonResponse.getString("moistureLevel");
                try {
                    int moisture = Integer.parseInt(moistureStr);
                    if (moisture >= 0) {
                        this.currentMoistureLevel = moisture;
                        System.out.println("Updated moistureLevel to: " + this.currentMoistureLevel);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid moistureLevel value: " + moistureStr);
                }
            }

            return new RefreshResult(true, null);
        } catch (Exception e) {
            System.out.println("Error refreshing plant data: " + e.getMessage());
            e.printStackTrace();
            return new RefreshResult(false, null);
        }
    }

    // Getter and Setter methods for each property
    public int getPlantId() {
        return plantId;
    }

    public void setPlantId(int plantId) {
        this.plantId = plantId;
    }

    public String getName() {
        return name;  // Getter for name
    }

    public void setName(String name) {
        this.name = name;  // Setter for name
    }

    public String getPlantType() {
        return plantType;  // Getter for plantType
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;  // Setter for plantType
    }

    public boolean isUseDays() {
        return useDays;
    }

    public void setUseDays(boolean useDays) {
        this.useDays = useDays;
    }

    public int getDelay() {
        return totalDelayMs;
    }

    public int getCurrentDelay() {
        return currentDelay;
    }

    public void setCurrentDelay(int currentDelay) {
        this.currentDelay = currentDelay;
    }

    public void setDelay(int delay) {
        this.totalDelayMs = delay;
    }

    public int getOutputML() {
        return outputML;
    }

    public void setOutputML(int outputML) {
        this.outputML = outputML;
    }

    public int getMinimumMoistureLevel() {
        return minimumMoistureLevel;
    }

    public void setMinimumMoistureLevel(int minimumMoistureLevel) {
        this.minimumMoistureLevel = minimumMoistureLevel;
    }

    public double getCurrentMoistureLevel() {
        return currentMoistureLevel;
    }

    public void setCurrentMoistureLevel(int currentMoistureLevel) {
        this.currentMoistureLevel = currentMoistureLevel;
    }

    public String getType() {
        return plantType;
    }

    public boolean getUseDays() {
        return useDays;
    }
}
