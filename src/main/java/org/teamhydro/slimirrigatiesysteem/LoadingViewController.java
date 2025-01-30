package org.teamhydro.slimirrigatiesysteem;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

public class LoadingViewController {
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label taskLabel;
    
    @FXML
    private HBox buttonContainer;

    private static LoadingViewController instance;
    private Runnable onRetry;

    @FXML
    public void initialize() {
        instance = this;
        buttonContainer.setVisible(false);
        progressIndicator.setVisible(true);
        updateStatus("Applicatie wordt geladen", "Even geduld...");
    }

    public static LoadingViewController getInstance() {
        return instance;
    }

    public void startOperation(String initialStatus, String initialTask, Runnable retryAction) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(true);
            buttonContainer.setVisible(false);
            updateStatus(initialStatus, initialTask);
            this.onRetry = retryAction;
        });
    }

    public void updateProgress(String status, String task) {
        updateStatus(status, task);
    }

    public void operationSucceeded(String nextView) {
        Platform.runLater(() -> {
            try {
                MainApplication.switchView(nextView);
            } catch (Exception e) {
                showError("Laden mislukt", "Er is een fout opgetreden bij het laden van het volgende scherm.");
            }
        });
    }

    public void operationFailed(String errorStatus, String errorMessage) {
        showError(errorStatus, errorMessage);
    }

    private void updateStatus(String status, String task) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            taskLabel.setText(task);
        });
    }

    @FXML
    private void handleRetry() {
        if (onRetry != null) {
            progressIndicator.setVisible(true);
            buttonContainer.setVisible(false);
            onRetry.run();
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void showError(String status, String message) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            taskLabel.setText(message);
            progressIndicator.setVisible(false);
            buttonContainer.setVisible(true);
        });
    }
} 