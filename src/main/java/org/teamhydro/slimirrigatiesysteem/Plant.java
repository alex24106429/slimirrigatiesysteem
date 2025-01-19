package org.teamhydro.slimirrigatiesysteem;

import org.json.JSONObject;

public class Plant {
    private int plantId;
    private String name;
    private String plantType;
    private boolean useDays;
    private int totalDelayMs;
    private int currentDelay;
    private int delay;
    private int outputML;
    private int minimumMoistureLevel;
    private double currentMoistureLevel;

    // Constructor
    public Plant(String name, String plantType, boolean useDays, int delay, int outputML, int minimumMoistureLevel, int currentMoistureLevel) {
        this.plantId = MainApplication.getRandomInt(0, Integer.MAX_VALUE - 1);
        this.name = name;
        this.plantType = plantType; // "african_violet", "hedgehog_cactus", "monstera", "pothos", "zz_plant", "aloe_vera", "jasmine", "orchid", "rosemary", "english_ivy", "lavender", "peace_lily", "snake_plant", "fiddle_leaf_fig", "mexican_fence_post_cactus", "philodendron", "spider_plant"
        this.useDays = useDays;
        this.delay = delay;
        this.outputML = outputML;
        this.minimumMoistureLevel = minimumMoistureLevel;
        this.currentMoistureLevel = currentMoistureLevel;
    }

    public void refreshFromArduino() {
        MainApplication.sendDataToArduino("fetch");
        String response = MainApplication.receiveDataFromArduino();
        assert response != null;

        // {"delayTime":"1","shouldUseDays":"false","needsWater":"false","totalDelayMs":"0","currentDelay":"0","moistureLevel":"0","status":"Fetching latest data"}
        JSONObject jsonResponse = new JSONObject(response);
        this.delay = jsonResponse.getInt("delayTime");
        this.useDays = jsonResponse.getBoolean("shouldUseDays");
        this.currentMoistureLevel = jsonResponse.getDouble("moistureLevel");
        this.totalDelayMs = jsonResponse.getInt("totalDelayMs");
        this.currentDelay = jsonResponse.getInt("currentDelay");
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
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
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
}
