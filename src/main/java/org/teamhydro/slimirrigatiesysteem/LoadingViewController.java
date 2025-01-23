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

    @FXML
    public void initialize() {
        attemptConnection();
    }

    private void updateStatus(String status, String task) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            taskLabel.setText(task);
        });
    }

    private void attemptConnection() {
        // Reset UI state
        progressIndicator.setVisible(true);
        buttonContainer.setVisible(false);
        updateStatus("Verbinden met Arduino device", "Zoeken naar beschikbare poorten...");

        // Start connection attempt in background thread
        Thread connectionThread = new Thread((Runnable) () -> {
            boolean connected = false;
            int attempts = 0;
            final int MAX_ATTEMPTS = 10;

            while (attempts < MAX_ATTEMPTS && !connected) {
                attempts++;
                final int currentAttempt = attempts;
                
                // Update status for each attempt phase
                updateStatus(
                    String.format("Verbinden met Arduino device (Poging %d/%d)", currentAttempt, MAX_ATTEMPTS),
                    "Initialiseren van verbinding..."
                );
                
                // Small delay to show initial status
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                updateStatus(
                    String.format("Verbinden met Arduino device (Poging %d/%d)", currentAttempt, MAX_ATTEMPTS),
                    "Seriële verbinding opstellen..."
                );
                
                if (MainApplication.getArduinoSerialConnection() == null) {
                    updateStatus(
                        String.format("Verbinden met Arduino device (Poging %d/%d)", currentAttempt, MAX_ATTEMPTS),
                        "Geen Arduino gevonden, opnieuw proberen..."
                    );
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }

                updateStatus(
                    String.format("Verbinden met Arduino device (Poging %d/%d)", currentAttempt, MAX_ATTEMPTS),
                    "Testen van communicatie..."
                );
                
                connected = MainApplication.testArduinoConnectivity();
                
                if (connected) {
                    updateStatus("Verbinding succesvol", "Applicatie wordt gestart...");
                    try {
                        Thread.sleep(1000); // Show success message briefly
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Connection successful, switch to login view
                    Platform.runLater(() -> {
                        try {
                            MainApplication.switchView("login-view.fxml");
                        } catch (Exception e) {
							System.out.println(e);
                            showError("Starten mislukt", "Er is een fout opgetreden bij het laden van het inlogscherm.");
                        }
                    });
                    return;
                } else {
                    updateStatus(
                        String.format("Verbinden met Arduino device (Poging %d/%d)", currentAttempt, MAX_ATTEMPTS),
                        "Geen reactie ontvangen, opnieuw proberen..."
                    );
                }

                try {
                    Thread.sleep(1000); // Wait before next attempt
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // If we get here, connection failed
            Platform.runLater(this::showError);
        });

        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @FXML
    private void handleRetry() {
        attemptConnection();
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void showError() {
        showError("Verbinding mislukt", 
                 "Controleer of de Arduino is aangesloten en of de juiste poort wordt gebruikt.");
    }

    private void showError(String status, String message) {
        statusLabel.setText(status);
        taskLabel.setText(message);
        progressIndicator.setVisible(false);
        buttonContainer.setVisible(true);
    }
} 