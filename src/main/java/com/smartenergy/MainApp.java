package com.smartenergy;

import com.smartenergy.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // On initialise la BDD au lancement
        DatabaseManager.getInstance().initialiserBase();

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml")
            );
            Parent root = loader.load();

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

    // Ferme proprement la connexion BDD à la sortie
    @Override
    public void stop() {
        DatabaseManager.getInstance().fermerConnexion();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
