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

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 400);
        stage.setTitle("Inloggen - Slim Irrigatie Systeem");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
    }

    public static void main(String[] args) {
        launch();
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