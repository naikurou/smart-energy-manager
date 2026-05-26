package com.smartenergy.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private BorderPane borderPaneMain;
    @FXML private Button btnDashboard;
    @FXML private Button btnBatiments;
    @FXML private Button btnConsommations;
    @FXML private Button btnGraphiques;
    @FXML private Button btnAnalyse;

    private Button boutonActif;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        naviguerVersDashboard();
    }

    @FXML
    public void naviguerVersDashboard() {
        chargerVue("/fxml/DashboardView.fxml");
        setActif(btnDashboard);
    }

    @FXML
    public void naviguerVersBatiments() {
        chargerVue("/fxml/BatimentsView.fxml");
        setActif(btnBatiments);
    }

    @FXML
    public void naviguerVersConsommations() {
        chargerVue("/fxml/ConsommationView.fxml");
        setActif(btnConsommations);
    }

    @FXML
    public void naviguerVersGraphiques() {
        chargerVue("/fxml/ChartsView.fxml");
        setActif(btnGraphiques);
    }

    @FXML
    public void naviguerVersAnalyse() {
        chargerVue("/fxml/AnalyseView.fxml");
        setActif(btnAnalyse);
    }

    // On charge le FXML et on l'injecte au centre du BorderPane
    private void chargerVue(String cheminFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(cheminFxml));
            Parent vue = loader.load();
            borderPaneMain.setCenter(vue);
        } catch (IOException e) {
            System.err.println("Impossible de charger la vue : " + cheminFxml);
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    // Gestion du style CSS pour le bouton actif dans la nav
    private void setActif(Button bouton) {
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("nav-btn-actif");
        }
        if (bouton != null) {
            bouton.getStyleClass().add("nav-btn-actif");
        }
        boutonActif = bouton;
    }
}
