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

public class EnergyRecordDAO {

    // Format compatible avec le stockage texte de SQLite
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    // Transaction batch pour optimiser les imports CSV
    public int insererEnLot(List<EnergyRecord> records) {
        String sql = """
            INSERT INTO energy_records
                (batiment_id, date_heure, type_energie, quantite, cout_estime, source, note)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        int compteur = 0;
        Connection conn = DatabaseManager.getInstance().getConnexion();

        try {
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
                conn.rollback(); // On annule tout si ça plante
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

    // Agrégation par type d'énergie pour le dashboard
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

    // Trouve le bâtiment qui consomme le plus sur la période
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

    // COALESCE pour éviter un null si aucun enregistrement trouvé
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
