package com.smartenergy.service;

import com.smartenergy.dao.BatimentDAO;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des bâtiments.
 * Cette couche intermédiaire orchestre les opérations entre les contrôleurs
 * et le DAO, en appliquant les règles métier (validations, transformations).
 *
 * <p>Séparation des responsabilités : les contrôleurs ne connaissent pas le DAO,
 * ils passent toujours par le service.</p>
 */
public class BatimentService {

    /** Accès aux données pour les bâtiments */
    private final BatimentDAO batimentDAO;

    /**
     * Constructeur — instancie le DAO associé.
     */
    public BatimentService() {
        this.batimentDAO = new BatimentDAO();
    }

    /**
     * Crée un nouveau bâtiment après validation des données.
     *
     * @param batiment Le bâtiment à créer
     * @return true si la création a réussi
     * @throws IllegalArgumentException si les données sont invalides
     */
    public boolean creer(Batiment batiment) {
        valider(batiment);
        return batimentDAO.inserer(batiment);
    }

    /**
     * Met à jour un bâtiment existant après validation.
     *
     * @param batiment Le bâtiment modifié
     * @return true si la mise à jour a réussi
     * @throws IllegalArgumentException si les données sont invalides
     */
    public boolean mettreAJour(Batiment batiment) {
        if (batiment.getId() <= 0) {
            throw new IllegalArgumentException("Le bâtiment doit avoir un identifiant valide pour être mis à jour.");
        }
        valider(batiment);
        return batimentDAO.mettreAJour(batiment);
    }

    /**
     * Supprime un bâtiment et toutes ses consommations associées.
     *
     * @param id L'identifiant du bâtiment à supprimer
     * @return true si la suppression a réussi
     */
    public boolean supprimer(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant invalide pour la suppression.");
        }
        return batimentDAO.supprimer(id);
    }

    /**
     * Recherche un bâtiment par son identifiant.
     *
     * @param id L'identifiant du bâtiment
     * @return Optional contenant le bâtiment ou vide si non trouvé
     */
    public Optional<Batiment> trouverParId(int id) {
        return batimentDAO.trouverParId(id);
    }

    /**
     * Retourne la liste complète de tous les bâtiments.
     *
     * @return Liste de tous les bâtiments
     */
    public List<Batiment> trouverTous() {
        return batimentDAO.trouverTous();
    }

    /**
     * Retourne les bâtiments filtrés par type.
     *
     * @param type Le type de bâtiment à filtrer
     * @return Liste des bâtiments du type spécifié
     */
    public List<Batiment> trouverParType(TypeBatiment type) {
        return batimentDAO.trouverParType(type);
    }

    /**
     * Duplique un bâtiment avec un nouveau nom.
     * Règle métier : le nouveau nom ne doit pas être vide.
     *
     * @param batimentOriginal Le bâtiment à cloner
     * @param nouveauNom       Le nom du clone
     * @return Le bâtiment cloné avec son ID généré, ou null si échec
     */
    public Batiment cloner(Batiment batimentOriginal, String nouveauNom) {
        if (nouveauNom == null || nouveauNom.isBlank()) {
            throw new IllegalArgumentException("Le nom du clone ne peut pas être vide.");
        }
        return batimentDAO.cloner(batimentOriginal, nouveauNom);
    }

    /**
     * Valide les données d'un bâtiment avant insertion ou mise à jour.
     * Applique les règles métier de validation.
     *
     * @param batiment Le bâtiment à valider
     * @throws IllegalArgumentException si une règle est violée
     */
    private void valider(Batiment batiment) {
        if (batiment == null) {
            throw new IllegalArgumentException("Le bâtiment ne peut pas être null.");
        }
        if (batiment.getNom() == null || batiment.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du bâtiment est obligatoire.");
        }
        if (batiment.getNom().length() > 100) {
            throw new IllegalArgumentException("Le nom du bâtiment ne peut pas dépasser 100 caractères.");
        }
        if (batiment.getType() == null) {
            throw new IllegalArgumentException("Le type de bâtiment est obligatoire.");
        }
        if (batiment.getSuperficie() < 0) {
            throw new IllegalArgumentException("La superficie ne peut pas être négative.");
        }
        if (batiment.getNombreOccupants() < 0) {
            throw new IllegalArgumentException("Le nombre d'occupants ne peut pas être négatif.");
        }
        if (batiment.getAnneeConstruction() < 1800 || batiment.getAnneeConstruction() > 2100) {
            throw new IllegalArgumentException("L'année de construction doit être comprise entre 1800 et 2100.");
        }
    }
}
