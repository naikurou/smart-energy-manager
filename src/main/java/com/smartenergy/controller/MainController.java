package com.smartenergy.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application.
 * Gère la navigation entre les différentes vues en remplaçant
 * le contenu central du BorderPane principal.
 *
 * <p>Ce contrôleur correspond à la vue MainView.fxml qui contient
 * la barre latérale de navigation et le panneau de contenu central.</p>
 */
public class MainController implements Initializable {

    /** Panneau principal de l'application */
    @FXML private BorderPane borderPaneMain;

    /** Boutons de navigation dans la barre latérale */
    @FXML private Button btnDashboard;
    @FXML private Button btnBatiments;
    @FXML private Button btnConsommations;
    @FXML private Button btnGraphiques;
    @FXML private Button btnAnalyse;

    /** Référence au bouton actuellement actif */
    private Button boutonActif;

    /**
     * Initialisation — affiche le tableau de bord par défaut.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Affichage du tableau de bord au démarrage
        naviguerVersDashboard();
    }

    /**
     * Navigue vers le tableau de bord principal.
     */
    @FXML
    public void naviguerVersDashboard() {
        chargerVue("/fxml/DashboardView.fxml");
        setActif(btnDashboard);
    }

    /**
     * Navigue vers la vue de gestion des bâtiments.
     */
    @FXML
    public void naviguerVersBatiments() {
        chargerVue("/fxml/BatimentsView.fxml");
        setActif(btnBatiments);
    }

    /**
     * Navigue vers la vue de saisie des consommations.
     */
    @FXML
    public void naviguerVersConsommations() {
        chargerVue("/fxml/ConsommationView.fxml");
        setActif(btnConsommations);
    }

    /**
     * Navigue vers la vue des graphiques.
     */
    @FXML
    public void naviguerVersGraphiques() {
        chargerVue("/fxml/ChartsView.fxml");
        setActif(btnGraphiques);
    }

    /**
     * Navigue vers la vue d'analyse.
     */
    @FXML
    public void naviguerVersAnalyse() {
        chargerVue("/fxml/AnalyseView.fxml");
        setActif(btnAnalyse);
    }

    /**
     * Charge une vue FXML et l'affiche dans le panneau central.
     *
     * @param cheminFxml Le chemin du fichier FXML (depuis le classpath resources)
     */
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

    /**
     * Marque un bouton de navigation comme actif (style CSS "active").
     *
     * @param bouton Le bouton à marquer comme actif
     */
    private void setActif(Button bouton) {
        // Retirer le style actif du bouton précédent
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("nav-btn-actif");
        }
        // Appliquer le style actif au nouveau bouton
        if (bouton != null) {
            bouton.getStyleClass().add("nav-btn-actif");
        }
        boutonActif = bouton;
    }
}
