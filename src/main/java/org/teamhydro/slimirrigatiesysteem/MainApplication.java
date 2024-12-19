package org.teamhydro.slimirrigatiesysteem;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Random;

public class MainApplication extends Application {

    public static void switchView(Stage stage, String fxmlName) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlName));
        Scene scene = new Scene(fxmlLoader.load(), 640, 400);

        String title = switch (fxmlName) {
            case "login-view.fxml" -> "Inloggen";
            case "plant-view.fxml" -> "Plant";
            case "passwordrecovery-view.fxml" -> "Wachtwoord Herstellen";
            case "userinfo-view.fxml" -> "Uw Gegevens";
            default -> "Onbekend";
        };
        stage.setTitle(title + " - Slim Irrigatie Systeem");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void start(Stage stage) throws IOException {
        switchView(stage, "login-view.fxml");
        stage.setResizable(false);
    }

    public static int getRandomInt(int min, int max) {
        // Create a Random object
        Random rand = new Random();

        // Generate a random integer between min and max (inclusive)
        return rand.nextInt(max - min + 1) + min;
    }

    static Plant[] plants = new Plant[100];

    public static Plant getPlantByName(String name) {
        for(Plant plant: plants) {
            if (plant.getName().equals(name)) return plant;
        }

        return null;
    }

    public static void main(String[] args) {
        for (int i = 0; i < plants.length; i++) {
            plants[i] = new Plant("Plant " + i, "lavender", Math.random() > 0.5, getRandomInt(0, 100), getRandomInt(50, 300), getRandomInt(0, 1023), getRandomInt(0, 1023));
        }
        launch();
    }

    private static String name = "Naam";

    public static String getName() {
        return name;
    }

    public static void setName(String newName) {
        name = newName;
    }

    private static String email = "test@example.com";

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String newEmail) {
        email = newEmail;
    }

    public static String address = "Sesamstraat";

    public static String getAddress() {
        return address;
    }

    public static void setAddress(String newAddress) {
        address = newAddress;
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
}