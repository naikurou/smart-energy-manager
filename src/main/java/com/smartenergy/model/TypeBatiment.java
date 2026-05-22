package com.smartenergy.model;

/**
 * Enumération représentant les différents types de bâtiments
 * gérés par l'application Smart Energy Manager.
 */
public enum TypeBatiment {
    MAISON("Maison"),
    APPARTEMENT("Appartement"),
    BUREAU("Bureau"),
    LOCAL_COMMERCIAL("Local commercial"),
    BATIMENT_UNIVERSITAIRE("Bâtiment universitaire");

    /** Libellé affiché dans l'interface utilisateur */
    private final String libelle;

    /**
     * Constructeur de l'énumération.
     *
     * @param libelle Le texte affiché pour ce type de bâtiment
     */
    TypeBatiment(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Retourne le libellé lisible du type de bâtiment.
     *
     * @return Le libellé sous forme de chaîne de caractères
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Surcharge de toString() pour afficher le libellé
     * dans les ComboBox et autres composants JavaFX.
     *
     * @return Le libellé du type de bâtiment
     */
    @Override
    public String toString() {
        return libelle;
    }
}
