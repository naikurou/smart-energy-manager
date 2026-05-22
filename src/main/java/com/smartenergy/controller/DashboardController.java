package com.smartenergy.controller;

import com.smartenergy.model.Alerte;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import com.smartenergy.service.BatimentService;
import com.smartenergy.service.EnergyService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur du tableau de bord principal.
 * Affiche les KPI globaux, les alertes actives et un résumé des consommations.
 *
 * <p>Ce contrôleur est lié au fichier FXML DashboardView.fxml.
 * Il actualise automatiquement les données au chargement.</p>
 */
public class DashboardController implements Initializable {

    // ===== Indicateurs de consommation =====
    @FXML private Label labelConsommationJour;
    @FXML private Label labelConsommationMois;
    @FXML private Label labelConsommationAnnee;
    @FXML private Label labelCoutMois;
    @FXML private Label labelNombreBatiments;
    @FXML private Label labelBatimentPlusConsommateur;

    // ===== Alertes =====
    @FXML private ListView<String> listViewAlertes;
    @FXML private Label labelNombreAlertes;

    // ===== Barres de progression KPI =====
    @FXML private ProgressBar progressBarElec;
    @FXML private ProgressBar progressBarEau;
    @FXML private ProgressBar progressBarGaz;

    // ===== Labels des KPI énergétiques =====
    @FXML private Label labelKpiElec;
    @FXML private Label labelKpiEau;
    @FXML private Label labelKpiGaz;

    // ===== Panneau des dernières activités =====
    @FXML private VBox vboxDernieresMesures;

    /** Service de gestion des bâtiments */
    private final BatimentService batimentService = new BatimentService();

    /** Service de gestion des consommations */
    private final EnergyService energyService = new EnergyService();

    /** Formateur pour les nombres décimaux */
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * Méthode d'initialisation appelée automatiquement par JavaFX
     * après le chargement du fichier FXML.
     *
     * @param url            L'URL de la ressource FXML
     * @param resourceBundle Les ressources de localisation
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        actualiserTableauDeBord();
    }

    /**
     * Actualise toutes les données affichées dans le tableau de bord.
     * Recharge les données depuis les services métier.
     */
    public void actualiserTableauDeBord() {
        chargerIndicateurs();
        chargerAlertes();
        chargerKpiEnergie();
    }

    /**
     * Charge et affiche les indicateurs clés de performance (KPI).
     */
    private void chargerIndicateurs() {
        // Consommation temporelle
        double conso_jour = energyService.getConsommationJour();
        double conso_mois = energyService.getConsommationMois();
        double conso_annee = energyService.getConsommationAnnee();

        labelConsommationJour.setText(df.format(conso_jour) + " unités");
        labelConsommationMois.setText(df.format(conso_mois) + " unités");
        labelConsommationAnnee.setText(df.format(conso_annee) + " unités");

        // Nombre de bâtiments
        List<Batiment> batiments = batimentService.trouverTous();
        labelNombreBatiments.setText(String.valueOf(batiments.size()));

        // Coût mensuel estimé (somme de tous les bâtiments)
        double coutTotal = batiments.stream()
            .mapToDouble(b -> energyService.estimerFactureMensuelle(b.getId()))
            .sum();
        labelCoutMois.setText(df.format(coutTotal) + " €");

        // Bâtiment le plus consommateur
        if (!batiments.isEmpty()) {
            batiments.stream()
                .max((b1, b2) -> Double.compare(
                    energyService.estimerFactureMensuelle(b1.getId()),
                    energyService.estimerFactureMensuelle(b2.getId())
                ))
                .ifPresent(b -> labelBatimentPlusConsommateur.setText(b.getNom()));
        } else {
            labelBatimentPlusConsommateur.setText("Aucun bâtiment");
        }
    }

    /**
     * Détecte et affiche les alertes de consommation anormale.
     */
    private void chargerAlertes() {
        List<Batiment> batiments = batimentService.trouverTous();
        List<Alerte> alertes = energyService.detecterAnomalies(batiments);

        listViewAlertes.getItems().clear();

        if (alertes.isEmpty()) {
            listViewAlertes.getItems().add("✓ Aucune anomalie détectée — consommation normale");
        } else {
            for (Alerte alerte : alertes) {
                listViewAlertes.getItems().add(alerte.getMessage());
            }
        }

        labelNombreAlertes.setText(String.valueOf(alertes.size()));

        // Coloration des alertes selon le niveau
        labelNombreAlertes.setStyle(alertes.isEmpty()
            ? "-fx-text-fill: #4caf50;"   // Vert si pas d'alerte
            : "-fx-text-fill: #ff5722;"   // Rouge si alertes
        );
    }

    /**
     * Charge les KPI par type d'énergie avec des barres de progression.
     */
    private void chargerKpiEnergie() {
        double totalElec = 0, totalEau = 0, totalGaz = 0;

        List<Batiment> batiments = batimentService.trouverTous();
        for (Batiment b : batiments) {
            List<EnergyRecord> records = energyService.getRecordsBatiment(b.getId());
            for (EnergyRecord r : records) {
                switch (r.getTypeEnergie()) {
                    case ELECTRICITE -> totalElec += r.getQuantite();
                    case EAU         -> totalEau += r.getQuantite();
                    case GAZ         -> totalGaz += r.getQuantite();
                    default -> {} // Chauffage et climatisation ignorés ici
                }
            }
        }

        double max = Math.max(1, Math.max(totalElec, Math.max(totalEau, totalGaz)));

        if (progressBarElec != null) {
            progressBarElec.setProgress(totalElec / max);
            labelKpiElec.setText(df.format(totalElec) + " kWh");
        }
        if (progressBarEau != null) {
            progressBarEau.setProgress(totalEau / max);
            labelKpiEau.setText(df.format(totalEau) + " m³");
        }
        if (progressBarGaz != null) {
            progressBarGaz.setProgress(totalGaz / max);
            labelKpiGaz.setText(df.format(totalGaz) + " m³");
        }
    }

    /**
     * Ouvre la vue de gestion des bâtiments depuis le bouton du tableau de bord.
     */
    @FXML
    private void ouvrirGestionBatiments() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/BatimentsView.fxml"));
            Parent vue = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Gestion des Bâtiments");
            stage.setScene(new Scene(vue, 900, 650));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture gestion bâtiments : " + e.getMessage());
        }
    }

    /**
     * Ouvre la vue de saisie de consommation depuis le tableau de bord.
     */
    @FXML
    private void ouvrirSaisieConsommation() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ConsommationView.fxml"));
            Parent vue = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Saisie de Consommation");
            stage.setScene(new Scene(vue, 800, 600));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture saisie consommation : " + e.getMessage());
        }
    }

    /**
     * Ouvre la vue des graphiques et visualisations.
     */
    @FXML
    private void ouvrirVisualisations() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ChartsView.fxml"));
            Parent vue = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Visualisations");
            stage.setScene(new Scene(vue, 1000, 700));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture visualisations : " + e.getMessage());
        }
    }
}
