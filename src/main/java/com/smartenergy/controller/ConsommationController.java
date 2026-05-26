package com.smartenergy.controller;

import com.smartenergy.model.Batiment;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import com.smartenergy.service.BatimentService;
import com.smartenergy.service.EnergyService;
import com.smartenergy.util.CsvParser;
import com.smartenergy.util.DataGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class ConsommationController implements Initializable {

    @FXML private ComboBox<Batiment> comboBatiment;
    @FXML private ComboBox<TypeEnergie> comboTypeEnergie;
    @FXML private DatePicker datePickerDate;
    @FXML private TextField champHeure;
    @FXML private TextField champQuantite;
    @FXML private TextField champCoutManuel;
    @FXML private TextArea champNote;
    @FXML private CheckBox checkCoutAuto;

    @FXML private TableView<EnergyRecord> tableViewRecords;
    @FXML private TableColumn<EnergyRecord, Integer> colRecordId;
    @FXML private TableColumn<EnergyRecord, String> colRecordDate;
    @FXML private TableColumn<EnergyRecord, TypeEnergie> colRecordType;
    @FXML private TableColumn<EnergyRecord, Double> colRecordQuantite;
    @FXML private TableColumn<EnergyRecord, Double> colRecordCout;
    @FXML private TableColumn<EnergyRecord, String> colRecordSource;

    @FXML private Spinner<Integer> spinnerNombreJours;
    @FXML private Spinner<Integer> spinnerMesuresParJour;

    @FXML private Label labelStatut;
    @FXML private Label labelUnite;

    private final BatimentService batimentService = new BatimentService();
    private final EnergyService energyService = new EnergyService();
    private ObservableList<EnergyRecord> listeRecords;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurerColonnes();
        configurerFormulaire();
        chargerBatiments();
        configurerEvenements();
    }

    private void configurerColonnes() {
        colRecordId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRecordType.setCellValueFactory(new PropertyValueFactory<>("typeEnergie"));
        colRecordQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colRecordCout.setCellValueFactory(new PropertyValueFactory<>("coutEstime"));
        colRecordSource.setCellValueFactory(new PropertyValueFactory<>("source"));

        // Affichage custom de la date sans le "T" de LocalDateTime
        colRecordDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    EnergyRecord r = (EnergyRecord) getTableRow().getItem();
                    if (r.getDateHeure() != null) {
                        setText(r.getDateHeure().toString().replace("T", " "));
                    }
                }
            }
        });

        colRecordCout.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f €", item));
            }
        });
    }

    private void configurerFormulaire() {
        comboTypeEnergie.setItems(FXCollections.observableArrayList(TypeEnergie.values()));
        comboTypeEnergie.getSelectionModel().selectFirst();
        datePickerDate.setValue(LocalDate.now());
        champHeure.setText(LocalTime.now().getHour() + ":00");

        if (spinnerNombreJours != null) {
            spinnerNombreJours.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 30));
        }
        if (spinnerMesuresParJour != null) {
            spinnerMesuresParJour.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 4));
        }
    }

    private void chargerBatiments() {
        List<Batiment> batiments = batimentService.trouverTous();
        comboBatiment.setItems(FXCollections.observableArrayList(batiments));
        if (!batiments.isEmpty()) {
            comboBatiment.getSelectionModel().selectFirst();
            chargerRecordsBatiment();
        }
    }

    // Chaîne d'événements : changement bâtiment/énergie/quantité → recalcul auto du coût
    private void configurerEvenements() {
        comboBatiment.setOnAction(e -> chargerRecordsBatiment());

        comboTypeEnergie.setOnAction(e -> {
            TypeEnergie type = comboTypeEnergie.getValue();
            if (type != null && labelUnite != null) {
                labelUnite.setText(type.getUnite());
            }
            if (checkCoutAuto != null && checkCoutAuto.isSelected()) {
                recalculerCout();
            }
        });

        champQuantite.textProperty().addListener((obs, ancien, nouveau) -> {
            if (checkCoutAuto != null && checkCoutAuto.isSelected()) {
                recalculerCout();
            }
        });

        if (checkCoutAuto != null) {
            checkCoutAuto.selectedProperty().addListener((obs, ancien, nouveau) -> {
                if (champCoutManuel != null) {
                    champCoutManuel.setDisable(nouveau);
                    if (nouveau) recalculerCout();
                }
            });
        }
    }

    // Coût auto = quantité × tarif unitaire du type d'énergie
    private void recalculerCout() {
        try {
            TypeEnergie type = comboTypeEnergie.getValue();
            double quantite = Double.parseDouble(champQuantite.getText().trim());
            if (type != null && champCoutManuel != null) {
                champCoutManuel.setText(String.format("%.2f", type.calculerCout(quantite)));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void chargerRecordsBatiment() {
        Batiment batiment = comboBatiment.getValue();
        if (batiment == null) return;
        List<EnergyRecord> records = energyService.getRecordsBatiment(batiment.getId());
        listeRecords = FXCollections.observableArrayList(records);
        tableViewRecords.setItems(listeRecords);
    }

    @FXML
    private void enregistrerConsommation() {
        Batiment batiment = comboBatiment.getValue();
        TypeEnergie typeEnergie = comboTypeEnergie.getValue();

        if (batiment == null || typeEnergie == null) {
            afficherErreur("Sélectionnez un bâtiment et un type d'énergie.");
            return;
        }

        try {
            double quantite = Double.parseDouble(champQuantite.getText().trim());
            LocalDate date = datePickerDate.getValue();
            String[] heureParts = champHeure.getText().trim().split(":");
            int heure = Integer.parseInt(heureParts[0]);
            int minute = heureParts.length > 1 ? Integer.parseInt(heureParts[1]) : 0;
            LocalDateTime dateHeure = date.atTime(heure, minute);

            EnergyRecord record = new EnergyRecord(batiment.getId(), dateHeure,
                typeEnergie, quantite, "MANUEL");

            if (checkCoutAuto != null && !checkCoutAuto.isSelected()
                    && champCoutManuel != null && !champCoutManuel.getText().isBlank()) {
                record.setCoutEstime(Double.parseDouble(champCoutManuel.getText().trim()));
            }

            record.setNote(champNote.getText().trim());

            if (energyService.enregistrer(record)) {
                chargerRecordsBatiment();
                effacerFormulaire();
                afficherStatut("✓ Consommation enregistrée avec succès.");
            }
        } catch (NumberFormatException e) {
            afficherErreur("Valeur numérique invalide : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            afficherErreur("Données invalides : " + e.getMessage());
        }
    }

    // Import CSV via FileChooser, puis insertion batch dans le service
    @FXML
    private void importerCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv", "*.txt")
        );

        File fichier = fileChooser.showOpenDialog(null);
        if (fichier != null) {
            List<EnergyRecord> records = CsvParser.importer(fichier);
            if (!records.isEmpty()) {
                int importes = energyService.importerEnLot(records);
                chargerRecordsBatiment();
                afficherStatut(String.format("✓ Import CSV : %d/%d enregistrement(s) importé(s).",
                    importes, records.size()));
            } else {
                afficherErreur("Aucun enregistrement valide trouvé dans le fichier.");
            }
        }
    }

    @FXML
    private void genererDonneesTest() {
        Batiment batiment = comboBatiment.getValue();
        if (batiment == null) {
            afficherErreur("Sélectionnez un bâtiment avant de générer des données.");
            return;
        }

        int jours = spinnerNombreJours != null ? spinnerNombreJours.getValue() : 30;
        int mesuresParJour = spinnerMesuresParJour != null ? spinnerMesuresParJour.getValue() : 4;

        List<EnergyRecord> records = DataGenerator.generer(batiment.getId(), jours, mesuresParJour);
        int importes = energyService.importerEnLot(records);

        chargerRecordsBatiment();
        afficherStatut(String.format("✓ %d enregistrements de test générés pour '%s'.",
            importes, batiment.getNom()));
    }

    @FXML
    private void supprimerRecord() {
        EnergyRecord selection = tableViewRecords.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez un enregistrement à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cet enregistrement de consommation ?", ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirmation");

        confirmation.showAndWait().ifPresent(reponse -> {
            if (reponse == ButtonType.YES) {
                if (energyService.supprimer(selection.getId())) {
                    chargerRecordsBatiment();
                    afficherStatut("✓ Enregistrement supprimé.");
                }
            }
        });
    }

    @FXML
    private void effacerFormulaire() {
        champQuantite.clear();
        if (champCoutManuel != null) champCoutManuel.clear();
        if (champNote != null) champNote.clear();
        datePickerDate.setValue(LocalDate.now());
        champHeure.setText(LocalTime.now().getHour() + ":00");
        comboTypeEnergie.getSelectionModel().selectFirst();
    }

    private void afficherStatut(String message) {
        if (labelStatut != null) {
            labelStatut.setText(message);
            labelStatut.setStyle("-fx-text-fill: #4caf50;");
        }
    }

    private void afficherErreur(String message) {
        if (labelStatut != null) {
            labelStatut.setText("⚠ " + message);
            labelStatut.setStyle("-fx-text-fill: #f44336;");
        }
    }
}
