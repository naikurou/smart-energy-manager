package com.smartenergy.service;

import com.smartenergy.dao.BatimentDAO;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;

import java.util.List;
import java.util.Optional;

// Couche service — les contrôleurs passent toujours par ici, jamais directement le DAO
public class BatimentService {

    private final BatimentDAO batimentDAO;

    public BatimentService() {
        this.batimentDAO = new BatimentDAO();
    }

    public boolean creer(Batiment batiment) {
        valider(batiment);
        return batimentDAO.inserer(batiment);
    }

    public boolean mettreAJour(Batiment batiment) {
        if (batiment.getId() <= 0) {
            throw new IllegalArgumentException("Le bâtiment doit avoir un identifiant valide pour être mis à jour.");
        }
        valider(batiment);
        return batimentDAO.mettreAJour(batiment);
    }

    public boolean supprimer(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant invalide pour la suppression.");
        }
        return batimentDAO.supprimer(id);
    }

    public Optional<Batiment> trouverParId(int id) {
        return batimentDAO.trouverParId(id);
    }

    public List<Batiment> trouverTous() {
        return batimentDAO.trouverTous();
    }

    public List<Batiment> trouverParType(TypeBatiment type) {
        return batimentDAO.trouverParType(type);
    }

    // Duplique un bâtiment avec un nouveau nom
    public Batiment cloner(Batiment batimentOriginal, String nouveauNom) {
        if (nouveauNom == null || nouveauNom.isBlank()) {
            throw new IllegalArgumentException("Le nom du clone ne peut pas être vide.");
        }
        return batimentDAO.cloner(batimentOriginal, nouveauNom);
    }

    // Règles de validation métier
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
