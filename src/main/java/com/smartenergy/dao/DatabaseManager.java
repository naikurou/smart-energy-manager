package com.smartenergy.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestionnaire de la base de données SQLite.
 * Implémente le pattern Singleton pour garantir une seule connexion active.
 *
 * <p>Cette classe gère le cycle de vie complet de la connexion SQLite :
 * initialisation, création des tables si nécessaire, et fermeture propre.</p>
 */
public class DatabaseManager {

    /** URL de connexion SQLite — le fichier est créé dans le répertoire d'exécution */
    private static String urlBdd = "jdbc:sqlite:smart_energy.db";

    /** Instance unique du gestionnaire (pattern Singleton) */
    private static DatabaseManager instance;

    /** Connexion active à la base de données */
    private Connection connexion;

    /**
     * Constructeur privé — empêche l'instanciation directe.
     */
    private DatabaseManager() {}

    /**
     * Retourne l'instance unique du DatabaseManager.
     * Crée l'instance si elle n'existe pas encore.
     *
     * @return L'instance singleton du DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Permet de modifier l'URL de la base de données (très utile pour utiliser une base en mémoire lors des tests).
     *
     * @param url La nouvelle URL JDBC SQLite
     */
    public static synchronized void setDatabaseUrl(String url) {
        urlBdd = url;
        if (instance != null) {
            instance.fermerConnexion();
        }
    }

    /**
     * Retourne la connexion active à la base de données.
     * Crée une nouvelle connexion si elle n'existe pas ou est fermée.
     *
     * @return La connexion SQLite active
     * @throws RuntimeException si la connexion échoue
     */
    public Connection getConnexion() {
        try {
            if (connexion == null || connexion.isClosed()) {
                connexion = DriverManager.getConnection(urlBdd);
                // Active les clés étrangères (désactivées par défaut dans SQLite)
                connexion.createStatement().execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se connecter à la base de données : " + e.getMessage(), e);
        }
        return connexion;
    }

    /**
     * Initialise la base de données en créant les tables si elles n'existent pas.
     * Cette méthode est appelée au démarrage de l'application.
     */
    public void initialiserBase() {
        try (Statement stmt = getConnexion().createStatement()) {

            // ===== TABLE BATIMENTS =====
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

            // ===== TABLE ENERGY_RECORDS =====
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

            // Index pour accélérer les recherches par bâtiment et date
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

    /**
     * Ferme proprement la connexion à la base de données.
     * Doit être appelée lors de la fermeture de l'application.
     */
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
