package org.teamhydro.slimirrigatiesysteem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CreatePlantController implements Initializable {

    @FXML
    private Label titleText;

    @FXML
    private MenuItem dagenMenuItem;

    @FXML
    private SplitMenuButton hourDay;

    @FXML
    private TextField minimumWaterField;

    @FXML
    private TextField nameField;

    @FXML
    private ChoiceBox<String> plantTypeChoiceBox;

    @FXML
    private TextField timeTextField;

    @FXML
    private MenuItem urenMenuItem;

    @FXML
    private TextField waterOutputField;

    // private boolean isValidAmount(String str) {
    //     if (str == null || str.isEmpty()) {
    //         return false;
    //     }
    //     try {
    //         int integer = Integer.parseInt(str);
    //         return integer > 0 && integer <= 365;
    //     } catch (NumberFormatException e) {
    //         return false;
    //     }
    // }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate the plantTypeChoiceBox with the specified plant types
        plantTypeChoiceBox.getItems().addAll(
            "african_violet",
            "hedgehog_cactus",
            "monstera",
            "pothos",
            "zz_plant",
            "aloe_vera",
            "jasmine",
            "orchid",
            "rosemary",
            "english_ivy",
            "lavender",
            "peace_lily",
            "snake_plant",
            "fiddle_leaf_fig",
            "mexican_fence_post_cactus",
            "philodendron",
            "spider_plant"
        );

        // Optionally, set a default value
        plantTypeChoiceBox.setValue("african_violet");
    }

    @FXML
    void handleDagenMenuItemAction(ActionEvent event) {
        hourDay.setText("Dagen");
    }

    @FXML
    void handleUrenMenuItemAction(ActionEvent event) {
        hourDay.setText("Uren");
    }

    @FXML
    void returnToPlantView() throws IOException {
        if(MainApplication.plants.length > 0) {
            MainApplication.switchView("plant-view.fxml");
        } else {
            MainApplication.switchView("login-view.fxml");
        }
    }

    private Plant originalPlant;

    public void loadPlantData(String plantName) {
        titleText.setText("Plant Bewerken");
        MainApplication.globalStage.setTitle("Plant Bewerken - Slim Irrigatie Systeem");
        
        originalPlant = MainApplication.getPlantByName(plantName);
        if (originalPlant != null) {
            nameField.setText(originalPlant.getName());
            plantTypeChoiceBox.setValue(originalPlant.getPlantType());
            hourDay.setText(originalPlant.isUseDays() ? "Dagen" : "Uren");
            timeTextField.setText(String.valueOf(originalPlant.getDelay()));
            waterOutputField.setText(String.valueOf(originalPlant.getOutputML()));
            minimumWaterField.setText(String.valueOf(originalPlant.getMinimumMoistureLevel()));
        }
    }

    @FXML
    void savePlant(ActionEvent event) throws IOException {
        try {
            // Gather input values from the UI fields
            String name = nameField.getText();
            String plantType = plantTypeChoiceBox.getValue();
            boolean useDays = hourDay.getText().equals("Dagen");
            int delay = Integer.parseInt(timeTextField.getText());
            int outputML = Integer.parseInt(waterOutputField.getText());
            int minimumMoistureLevel = Integer.parseInt(minimumWaterField.getText());
            int currentMoistureLevel = 768; // Default value, can be adjusted as needed

            // Validate input values
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Plant name cannot be empty.");
            }
            if (delay < 0) {
                throw new IllegalArgumentException("Delay time cannot be negative.");
            }
            if (outputML < 0) {
                throw new IllegalArgumentException("Water output cannot be negative.");
            }
            if (minimumMoistureLevel < 0) {
                throw new IllegalArgumentException("Minimum moisture level cannot be negative.");
            }

            // Check for duplicate plant names
            for (Plant plant : MainApplication.plants) {
                if (plant.getName().equalsIgnoreCase(name) && (originalPlant == null || !originalPlant.getName().equals(name))) {
                    throw new IllegalArgumentException("Een plant met deze naam bestaat al.");
                }
            }


            // Create a new Plant object
            Plant newPlant = new Plant(name, plantType, useDays, delay, outputML, minimumMoistureLevel, currentMoistureLevel);

            // Determine if we are updating an existing plant or creating a new one
            boolean success;
            if (originalPlant != null) {
                success = PlantRepository.updatePlant(newPlant, originalPlant.getName());
            } else {
                success = PlantRepository.addPlant(newPlant);
            }

            // Show a success or error message based on the result
            if (success) {
                // Refresh the plants list
                PlantRepository.refreshPlants();
                returnToPlantView();
            } else {
                System.out.println("Error saving plant.");

                // Show an error alert to the user
                MainApplication.showAlert(AlertType.ERROR, "Error", "Er is een fout opgetreden bij het opslaan van de plant.");
            }
        } catch (NumberFormatException e) {
            // Handle invalid number format in input fields
            MainApplication.showAlert(AlertType.ERROR, "Input Error", "Voer geldige getallen in voor delay, water output, en minimum moisture level.");
        } catch (IllegalArgumentException e) {
            // Handle invalid input values
            MainApplication.showAlert(AlertType.ERROR, "Input Error", e.getMessage());
        } catch (Exception e) {
            // Handle any other unexpected errors
            MainApplication.showAlert(AlertType.ERROR, "Error", "Er is een onverwachte fout opgetreden: " + e.getMessage());
        }
    }
}