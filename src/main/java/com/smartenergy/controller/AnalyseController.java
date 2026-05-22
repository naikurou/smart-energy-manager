package com.smartenergy.controller;

import com.smartenergy.model.Alerte;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import com.smartenergy.service.BatimentService;
import com.smartenergy.service.EnergyService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la vue d'analyse avancée.
 * Affiche les pics de consommation, les anomalies détectées,
 * les indicateurs analytiques et le résumé par type d'énergie.
 */
public class AnalyseController implements Initializable {

    @FXML private ComboBox<Batiment> comboBatimentAnalyse;

    // KPI labels
    @FXML private Label labelAnalyseEnergieDom;
    @FXML private Label labelAnalyseTendance;
    @FXML private Label labelAnalyseFacture;

    // Table des pics
    @FXML private TableView<EnergyRecord> tablePics;
    @FXML private TableColumn<EnergyRecord, String> colPicDate;
    @FXML private TableColumn<EnergyRecord, String> colPicType;
    @FXML private TableColumn<EnergyRecord, Double> colPicQuantite;
    @FXML private TableColumn<EnergyRecord, Double> colPicCout;

    // Liste des anomalies
    @FXML private ListView<String> listAnomalies;

    // Table résumé par énergie
    @FXML private TableView<Map<String, Object>> tableResume;
    @FXML private TableColumn<Map<String, Object>, String> colResumeType;
    @FXML private TableColumn<Map<String, Object>, Double> colResumeTotal;
    @FXML private TableColumn<Map<String, Object>, Double> colResumeCout;
    @FXML private TableColumn<Map<String, Object>, Double> colResumePart;

    private final BatimentService batimentService = new BatimentService();
    private final EnergyService energyService = new EnergyService();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurerColonnes();
        chargerBatiments();
    }

    /**
     * Configure les colonnes des tableaux d'analyse.
     */
    private void configurerColonnes() {
        // Colonnes de la table des pics
        colPicDate.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDateHeure() != null
                ? data.getValue().getDateHeure().format(FMT) : "—"));
        colPicType.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getTypeEnergie() != null
                ? data.getValue().getTypeEnergie().getLibelle() : "—"));
        colPicQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colPicCout.setCellValueFactory(new PropertyValueFactory<>("coutEstime"));

        // Colonnes du résumé par énergie
        colResumeType.setCellValueFactory(data ->
            new SimpleStringProperty((String) data.getValue().get("type")));
        colResumeTotal.setCellValueFactory(data ->
            new SimpleDoubleProperty((Double) data.getValue().get("total")).asObject());
        colResumeCout.setCellValueFactory(data ->
            new SimpleDoubleProperty((Double) data.getValue().get("cout")).asObject());
        colResumePart.setCellValueFactory(data ->
            new SimpleDoubleProperty((Double) data.getValue().get("part")).asObject());

        // Formatage de la colonne part (%)
        colResumePart.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f %%", item));
            }
        });
    }

    /**
     * Charge la liste des bâtiments dans la ComboBox.
     */
    private void chargerBatiments() {
        List<Batiment> batiments = batimentService.trouverTous();
        comboBatimentAnalyse.setItems(FXCollections.observableArrayList(batiments));
        if (!batiments.isEmpty()) {
            comboBatimentAnalyse.getSelectionModel().selectFirst();
        }
    }

    /**
     * Lance l'analyse complète pour le bâtiment sélectionné.
     */
    @FXML
    public void lancerAnalyse() {
        Batiment batiment = comboBatimentAnalyse.getValue();
        if (batiment == null) return;

        afficherKPI(batiment);
        afficherPics(batiment);
        afficherAnomalies(batiment);
        afficherResumeEnergie(batiment);
    }

    /**
     * Affiche les indicateurs clés d'analyse.
     *
     * @param batiment Le bâtiment analysé
     */
    private void afficherKPI(Batiment batiment) {
        // Énergie dominante
        TypeEnergie dominante = energyService.getEnergieDominante(batiment.getId());
        labelAnalyseEnergieDom.setText(dominante != null ? dominante.getLibelle() : "Aucune donnée");

        // Tendance
        double tendance = energyService.calculerTendance(batiment.getId());
        String symbole = tendance >= 0 ? "▲ +" : "▼ ";
        labelAnalyseTendance.setText(symbole + df.format(Math.abs(tendance)) + "%");
        labelAnalyseTendance.setStyle(tendance > 10
            ? "-fx-text-fill: #f44336;" : "-fx-text-fill: #4caf50;");

        // Facture estimée
        double facture = energyService.estimerFactureMensuelle(batiment.getId());
        labelAnalyseFacture.setText(df.format(facture) + " €");
    }

    /**
     * Affiche les pics de consommation dans le tableau dédié.
     *
     * @param batiment Le bâtiment analysé
     */
    private void afficherPics(Batiment batiment) {
        List<EnergyRecord> pics = energyService.detecterPics(batiment.getId());
        tablePics.setItems(FXCollections.observableArrayList(pics));
    }

    /**
     * Affiche les anomalies détectées dans la ListView.
     *
     * @param batiment Le bâtiment analysé
     */
    private void afficherAnomalies(Batiment batiment) {
        listAnomalies.getItems().clear();
        List<Alerte> anomalies = energyService.detecterAnomalies(List.of(batiment));

        if (anomalies.isEmpty()) {
            listAnomalies.getItems().add("✓ Aucune anomalie détectée — consommation stable.");
        } else {
            anomalies.forEach(a -> listAnomalies.getItems().add(a.getMessage()));
        }
    }

    /**
     * Affiche le résumé de consommation par type d'énergie dans le tableau.
     *
     * @param batiment Le bâtiment analysé
     */
    private void afficherResumeEnergie(Batiment batiment) {
        List<EnergyRecord> records = energyService.getRecordsBatiment(batiment.getId());

        // Totaux par énergie
        Map<TypeEnergie, Double> totauxQte = records.stream()
            .collect(Collectors.groupingBy(EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getQuantite)));

        Map<TypeEnergie, Double> totauxCout = records.stream()
            .collect(Collectors.groupingBy(EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getCoutEstime)));

        double coutTotal = totauxCout.values().stream().mapToDouble(d -> d).sum();

        List<Map<String, Object>> lignes = new ArrayList<>();
        for (TypeEnergie type : TypeEnergie.values()) {
            double qte = totauxQte.getOrDefault(type, 0.0);
            double cout = totauxCout.getOrDefault(type, 0.0);
            double part = coutTotal > 0 ? (cout / coutTotal) * 100 : 0;

            Map<String, Object> ligne = new HashMap<>();
            ligne.put("type", type.getLibelle());
            ligne.put("total", qte);
            ligne.put("cout", cout);
            ligne.put("part", part);
            lignes.add(ligne);
        }

        tableResume.setItems(FXCollections.observableArrayList(lignes));
    }
}
