module org.teamhydro.slimirrigatiesysteem {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;
    requires java.sql;


    opens org.teamhydro.slimirrigatiesysteem to javafx.fxml;
    exports org.teamhydro.slimirrigatiesysteem;
}