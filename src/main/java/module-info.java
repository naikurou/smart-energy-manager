module com.smartenergy {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.charts;
    requires java.sql;

    opens com.smartenergy to javafx.fxml;
    opens com.smartenergy.controller to javafx.fxml;
    opens com.smartenergy.model to javafx.base;

    exports com.smartenergy;
    exports com.smartenergy.model;
    exports com.smartenergy.service;
    exports com.smartenergy.dao;
    exports com.smartenergy.util;
    exports com.smartenergy.controller;
}
