package com.smartenergy.model;

public class Batiment {

    private int id;
    private String nom;
    private String adresse;
    private TypeBatiment type;
    private double superficie;
    private int nombreOccupants;
    private int anneeConstruction;
    private String description;

    public Batiment() {}

    public Batiment(String nom, String adresse, TypeBatiment type,
                    double superficie, int nombreOccupants,
                    int anneeConstruction, String description) {
        this.nom = nom;
        this.adresse = adresse;
        this.type = type;
        this.superficie = superficie;
        this.nombreOccupants = nombreOccupants;
        this.anneeConstruction = anneeConstruction;
        this.description = description;
    }

    // Duplique le bâtiment avec un nouveau nom
    public Batiment cloner(String nouveauNom) {
        return new Batiment(
            nouveauNom,
            this.adresse,
            this.type,
            this.superficie,
            this.nombreOccupants,
            this.anneeConstruction,
            "Copie de " + this.nom + " - " + this.description
        );
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public TypeBatiment getType() { return type; }
    public void setType(TypeBatiment type) { this.type = type; }
    public double getSuperficie() { return superficie; }
    public void setSuperficie(double superficie) { this.superficie = superficie; }
    public int getNombreOccupants() { return nombreOccupants; }
    public void setNombreOccupants(int nombreOccupants) { this.nombreOccupants = nombreOccupants; }
    public int getAnneeConstruction() { return anneeConstruction; }
    public void setAnneeConstruction(int anneeConstruction) { this.anneeConstruction = anneeConstruction; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return nom + " (" + (type != null ? type.getLibelle() : "?") + ")";
    }
}
