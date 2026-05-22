package com.smartenergy.model;

/**
 * Enumération représentant les différents types d'énergie
 * suivis par l'application Smart Energy Manager.
 */
public enum TypeEnergie {
    ELECTRICITE("Électricité", "kWh", 0.18),
    EAU("Eau", "m³", 3.50),
    GAZ("Gaz", "m³", 1.12),
    CHAUFFAGE("Chauffage", "kWh", 0.09),
    CLIMATISATION("Climatisation", "kWh", 0.18);

    /** Libellé affiché dans l'interface */
    private final String libelle;
    /** Unité de mesure associée à ce type d'énergie */
    private final String unite;
    /** Prix unitaire par défaut (en euros) */
    private final double prixUnitaire;

    /**
     * Constructeur de l'énumération TypeEnergie.
     *
     * @param libelle       Le nom affiché du type d'énergie
     * @param unite         L'unité de mesure (ex: kWh, m³)
     * @param prixUnitaire  Le coût par unité en euros
     */
    TypeEnergie(String libelle, String unite, double prixUnitaire) {
        this.libelle = libelle;
        this.unite = unite;
        this.prixUnitaire = prixUnitaire;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getUnite() {
        return unite;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    /**
     * Calcule le coût estimé pour une quantité donnée.
     *
     * @param quantite La quantité consommée
     * @return Le coût estimé en euros
     */
    public double calculerCout(double quantite) {
        return quantite * prixUnitaire;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
