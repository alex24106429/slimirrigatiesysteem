package org.teamhydro.slimirrigatiesysteem;

public class Plant {
    private int plantId;
    private String name;
    private boolean useDays;
    private int delay;
    private int outputML;
    private int minimumMoistureLevel;
    private int currentMoistureLevel;

    // Constructor
    public Plant(String name, boolean useDays, int delay, int outputML, int minimumMoistureLevel, int currentMoistureLevel) {
        this.plantId = MainApplication.getRandomInt(0, Integer.MAX_VALUE - 1);
        this.name = name;
        this.useDays = useDays;
        this.delay = delay;
        this.outputML = outputML;
        this.minimumMoistureLevel = minimumMoistureLevel;
        this.currentMoistureLevel = currentMoistureLevel;
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

    public int getCurrentMoistureLevel() {
        return currentMoistureLevel;
    }

    public void setCurrentMoistureLevel(int currentMoistureLevel) {
        this.currentMoistureLevel = currentMoistureLevel;
    }
}
