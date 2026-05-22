package com.smartenergy.dao;

import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) pour la gestion des bâtiments en base de données.
 * Implémente les opérations CRUD complètes pour l'entité Batiment.
 *
 * <p>Chaque méthode gère sa propre gestion d'erreur et journalise
 * les problèmes SQL pour faciliter le débogage.</p>
 */
public class BatimentDAO {

    /**
     * Enregistre un nouveau bâtiment dans la base de données.
     *
     * @param batiment Le bâtiment à insérer
     * @return true si l'insertion a réussi, false sinon
     */
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

            // Récupération de l'ID généré automatiquement
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

    /**
     * Met à jour les informations d'un bâtiment existant.
     *
     * @param batiment Le bâtiment à mettre à jour (doit avoir un id valide)
     * @return true si la mise à jour a réussi, false sinon
     */
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

    /**
     * Supprime un bâtiment et toutes ses consommations associées (CASCADE).
     *
     * @param id L'identifiant du bâtiment à supprimer
     * @return true si la suppression a réussi, false sinon
     */
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

    /**
     * Recherche un bâtiment par son identifiant.
     *
     * @param id L'identifiant du bâtiment
     * @return Un Optional contenant le bâtiment trouvé, ou vide si inexistant
     */
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

    /**
     * Récupère la liste complète de tous les bâtiments.
     *
     * @return La liste de tous les bâtiments triés par nom
     */
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

    /**
     * Recherche des bâtiments par type.
     *
     * @param type Le type de bâtiment à filtrer
     * @return La liste des bâtiments du type spécifié
     */
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

    /**
     * Duplique un bâtiment existant en base de données.
     *
     * @param batimentOriginal Le bâtiment à cloner
     * @param nouveauNom       Le nom du nouveau bâtiment
     * @return Le nouveau bâtiment inséré, ou null si l'opération a échoué
     */
    public Batiment cloner(Batiment batimentOriginal, String nouveauNom) {
        Batiment clone = batimentOriginal.cloner(nouveauNom);
        if (inserer(clone)) {
            return clone;
        }
        return null;
    }

    /**
     * Construit un objet Batiment à partir d'une ligne de ResultSet SQL.
     * Méthode utilitaire interne pour éviter la duplication de code.
     *
     * @param rs Le ResultSet positionné sur une ligne
     * @return L'objet Batiment correspondant
     * @throws SQLException en cas d'erreur de lecture du ResultSet
     */
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
