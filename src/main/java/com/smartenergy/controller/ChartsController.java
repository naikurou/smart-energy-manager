package com.smartenergy.controller;

import com.smartenergy.model.Batiment;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import com.smartenergy.service.BatimentService;
import com.smartenergy.service.EnergyService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour les visualisations graphiques des consommations énergétiques.
 * Génère plusieurs types de graphiques JavaFX Charts :
 * - Courbe temporelle de consommation
 * - Histogramme par type d'énergie
 * - Comparaison multi-bâtiments
 * - Répartition en camembert par énergie
 *
 * <p>Les graphiques sont recalculés dynamiquement selon le bâtiment
 * et la période sélectionnés par l'utilisateur.</p>
 */
public class ChartsController implements Initializable {

    // ===== Filtres =====
    @FXML private ComboBox<Batiment> comboBatiment;
    @FXML private ComboBox<String> comboPeriode;

    // ===== Graphiques =====
    @FXML private LineChart<String, Number> lineChartTemporel;
    @FXML private BarChart<String, Number> barChartParEnergie;
    @FXML private PieChart pieChartRepartition;
    @FXML private BarChart<String, Number> barChartComparaison;

    // ===== Analyse texte =====
    @FXML private Label labelAnalyse;
    @FXML private Label labelTendance;
    @FXML private Label labelEnergieDominante;
    @FXML private Label labelEstimationFacture;

    /** Services métier */
    private final BatimentService batimentService = new BatimentService();
    private final EnergyService energyService = new EnergyService();

    /** Format de date pour les axes */
    private static final DateTimeFormatter FMT_JOUR = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

    /**
     * Initialisation de la vue.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chargerBatiments();
        configurerComboPeriode();
        configurerEvenements();
    }

    /**
     * Charge la liste des bâtiments dans la ComboBox de sélection.
     */
    private void chargerBatiments() {
        List<Batiment> batiments = batimentService.trouverTous();
        comboBatiment.setItems(FXCollections.observableArrayList(batiments));
        if (!batiments.isEmpty()) {
            comboBatiment.getSelectionModel().selectFirst();
        }
    }

    /**
     * Configure les périodes disponibles pour l'analyse.
     */
    private void configurerComboPeriode() {
        comboPeriode.setItems(FXCollections.observableArrayList(
            "7 derniers jours",
            "30 derniers jours",
            "3 derniers mois",
            "6 derniers mois",
            "12 derniers mois"
        ));
        comboPeriode.getSelectionModel().select(1); // 30 jours par défaut
    }

    /**
     * Configure les événements pour actualiser les graphiques.
     */
    private void configurerEvenements() {
        comboBatiment.setOnAction(e -> actualiserGraphiques());
        comboPeriode.setOnAction(e -> actualiserGraphiques());
    }

    /**
     * Actualise tous les graphiques selon les filtres sélectionnés.
     */
    @FXML
    public void actualiserGraphiques() {
        Batiment batiment = comboBatiment.getValue();
        if (batiment == null) return;

        LocalDateTime[] periode = getPeriode();
        LocalDateTime debut = periode[0];
        LocalDateTime fin = periode[1];

        List<EnergyRecord> records = energyService.getRecordsPeriode(
            batiment.getId(), debut, fin);

        genererCourbeTemporelle(records, batiment.getNom());
        genererHistogrammeParEnergie(records);
        genererCamembert(records);
        genererComparaisonBatiments(debut, fin);
        actualiserAnalyse(batiment);
    }

    /**
     * Génère la courbe temporelle de consommation pour un bâtiment.
     * Une série par type d'énergie, valeurs agrégées par jour.
     *
     * @param records   Les enregistrements à afficher
     * @param nomBatiment Le nom du bâtiment (pour le titre)
     */
    private void genererCourbeTemporelle(List<EnergyRecord> records, String nomBatiment) {
        lineChartTemporel.getData().clear();
        lineChartTemporel.setTitle("Évolution de la consommation — " + nomBatiment);

        // Groupement par type d'énergie
        Map<TypeEnergie, List<EnergyRecord>> parType = records.stream()
            .collect(Collectors.groupingBy(EnergyRecord::getTypeEnergie));

        for (Map.Entry<TypeEnergie, List<EnergyRecord>> entree : parType.entrySet()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(entree.getKey().getLibelle());

            // Agrégation par jour
            Map<LocalDate, Double> parJour = new TreeMap<>();
            for (EnergyRecord r : entree.getValue()) {
                LocalDate jour = r.getDateHeure().toLocalDate();
                parJour.merge(jour, r.getQuantite(), Double::sum);
            }

            for (Map.Entry<LocalDate, Double> entry : parJour.entrySet()) {
                serie.getData().add(new XYChart.Data<>(
                    entry.getKey().format(FMT_JOUR),
                    Math.round(entry.getValue() * 100.0) / 100.0
                ));
            }

            lineChartTemporel.getData().add(serie);
        }
    }

    /**
     * Génère un histogramme de la consommation totale par type d'énergie.
     *
     * @param records Les enregistrements à analyser
     */
    private void genererHistogrammeParEnergie(List<EnergyRecord> records) {
        barChartParEnergie.getData().clear();
        barChartParEnergie.setTitle("Consommation par type d'énergie");

        XYChart.Series<String, Number> serieQuantite = new XYChart.Series<>();
        serieQuantite.setName("Quantité consommée");

        XYChart.Series<String, Number> serieCout = new XYChart.Series<>();
        serieCout.setName("Coût estimé (€)");

        // Totaux par type d'énergie
        Map<TypeEnergie, Double> totauxQuantite = records.stream()
            .collect(Collectors.groupingBy(
                EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getQuantite)
            ));

        Map<TypeEnergie, Double> totauxCout = records.stream()
            .collect(Collectors.groupingBy(
                EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getCoutEstime)
            ));

        for (TypeEnergie type : TypeEnergie.values()) {
            double qte = totauxQuantite.getOrDefault(type, 0.0);
            double cout = totauxCout.getOrDefault(type, 0.0);
            serieQuantite.getData().add(new XYChart.Data<>(type.getLibelle(), qte));
            serieCout.getData().add(new XYChart.Data<>(type.getLibelle(), cout));
        }

        barChartParEnergie.getData().addAll(serieQuantite, serieCout);
    }

    /**
     * Génère un diagramme camembert de la répartition par type d'énergie (en coût).
     *
     * @param records Les enregistrements à analyser
     */
    private void genererCamembert(List<EnergyRecord> records) {
        pieChartRepartition.getData().clear();
        pieChartRepartition.setTitle("Répartition des coûts par énergie");

        Map<TypeEnergie, Double> totauxCout = records.stream()
            .collect(Collectors.groupingBy(
                EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getCoutEstime)
            ));

        double totalGlobal = totauxCout.values().stream().mapToDouble(d -> d).sum();
        if (totalGlobal == 0) return;

        for (Map.Entry<TypeEnergie, Double> entree : totauxCout.entrySet()) {
            double pourcentage = (entree.getValue() / totalGlobal) * 100;
            PieChart.Data slice = new PieChart.Data(
                String.format("%s (%.1f%%)", entree.getKey().getLibelle(), pourcentage),
                entree.getValue()
            );
            pieChartRepartition.getData().add(slice);
        }
    }

    /**
     * Génère un histogramme comparatif entre tous les bâtiments.
     *
     * @param debut Date de début de la période
     * @param fin   Date de fin de la période
     */
    private void genererComparaisonBatiments(LocalDateTime debut, LocalDateTime fin) {
        barChartComparaison.getData().clear();
        barChartComparaison.setTitle("Comparaison des consommations — Tous bâtiments");

        List<Batiment> batiments = batimentService.trouverTous();

        // Une série par type d'énergie
        for (TypeEnergie type : TypeEnergie.values()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(type.getLibelle());

            for (Batiment b : batiments) {
                List<EnergyRecord> records = energyService.getRecordsPeriode(
                    b.getId(), debut, fin);

                double total = records.stream()
                    .filter(r -> r.getTypeEnergie() == type)
                    .mapToDouble(EnergyRecord::getQuantite)
                    .sum();

                // Tronquer le nom si trop long
                String nomBatiment = b.getNom().length() > 15
                    ? b.getNom().substring(0, 12) + "..."
                    : b.getNom();

                serie.getData().add(new XYChart.Data<>(nomBatiment, total));
            }

            barChartComparaison.getData().add(serie);
        }
    }

    /**
     * Met à jour les labels d'analyse textuelle pour le bâtiment sélectionné.
     *
     * @param batiment Le bâtiment à analyser
     */
    private void actualiserAnalyse(Batiment batiment) {
        // Énergie dominante
        TypeEnergie dominante = energyService.getEnergieDominante(batiment.getId());
        if (labelEnergieDominante != null) {
            labelEnergieDominante.setText(dominante != null
                ? dominante.getLibelle() : "Aucune donnée");
        }

        // Tendance mensuelle
        double tendance = energyService.calculerTendance(batiment.getId());
        if (labelTendance != null) {
            String symbole = tendance >= 0 ? "▲ +" : "▼ ";
            String couleur = tendance > 10 ? "#f44336" : (tendance < -5 ? "#4caf50" : "#ff9800");
            labelTendance.setText(symbole + String.format("%.1f%%", Math.abs(tendance)));
            labelTendance.setStyle("-fx-text-fill: " + couleur + ";");
        }

        // Estimation de facture mensuelle
        double facture = energyService.estimerFactureMensuelle(batiment.getId());
        if (labelEstimationFacture != null) {
            labelEstimationFacture.setText(String.format("%.2f €/mois", facture));
        }
    }

    /**
     * Retourne la période [debut, fin] correspondant au filtre sélectionné.
     *
     * @return Un tableau [début, fin] de LocalDateTime
     */
    private LocalDateTime[] getPeriode() {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = switch (comboPeriode.getValue()) {
            case "7 derniers jours"   -> fin.minusDays(7);
            case "3 derniers mois"    -> fin.minusMonths(3);
            case "6 derniers mois"    -> fin.minusMonths(6);
            case "12 derniers mois"   -> fin.minusMonths(12);
            default                   -> fin.minusDays(30); // 30 jours par défaut
        };
        return new LocalDateTime[]{debut, fin};
    }
}
