module org.teamhydro.slimirrigatiesysteem {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.teamhydro.slimirrigatiesysteem to javafx.fxml;
    exports org.teamhydro.slimirrigatiesysteem;
}