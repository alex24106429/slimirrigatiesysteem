module org.teamhydro.slimirrigatiesysteem {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires jdk.compiler;
    requires transitive java.sql;
    requires javafx.graphics;
    requires transitive com.fazecast.jSerialComm;
    requires java.net.http;
    requires java.prefs;
    requires transitive org.json;


    opens org.teamhydro.slimirrigatiesysteem to javafx.fxml;
    exports org.teamhydro.slimirrigatiesysteem;
}