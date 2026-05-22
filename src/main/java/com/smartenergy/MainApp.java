package com.smartenergy;

import com.smartenergy.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principale de l'application Smart Energy Manager.
 * Point d'entrée de l'application JavaFX.
 * Elle initialise la base de données et charge la vue principale.
 */
public class MainApp extends Application {

    /**
     * Méthode de démarrage de l'application JavaFX.
     * Initialise la base de données, charge le FXML principal et affiche la fenêtre.
     *
     * @param primaryStage La fenêtre principale de l'application
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialisation de la base de données SQLite
        DatabaseManager.getInstance().initialiserBase();

        try {
            // Chargement de la vue principale depuis le fichier FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml")
            );
            Parent root = loader.load();

            // Configuration de la scène principale
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
            );

            primaryStage.setTitle("Smart Energy Manager");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'interface : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Méthode appelée à la fermeture de l'application.
     * Ferme proprement la connexion à la base de données.
     */
    @Override
    public void stop() {
        DatabaseManager.getInstance().fermerConnexion();
    }

    /**
     * Point d'entrée principal Java.
     *
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        launch(args);
    }
}
