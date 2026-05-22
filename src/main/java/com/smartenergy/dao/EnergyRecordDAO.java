package com.smartenergy.dao;

import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * DAO pour la gestion des enregistrements de consommation énergétique.
 * Fournit les opérations CRUD ainsi que des requêtes analytiques avancées
 * pour alimenter le tableau de bord et les visualisations.
 */
public class EnergyRecordDAO {

    /** Format de date/heure utilisé pour le stockage SQLite */
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Insère un nouvel enregistrement de consommation en base de données.
     *
     * @param record L'enregistrement à insérer
     * @return true si l'insertion a réussi, false sinon
     */
    public boolean inserer(EnergyRecord record) {
        String sql = """
            INSERT INTO energy_records
                (batiment_id, date_heure, type_energie, quantite, cout_estime, source, note)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, record.getBatimentId());
            ps.setString(2, record.getDateHeure().format(FORMATTER));
            ps.setString(3, record.getTypeEnergie().name());
            ps.setDouble(4, record.getQuantite());
            ps.setDouble(5, record.getCoutEstime());
            ps.setString(6, record.getSource());
            ps.setString(7, record.getNote());

            int lignes = ps.executeUpdate();
            if (lignes > 0) {
                ResultSet cles = ps.getGeneratedKeys();
                if (cles.next()) {
                    record.setId(cles.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de l'enregistrement : " + e.getMessage());
        }
        return false;
    }

    /**
     * Insère plusieurs enregistrements en une seule transaction pour les imports CSV.
     * L'utilisation d'une transaction améliore considérablement les performances.
     *
     * @param records La liste d'enregistrements à insérer
     * @return Le nombre d'enregistrements insérés avec succès
     */
    public int insererEnLot(List<EnergyRecord> records) {
        String sql = """
            INSERT INTO energy_records
                (batiment_id, date_heure, type_energie, quantite, cout_estime, source, note)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        int compteur = 0;
        Connection conn = DatabaseManager.getInstance().getConnexion();

        try {
            // Désactivation de l'auto-commit pour la transaction
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (EnergyRecord record : records) {
                    ps.setInt(1, record.getBatimentId());
                    ps.setString(2, record.getDateHeure().format(FORMATTER));
                    ps.setString(3, record.getTypeEnergie().name());
                    ps.setDouble(4, record.getQuantite());
                    ps.setDouble(5, record.getCoutEstime());
                    ps.setString(6, record.getSource());
                    ps.setString(7, record.getNote());
                    ps.addBatch();
                    compteur++;
                }
                ps.executeBatch();
            }

            conn.commit();
            System.out.println("✓ " + compteur + " enregistrements insérés en lot.");

        } catch (SQLException e) {
            try {
                conn.rollback(); // Annulation en cas d'erreur
            } catch (SQLException ex) {
                System.err.println("Erreur lors du rollback : " + ex.getMessage());
            }
            System.err.println("Erreur lors de l'insertion en lot : " + e.getMessage());
            compteur = 0;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur restauration auto-commit : " + e.getMessage());
            }
        }

        return compteur;
    }

    /**
     * Supprime un enregistrement par son identifiant.
     *
     * @param id L'identifiant de l'enregistrement à supprimer
     * @return true si la suppression a réussi
     */
    public boolean supprimer(int id) {
        String sql = "DELETE FROM energy_records WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
        }
        return false;
    }

    /**
     * Récupère tous les enregistrements d'un bâtiment.
     *
     * @param batimentId L'identifiant du bâtiment
     * @return La liste des enregistrements triés par date décroissante
     */
    public List<EnergyRecord> trouverParBatiment(int batimentId) {
        List<EnergyRecord> records = new ArrayList<>();
        String sql = """
            SELECT * FROM energy_records
            WHERE batiment_id = ?
            ORDER BY date_heure DESC
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setInt(1, batimentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(construireRecord(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des enregistrements : " + e.getMessage());
        }
        return records;
    }

    /**
     * Récupère les enregistrements d'un bâtiment pour une période donnée.
     *
     * @param batimentId L'identifiant du bâtiment
     * @param debut      La date/heure de début
     * @param fin        La date/heure de fin
     * @return Les enregistrements dans la période spécifiée
     */
    public List<EnergyRecord> trouverParBatimentEtPeriode(int batimentId,
                                                           LocalDateTime debut,
                                                           LocalDateTime fin) {
        List<EnergyRecord> records = new ArrayList<>();
        String sql = """
            SELECT * FROM energy_records
            WHERE batiment_id = ?
              AND date_heure BETWEEN ? AND ?
            ORDER BY date_heure
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setInt(1, batimentId);
            ps.setString(2, debut.format(FORMATTER));
            ps.setString(3, fin.format(FORMATTER));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                records.add(construireRecord(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par période : " + e.getMessage());
        }
        return records;
    }

    /**
     * Calcule la consommation totale (par type d'énergie) pour tous les bâtiments
     * sur une période donnée. Utilisé pour le tableau de bord global.
     *
     * @param debut Date de début
     * @param fin   Date de fin
     * @return Map associant chaque TypeEnergie à sa consommation totale
     */
    public Map<TypeEnergie, Double> getTotauxParEnergie(LocalDateTime debut, LocalDateTime fin) {
        Map<TypeEnergie, Double> totaux = new HashMap<>();
        String sql = """
            SELECT type_energie, SUM(quantite) as total
            FROM energy_records
            WHERE date_heure BETWEEN ? AND ?
            GROUP BY type_energie
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setString(1, debut.format(FORMATTER));
            ps.setString(2, fin.format(FORMATTER));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TypeEnergie type = TypeEnergie.valueOf(rs.getString("type_energie"));
                totaux.put(type, rs.getDouble("total"));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul des totaux : " + e.getMessage());
        }
        return totaux;
    }

    /**
     * Identifie le bâtiment le plus consommateur sur une période donnée.
     *
     * @param debut Date de début
     * @param fin   Date de fin
     * @return L'identifiant du bâtiment le plus consommateur, ou -1 si aucun résultat
     */
    public int getBatimentPlusConsommateur(LocalDateTime debut, LocalDateTime fin) {
        String sql = """
            SELECT batiment_id, SUM(quantite) as total
            FROM energy_records
            WHERE date_heure BETWEEN ? AND ?
            GROUP BY batiment_id
            ORDER BY total DESC
            LIMIT 1
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setString(1, debut.format(FORMATTER));
            ps.setString(2, fin.format(FORMATTER));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("batiment_id");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche du bâtiment max : " + e.getMessage());
        }
        return -1;
    }

    /**
     * Calcule le coût total estimé sur une période pour un bâtiment.
     *
     * @param batimentId L'identifiant du bâtiment
     * @param debut      Date de début
     * @param fin        Date de fin
     * @return Le coût total en euros
     */
    public double getCoutTotalBatiment(int batimentId, LocalDateTime debut, LocalDateTime fin) {
        String sql = """
            SELECT COALESCE(SUM(cout_estime), 0) as total_cout
            FROM energy_records
            WHERE batiment_id = ? AND date_heure BETWEEN ? AND ?
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setInt(1, batimentId);
            ps.setString(2, debut.format(FORMATTER));
            ps.setString(3, fin.format(FORMATTER));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total_cout");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul du coût total : " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Récupère tous les enregistrements de la base de données.
     * Utilisé pour les analyses globales.
     *
     * @return La liste complète des enregistrements
     */
    public List<EnergyRecord> trouverTous() {
        List<EnergyRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM energy_records ORDER BY date_heure DESC";

        try (Statement stmt = DatabaseManager.getInstance().getConnexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(construireRecord(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération globale : " + e.getMessage());
        }
        return records;
    }

    /**
     * Construit un EnergyRecord à partir d'un ResultSet SQL.
     * Méthode utilitaire interne.
     *
     * @param rs Le ResultSet positionné sur une ligne
     * @return L'objet EnergyRecord correspondant
     * @throws SQLException en cas d'erreur de lecture
     */
    private EnergyRecord construireRecord(ResultSet rs) throws SQLException {
        EnergyRecord record = new EnergyRecord();
        record.setId(rs.getInt("id"));
        record.setBatimentId(rs.getInt("batiment_id"));
        record.setDateHeure(LocalDateTime.parse(rs.getString("date_heure"), FORMATTER));
        record.setTypeEnergie(TypeEnergie.valueOf(rs.getString("type_energie")));
        record.setQuantite(rs.getDouble("quantite"));
        record.setCoutEstime(rs.getDouble("cout_estime"));
        record.setSource(rs.getString("source"));
        record.setNote(rs.getString("note"));
        return record;
    }
}
