package com.smartenergy.model;

public enum TypeEnergie {
    ELECTRICITE("Électricité", "kWh", 0.18),
    EAU("Eau", "m³", 3.50),
    GAZ("Gaz", "m³", 1.12),
    CHAUFFAGE("Chauffage", "kWh", 0.09),
    CLIMATISATION("Climatisation", "kWh", 0.18);

    private final String libelle;
    private final String unite;
    // Prix par défaut en euros par unité
    private final double prixUnitaire;

    TypeEnergie(String libelle, String unite, double prixUnitaire) {
        this.libelle = libelle;
        this.unite = unite;
        this.prixUnitaire = prixUnitaire;
    }

    public String getLibelle() { return libelle; }
    public String getUnite() { return unite; }
    public double getPrixUnitaire() { return prixUnitaire; }

    public double calculerCout(double quantite) {
        return quantite * prixUnitaire;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
