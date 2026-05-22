package com.smartenergy.controller;

import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;
import com.smartenergy.service.BatimentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la gestion CRUD complète des bâtiments.
 * Gère l'affichage en tableau, la sélection, la création, la modification,
 * la suppression et la duplication des bâtiments.
 *
 * <p>Ce contrôleur utilise un TableView JavaFX avec des colonnes liées
 * aux propriétés du modèle Batiment via les PropertyValueFactory.</p>
 */
public class BatimentsController implements Initializable {

    // ===== TableView et colonnes =====
    @FXML private TableView<Batiment> tableViewBatiments;
    @FXML private TableColumn<Batiment, Integer> colId;
    @FXML private TableColumn<Batiment, String> colNom;
    @FXML private TableColumn<Batiment, String> colAdresse;
    @FXML private TableColumn<Batiment, TypeBatiment> colType;
    @FXML private TableColumn<Batiment, Double> colSuperficie;
    @FXML private TableColumn<Batiment, Integer> colOccupants;

    // ===== Formulaire de saisie =====
    @FXML private TextField champNom;
    @FXML private TextField champAdresse;
    @FXML private ComboBox<TypeBatiment> comboType;
    @FXML private TextField champSuperficie;
    @FXML private TextField champOccupants;
    @FXML private TextField champAnnee;
    @FXML private TextArea champDescription;

    // ===== Boutons d'action =====
    @FXML private Button btnCreer;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnCloner;
    @FXML private Button btnEffacer;

    // ===== Filtre de recherche =====
    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> comboFiltreType;

    // ===== Label de statut =====
    @FXML private Label labelStatut;

    /** Service métier pour les bâtiments */
    private final BatimentService batimentService = new BatimentService();

    /** Liste observable pour le TableView */
    private ObservableList<Batiment> listeBatiments;

    /** Bâtiment actuellement sélectionné dans la table */
    private Batiment batimentSelectionne;

    /**
     * Initialisation de la vue après chargement du FXML.
     * Configure les colonnes, les ComboBox et charge les données.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurerColonnes();
        configurerComboTypes();
        configurerSelectionTable();
        configurerRecherche();
        chargerBatiments();
        desactiverBoutons(true);
    }

    /**
     * Configure les colonnes du TableView en liant les propriétés du modèle.
     */
    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSuperficie.setCellValueFactory(new PropertyValueFactory<>("superficie"));
        colOccupants.setCellValueFactory(new PropertyValueFactory<>("nombreOccupants"));

        // Formater la superficie avec 2 décimales
        colSuperficie.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f m²", item));
            }
        });
    }

    /**
     * Remplit les ComboBox de type de bâtiment.
     */
    private void configurerComboTypes() {
        comboType.setItems(FXCollections.observableArrayList(TypeBatiment.values()));
        comboType.getSelectionModel().selectFirst();

        comboFiltreType.setItems(FXCollections.observableArrayList(
            "Tous les types",
            TypeBatiment.MAISON.getLibelle(),
            TypeBatiment.APPARTEMENT.getLibelle(),
            TypeBatiment.BUREAU.getLibelle(),
            TypeBatiment.LOCAL_COMMERCIAL.getLibelle(),
            TypeBatiment.BATIMENT_UNIVERSITAIRE.getLibelle()
        ));
        comboFiltreType.getSelectionModel().selectFirst();
    }

    /**
     * Configure la sélection dans le tableau pour remplir le formulaire.
     */
    private void configurerSelectionTable() {
        tableViewBatiments.getSelectionModel().selectedItemProperty()
            .addListener((obs, ancien, nouveau) -> {
                batimentSelectionne = nouveau;
                if (nouveau != null) {
                    remplirFormulaire(nouveau);
                    desactiverBoutons(false);
                } else {
                    effacerFormulaire();
                    desactiverBoutons(true);
                }
            });
    }

    /**
     * Configure le filtre de recherche en temps réel.
     */
    private void configurerRecherche() {
        champRecherche.textProperty().addListener((obs, ancien, nouveau) -> filtrerBatiments());
        comboFiltreType.setOnAction(e -> filtrerBatiments());
    }

    /**
     * Charge tous les bâtiments depuis le service et actualise le tableau.
     */
    private void chargerBatiments() {
        List<Batiment> batiments = batimentService.trouverTous();
        listeBatiments = FXCollections.observableArrayList(batiments);
        tableViewBatiments.setItems(listeBatiments);
        afficherStatut("✓ " + batiments.size() + " bâtiment(s) chargé(s).");
    }

    /**
     * Filtre les bâtiments selon la recherche et le type sélectionné.
     */
    private void filtrerBatiments() {
        String recherche = champRecherche.getText().toLowerCase().trim();
        String typeFiltré = comboFiltreType.getValue();

        List<Batiment> tous = batimentService.trouverTous();
        List<Batiment> filtres = tous.stream()
            .filter(b -> {
                boolean matchNom = b.getNom().toLowerCase().contains(recherche)
                    || b.getAdresse().toLowerCase().contains(recherche);
                boolean matchType = "Tous les types".equals(typeFiltré)
                    || b.getType().getLibelle().equals(typeFiltré);
                return matchNom && matchType;
            })
            .toList();

        listeBatiments = FXCollections.observableArrayList(filtres);
        tableViewBatiments.setItems(listeBatiments);
    }

    /**
     * Crée un nouveau bâtiment à partir des données du formulaire.
     */
    @FXML
    private void creerBatiment() {
        try {
            Batiment batiment = lireFormulaire();
            if (batimentService.creer(batiment)) {
                chargerBatiments();
                effacerFormulaire();
                afficherStatut("✓ Bâtiment '" + batiment.getNom() + "' créé avec succès.");
            }
        } catch (IllegalArgumentException e) {
            afficherErreur("Données invalides : " + e.getMessage());
        }
    }

    /**
     * Modifie le bâtiment sélectionné avec les nouvelles données du formulaire.
     */
    @FXML
    private void modifierBatiment() {
        if (batimentSelectionne == null) return;

        try {
            Batiment modifie = lireFormulaire();
            modifie.setId(batimentSelectionne.getId());

            if (batimentService.mettreAJour(modifie)) {
                chargerBatiments();
                afficherStatut("✓ Bâtiment '" + modifie.getNom() + "' mis à jour.");
            }
        } catch (IllegalArgumentException e) {
            afficherErreur("Données invalides : " + e.getMessage());
        }
    }

    /**
     * Supprime le bâtiment sélectionné après confirmation.
     */
    @FXML
    private void supprimerBatiment() {
        if (batimentSelectionne == null) return;

        // Demande de confirmation avant suppression
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer la suppression");
        confirmation.setHeaderText("Supprimer le bâtiment : " + batimentSelectionne.getNom());
        confirmation.setContentText(
            "Attention : toutes les consommations associées seront également supprimées !\n\n" +
            "Êtes-vous sûr de vouloir continuer ?");

        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            if (batimentService.supprimer(batimentSelectionne.getId())) {
                chargerBatiments();
                effacerFormulaire();
                afficherStatut("✓ Bâtiment supprimé avec succès.");
            } else {
                afficherErreur("Impossible de supprimer le bâtiment.");
            }
        }
    }

    /**
     * Clone le bâtiment sélectionné en demandant un nouveau nom.
     */
    @FXML
    private void clonerBatiment() {
        if (batimentSelectionne == null) return;

        // Boîte de dialogue pour le nom du clone
        TextInputDialog dialog = new TextInputDialog("Copie de " + batimentSelectionne.getNom());
        dialog.setTitle("Cloner le bâtiment");
        dialog.setHeaderText("Dupliquer : " + batimentSelectionne.getNom());
        dialog.setContentText("Nom du nouveau bâtiment :");

        Optional<String> resultat = dialog.showAndWait();
        resultat.ifPresent(nom -> {
            try {
                Batiment clone = batimentService.cloner(batimentSelectionne, nom);
                if (clone != null) {
                    chargerBatiments();
                    afficherStatut("✓ Bâtiment cloné sous le nom '" + nom + "'.");
                }
            } catch (IllegalArgumentException e) {
                afficherErreur(e.getMessage());
            }
        });
    }

    /**
     * Efface le formulaire et désélectionne le tableau.
     */
    @FXML
    private void effacerFormulaire() {
        champNom.clear();
        champAdresse.clear();
        comboType.getSelectionModel().selectFirst();
        champSuperficie.clear();
        champOccupants.clear();
        champAnnee.setText("2000");
        champDescription.clear();
        tableViewBatiments.getSelectionModel().clearSelection();
        batimentSelectionne = null;
        desactiverBoutons(true);
    }

    /**
     * Remplit le formulaire avec les données du bâtiment sélectionné.
     *
     * @param batiment Le bâtiment dont les données sont à afficher
     */
    private void remplirFormulaire(Batiment batiment) {
        champNom.setText(batiment.getNom());
        champAdresse.setText(batiment.getAdresse() != null ? batiment.getAdresse() : "");
        comboType.setValue(batiment.getType());
        champSuperficie.setText(String.valueOf(batiment.getSuperficie()));
        champOccupants.setText(String.valueOf(batiment.getNombreOccupants()));
        champAnnee.setText(String.valueOf(batiment.getAnneeConstruction()));
        champDescription.setText(batiment.getDescription() != null ? batiment.getDescription() : "");
    }

    /**
     * Lit les données du formulaire et crée un objet Batiment.
     * Lance une exception si les champs obligatoires sont manquants ou invalides.
     *
     * @return Le Batiment construit à partir du formulaire
     * @throws IllegalArgumentException si les données sont invalides
     */
    private Batiment lireFormulaire() {
        String nom = champNom.getText().trim();
        if (nom.isBlank()) throw new IllegalArgumentException("Le nom est obligatoire.");

        double superficie;
        try {
            superficie = champSuperficie.getText().isBlank() ? 0
                : Double.parseDouble(champSuperficie.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La superficie doit être un nombre.");
        }

        int occupants;
        try {
            occupants = champOccupants.getText().isBlank() ? 1
                : Integer.parseInt(champOccupants.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le nombre d'occupants doit être un entier.");
        }

        int annee;
        try {
            annee = champAnnee.getText().isBlank() ? 2000
                : Integer.parseInt(champAnnee.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("L'année doit être un entier.");
        }

        TypeBatiment type = comboType.getValue();
        if (type == null) throw new IllegalArgumentException("Sélectionnez un type de bâtiment.");

        return new Batiment(nom, champAdresse.getText().trim(), type,
            superficie, occupants, annee, champDescription.getText().trim());
    }

    /**
     * Active ou désactive les boutons d'action selon la sélection.
     *
     * @param desactiver true pour désactiver Modifier/Supprimer/Cloner
     */
    private void desactiverBoutons(boolean desactiver) {
        btnModifier.setDisable(desactiver);
        btnSupprimer.setDisable(desactiver);
        btnCloner.setDisable(desactiver);
    }

    /**
     * Affiche un message de statut vert.
     *
     * @param message Le message à afficher
     */
    private void afficherStatut(String message) {
        labelStatut.setText(message);
        labelStatut.setStyle("-fx-text-fill: #4caf50;");
    }

    /**
     * Affiche un message d'erreur rouge.
     *
     * @param message Le message d'erreur
     */
    private void afficherErreur(String message) {
        labelStatut.setText("⚠ " + message);
        labelStatut.setStyle("-fx-text-fill: #f44336;");
    }
}
