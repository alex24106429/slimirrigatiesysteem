package org.teamhydro.slimirrigatiesysteem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlantRepository {

    public static boolean addPlant(Plant plant) {
        // Add the plant to the database
        String query = """
                    INSERT INTO plants (Name, ShortName, Location)
                    VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(query)) {
            statement.setString(1, plant.getName());
            statement.setString(2, plant.getPlantType());
            statement.setString(3, "Living Room");

            statement.executeUpdate();
        } catch (Exception e) {
            // TODO: Handle exception
            return false;
        }

        return true;
    }

    public static List<Plant> getAllPlants() {
        String query = """
            SELECT p.Name, p.ShortName, p.Location,\s
                   pc.UseDays, pc.Delay, pc.OutputML, pc.MinimumMoistureLevel\s
            FROM plants p
            JOIN plant_configs pc ON p.PlantId = pc.PlantId
       \s""";

        List<Plant> plants = new ArrayList<>();

        try (PreparedStatement statement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("Name");
                    String plantType = resultSet.getString("ShortName"); // Assuming `ShortName` acts as `plantType`.
                    boolean useDays = resultSet.getBoolean("UseDays");
                    int delay = resultSet.getInt("Delay");
                    int outputML = resultSet.getInt("OutputML");
                    int minimumMoistureLevel = resultSet.getInt("MinimumMoistureLevel");
                    int currentMoistureLevel = 0; // Set default or fetch if available.

                    Plant plant = new Plant(name, plantType, useDays, delay, outputML, minimumMoistureLevel, currentMoistureLevel);
                    plants.add(plant);
                }
            }
        } catch (Exception e) {
            // TODO: Handle exception
            // For now return a list with dummy data
            plants.add(new Plant(
                    "African Violet",
                    "african_violet",
                    true,
                    1,
                    105,
                    50,
                    0
            ));
            plants.add(new Plant(
                    "Hedgehog Cactus",
                    "hedgehog_cactus",
                    true,
                    1,
                    105,
                    50,
                    0
            ));
            plants.add(new Plant(
                    "Monstera",
                    "monstera",
                    true,
                    1,
                    105,
                    50,
                    0
            ));
        }

        System.out.println(plants);
        return plants;
    }

    public static boolean updatePlant(Plant plant) {
        // Update the plant in the database
        String query = """
            UPDATE plants p
            JOIN plant_configs pc ON p.PlantId = pc.PlantId
            SET pc.UseDays = ?, pc.Delay = ?, pc.OutputML = ?, pc.MinimumMoistureLevel = ?
            WHERE p.Name = ?
        """;

        try (PreparedStatement statement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(query)) {
            statement.setBoolean(1, plant.isUseDays());
            statement.setInt(2, plant.getDelay());
            statement.setInt(3, plant.getOutputML());
            statement.setInt(4, plant.getMinimumMoistureLevel());
            statement.setString(5, plant.getName());

            statement.executeUpdate();
        } catch (Exception e) {
            // TODO: Handle exception
            return false;
        }

        return true;
    }

    public static boolean deletePlant(Plant plant) {
        // Delete the plant (and its configuration) from the database
        String query = """
            DELETE p, pc
            FROM plants p
            JOIN plant_configs pc ON p.PlantId = pc.PlantId
            WHERE p.Name = ?
        """;

        try (PreparedStatement statement = Objects.requireNonNull(MainApplication.getConnection()).prepareStatement(query)) {
            statement.setString(1, plant.getName());

            statement.executeUpdate();
        } catch (Exception e) {
            // TODO: Handle exception
            return false;
        }

        return true;
    }
}