package com.smartenergy.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Singleton pour garantir une seule connexion à la BDD
public class DatabaseManager {

    private static String urlBdd = "jdbc:sqlite:smart_energy.db";
    private static DatabaseManager instance;
    private Connection connexion;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Permet de changer l'URL, utile pour les tests avec une base en mémoire
    public static synchronized void setDatabaseUrl(String url) {
        urlBdd = url;
        if (instance != null) {
            instance.fermerConnexion();
        }
    }

    public Connection getConnexion() {
        try {
            if (connexion == null || connexion.isClosed()) {
                connexion = DriverManager.getConnection(urlBdd);
                // Les clés étrangères sont désactivées par défaut dans SQLite
                connexion.createStatement().execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se connecter à la base de données : " + e.getMessage(), e);
        }
        return connexion;
    }

    public void initialiserBase() {
        try (Statement stmt = getConnexion().createStatement()) {

            // Table bâtiments
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS batiments (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    nom                 TEXT NOT NULL,
                    adresse             TEXT,
                    type                TEXT NOT NULL,
                    superficie          REAL DEFAULT 0.0,
                    nombre_occupants    INTEGER DEFAULT 1,
                    annee_construction  INTEGER DEFAULT 2000,
                    description         TEXT
                )
            """);

            // Table consommations énergétiques
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS energy_records (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    batiment_id     INTEGER NOT NULL,
                    date_heure      TEXT NOT NULL,
                    type_energie    TEXT NOT NULL,
                    quantite        REAL NOT NULL,
                    cout_estime     REAL DEFAULT 0.0,
                    source          TEXT DEFAULT 'MANUEL',
                    note            TEXT,
                    FOREIGN KEY (batiment_id) REFERENCES batiments(id) ON DELETE CASCADE
                )
            """);

            // Index pour accélérer les recherches fréquentes
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_records_batiment
                ON energy_records (batiment_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_records_date
                ON energy_records (date_heure)
            """);

            System.out.println("✓ Base de données initialisée avec succès.");

        } catch (SQLException e) {
            System.err.println("Erreur d'initialisation de la base : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void fermerConnexion() {
        try {
            if (connexion != null && !connexion.isClosed()) {
                connexion.close();
                System.out.println("✓ Connexion à la base de données fermée.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}
