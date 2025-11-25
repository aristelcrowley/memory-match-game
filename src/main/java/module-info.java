module com.aristel {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics; 
    opens com.aristel.controller to javafx.fxml;
    exports com.aristel;
}