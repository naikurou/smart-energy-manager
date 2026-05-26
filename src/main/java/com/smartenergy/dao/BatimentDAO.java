package com.smartenergy.dao;

import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BatimentDAO {

    public boolean inserer(Batiment batiment) {
        String sql = """
            INSERT INTO batiments
                (nom, adresse, type, superficie, nombre_occupants, annee_construction, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, batiment.getNom());
            ps.setString(2, batiment.getAdresse());
            ps.setString(3, batiment.getType().name());
            ps.setDouble(4, batiment.getSuperficie());
            ps.setInt(5, batiment.getNombreOccupants());
            ps.setInt(6, batiment.getAnneeConstruction());
            ps.setString(7, batiment.getDescription());

            int lignesAffectees = ps.executeUpdate();

            // On récupère l'ID auto-généré par SQLite
            if (lignesAffectees > 0) {
                ResultSet cles = ps.getGeneratedKeys();
                if (cles.next()) {
                    batiment.setId(cles.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du bâtiment : " + e.getMessage());
        }
        return false;
    }

    public boolean mettreAJour(Batiment batiment) {
        String sql = """
            UPDATE batiments SET
                nom = ?,
                adresse = ?,
                type = ?,
                superficie = ?,
                nombre_occupants = ?,
                annee_construction = ?,
                description = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setString(1, batiment.getNom());
            ps.setString(2, batiment.getAdresse());
            ps.setString(3, batiment.getType().name());
            ps.setDouble(4, batiment.getSuperficie());
            ps.setInt(5, batiment.getNombreOccupants());
            ps.setInt(6, batiment.getAnneeConstruction());
            ps.setString(7, batiment.getDescription());
            ps.setInt(8, batiment.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du bâtiment : " + e.getMessage());
        }
        return false;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM batiments WHERE id = ?";

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du bâtiment : " + e.getMessage());
        }
        return false;
    }

    public Optional<Batiment> trouverParId(int id) {
        String sql = "SELECT * FROM batiments WHERE id = ?";

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(construireBatiment(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche du bâtiment : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Batiment> trouverTous() {
        List<Batiment> batiments = new ArrayList<>();
        String sql = "SELECT * FROM batiments ORDER BY nom";

        try (Statement stmt = DatabaseManager.getInstance().getConnexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                batiments.add(construireBatiment(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des bâtiments : " + e.getMessage());
        }
        return batiments;
    }

    public List<Batiment> trouverParType(TypeBatiment type) {
        List<Batiment> batiments = new ArrayList<>();
        String sql = "SELECT * FROM batiments WHERE type = ? ORDER BY nom";

        try (PreparedStatement ps = DatabaseManager.getInstance()
                .getConnexion().prepareStatement(sql)) {

            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                batiments.add(construireBatiment(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par type : " + e.getMessage());
        }
        return batiments;
    }

    public Batiment cloner(Batiment batimentOriginal, String nouveauNom) {
        Batiment clone = batimentOriginal.cloner(nouveauNom);
        if (inserer(clone)) {
            return clone;
        }
        return null;
    }


    private Batiment construireBatiment(ResultSet rs) throws SQLException {
        Batiment b = new Batiment();
        b.setId(rs.getInt("id"));
        b.setNom(rs.getString("nom"));
        b.setAdresse(rs.getString("adresse"));
        b.setType(TypeBatiment.valueOf(rs.getString("type")));
        b.setSuperficie(rs.getDouble("superficie"));
        b.setNombreOccupants(rs.getInt("nombre_occupants"));
        b.setAnneeConstruction(rs.getInt("annee_construction"));
        b.setDescription(rs.getString("description"));
        return b;
    }
}
