package com.smartenergy.model;

public enum TypeBatiment {
    MAISON("Maison"),
    APPARTEMENT("Appartement"),
    BUREAU("Bureau"),
    LOCAL_COMMERCIAL("Local commercial"),
    BATIMENT_UNIVERSITAIRE("Bâtiment universitaire");

    private final String libelle;

    TypeBatiment(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    // toString pour afficher le libellé dans les ComboBox JavaFX
    @Override
    public String toString() {
        return libelle;
    }
}
