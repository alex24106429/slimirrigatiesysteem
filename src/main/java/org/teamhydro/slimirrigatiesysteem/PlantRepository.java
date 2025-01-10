package org.teamhydro.slimirrigatiesysteem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlantRepository {

    public static boolean addPlant(Plant plant) {
        // Add the plant to the database
        String insertPlantQuery = """
                    INSERT INTO plants (Name)
                    VALUES (?)
                """;
    
        String insertConfigQuery = """
                    INSERT INTO plant_configs (PlantId, PlantType, UseDays, Delay, OutputML, MinimumMoistureLevel, CurrentMoistureLevel)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
    
        try (PreparedStatement plantStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(insertPlantQuery)) {
            plantStatement.setString(1, plant.getName());
            plantStatement.executeUpdate();
    
            // Retrieve the last inserted PlantId
            try (PreparedStatement idStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement("SELECT last_insert_rowid()")) {
                try (ResultSet generatedKeys = idStatement.executeQuery()) {
                    if (generatedKeys.next()) {
                        int plantId = generatedKeys.getInt(1);
    
                        // Insert the plant configuration
                        try (PreparedStatement configStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(insertConfigQuery)) {
                            configStatement.setInt(1, plantId);
                            configStatement.setString(2, plant.getPlantType());
                            configStatement.setBoolean(3, plant.isUseDays());
                            configStatement.setInt(4, plant.getDelay());
                            configStatement.setInt(5, plant.getOutputML());
                            configStatement.setInt(6, plant.getMinimumMoistureLevel());
                            configStatement.setInt(7, plant.getCurrentMoistureLevel());
    
                            configStatement.executeUpdate();
                        }
                    } else {
                        throw new Exception("Failed to retrieve generated PlantId.");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    
        return true;
    }

    public static List<Plant> getAllPlants() {
        String query = """
            SELECT p.PlantId, p.Name,\s
                   pc.plantType, pc.UseDays, pc.Delay, pc.OutputML, pc.MinimumMoistureLevel, pc.CurrentMoistureLevel\s
            FROM plants p
            JOIN plant_configs pc ON p.PlantId = pc.PlantId
       \s""";

        List<Plant> plants = new ArrayList<>();

        try (PreparedStatement statement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // int plantId = resultSet.getInt("PlantId");
                    String name = resultSet.getString("Name");
                    String plantType = resultSet.getString("plantType");
                    boolean useDays = resultSet.getBoolean("UseDays");
                    int delay = resultSet.getInt("Delay");
                    int outputML = resultSet.getInt("OutputML");
                    int minimumMoistureLevel = resultSet.getInt("MinimumMoistureLevel");
                    int currentMoistureLevel = resultSet.getInt("currentMoistureLevel");

                    Plant plant = new Plant(name, plantType, useDays, delay, outputML, minimumMoistureLevel, currentMoistureLevel);
                    plants.add(plant);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            return plants;
        }

        System.out.println(plants);
        return plants;
    }

    public static boolean updatePlant(Plant plant, String originalName) {
        // Update the plant in the database
        String updatePlantQuery = """
            UPDATE plants
            SET Name = ?
            WHERE Name = ?
        """;
    
        String updateConfigQuery = """
            UPDATE plant_configs
            SET PlantType = ?, UseDays = ?, Delay = ?, OutputML = ?, MinimumMoistureLevel = ?
            WHERE PlantId = (SELECT PlantId FROM plants WHERE Name = ?)
        """;
    
        try (PreparedStatement plantStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(updatePlantQuery);
             PreparedStatement configStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(updateConfigQuery)) {
    
            // Update the plant name
            plantStatement.setString(1, plant.getName());
            plantStatement.setString(2, originalName);
            plantStatement.executeUpdate();
    
            // Update the plant configuration
            configStatement.setString(1, plant.getPlantType());
            configStatement.setBoolean(2, plant.isUseDays());
            configStatement.setInt(3, plant.getDelay());
            configStatement.setInt(4, plant.getOutputML());
            configStatement.setInt(5, plant.getMinimumMoistureLevel());
            configStatement.setString(6, plant.getName());
    
            configStatement.executeUpdate();
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    
        return true;
    }

    public static boolean deletePlant(String plantName) {
        // First, delete the plant configuration
        String deleteConfigQuery = """
            DELETE FROM plant_configs
            WHERE PlantId = (SELECT PlantId FROM plants WHERE Name = ?)
        """;
    
        // Then, delete the plant
        String deletePlantQuery = """
            DELETE FROM plants
            WHERE Name = ?
        """;
    
        try (PreparedStatement configStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(deleteConfigQuery);
             PreparedStatement plantStatement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(deletePlantQuery)) {
    
            // Delete the plant configuration
            configStatement.setString(1, plantName);
            configStatement.executeUpdate();
    
            // Delete the plant
            plantStatement.setString(1, plantName);
            plantStatement.executeUpdate();
    
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    
        return true;
    }

    public static void refreshPlants() {
        MainApplication.plants = getAllPlants().toArray(new Plant[0]);
    }
}