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

public class ChartsController implements Initializable {

    @FXML private ComboBox<Batiment> comboBatiment;
    @FXML private ComboBox<String> comboPeriode;

    @FXML private LineChart<String, Number> lineChartTemporel;
    @FXML private BarChart<String, Number> barChartParEnergie;
    @FXML private PieChart pieChartRepartition;
    @FXML private BarChart<String, Number> barChartComparaison;

    @FXML private Label labelAnalyse;
    @FXML private Label labelTendance;
    @FXML private Label labelEnergieDominante;
    @FXML private Label labelEstimationFacture;

    private final BatimentService batimentService = new BatimentService();
    private final EnergyService energyService = new EnergyService();

    private static final DateTimeFormatter FMT_JOUR = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chargerBatiments();
        configurerComboPeriode();
        configurerEvenements();
    }

    private void chargerBatiments() {
        List<Batiment> batiments = batimentService.trouverTous();
        comboBatiment.setItems(FXCollections.observableArrayList(batiments));
        if (!batiments.isEmpty()) {
            comboBatiment.getSelectionModel().selectFirst();
        }
    }

    private void configurerComboPeriode() {
        comboPeriode.setItems(FXCollections.observableArrayList(
            "7 derniers jours",
            "30 derniers jours",
            "3 derniers mois",
            "6 derniers mois",
            "12 derniers mois"
        ));
        comboPeriode.getSelectionModel().select(1);
    }

    private void configurerEvenements() {
        comboBatiment.setOnAction(e -> actualiserGraphiques());
        comboPeriode.setOnAction(e -> actualiserGraphiques());
    }

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

    // Une série par type d'énergie, valeurs agrégées par jour via TreeMap pour garder l'ordre
    private void genererCourbeTemporelle(List<EnergyRecord> records, String nomBatiment) {
        lineChartTemporel.getData().clear();
        lineChartTemporel.setTitle("Évolution de la consommation — " + nomBatiment);

        Map<TypeEnergie, List<EnergyRecord>> parType = records.stream()
            .collect(Collectors.groupingBy(EnergyRecord::getTypeEnergie));

        for (Map.Entry<TypeEnergie, List<EnergyRecord>> entree : parType.entrySet()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(entree.getKey().getLibelle());

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

    // Histogramme avec double série : quantité + coût estimé
    private void genererHistogrammeParEnergie(List<EnergyRecord> records) {
        barChartParEnergie.getData().clear();
        barChartParEnergie.setTitle("Consommation par type d'énergie");

        XYChart.Series<String, Number> serieQuantite = new XYChart.Series<>();
        serieQuantite.setName("Quantité consommée");

        XYChart.Series<String, Number> serieCout = new XYChart.Series<>();
        serieCout.setName("Coût estimé (€)");

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

    // Camembert basé sur les coûts (pas les quantités) pour mieux refléter l'impact financier
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

    private void genererComparaisonBatiments(LocalDateTime debut, LocalDateTime fin) {
        barChartComparaison.getData().clear();
        barChartComparaison.setTitle("Comparaison des consommations — Tous bâtiments");

        List<Batiment> batiments = batimentService.trouverTous();

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

                String nomBatiment = b.getNom().length() > 15
                    ? b.getNom().substring(0, 12) + "..."
                    : b.getNom();

                serie.getData().add(new XYChart.Data<>(nomBatiment, total));
            }

            barChartComparaison.getData().add(serie);
        }
    }

    private void actualiserAnalyse(Batiment batiment) {
        TypeEnergie dominante = energyService.getEnergieDominante(batiment.getId());
        if (labelEnergieDominante != null) {
            labelEnergieDominante.setText(dominante != null
                ? dominante.getLibelle() : "Aucune donnée");
        }

        // Tendance : vert si ça baisse, rouge si ça monte trop
        double tendance = energyService.calculerTendance(batiment.getId());
        if (labelTendance != null) {
            String symbole = tendance >= 0 ? "▲ +" : "▼ ";
            String couleur = tendance > 10 ? "#f44336" : (tendance < -5 ? "#4caf50" : "#ff9800");
            labelTendance.setText(symbole + String.format("%.1f%%", Math.abs(tendance)));
            labelTendance.setStyle("-fx-text-fill: " + couleur + ";");
        }

        double facture = energyService.estimerFactureMensuelle(batiment.getId());
        if (labelEstimationFacture != null) {
            labelEstimationFacture.setText(String.format("%.2f €/mois", facture));
        }
    }

    private LocalDateTime[] getPeriode() {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = switch (comboPeriode.getValue()) {
            case "7 derniers jours"   -> fin.minusDays(7);
            case "3 derniers mois"    -> fin.minusMonths(3);
            case "6 derniers mois"    -> fin.minusMonths(6);
            case "12 derniers mois"   -> fin.minusMonths(12);
            default                   -> fin.minusDays(30);
        };
        return new LocalDateTime[]{debut, fin};
    }
}
