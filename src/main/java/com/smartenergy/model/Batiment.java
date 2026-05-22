package com.smartenergy.model;

/**
 * Classe représentant un bâtiment dans l'application Smart Energy Manager.
 * Un bâtiment est l'entité de base à laquelle sont associées les consommations énergétiques.
 *
 * <p>Chaque bâtiment possède un identifiant unique, un nom, une adresse,
 * un type (maison, bureau, etc.) et une superficie.</p>
 */
public class Batiment {

    /** Identifiant unique du bâtiment en base de données */
    private int id;

    /** Nom descriptif du bâtiment (ex: "Résidence Soleil") */
    private String nom;

    /** Adresse physique du bâtiment */
    private String adresse;

    /** Type de bâtiment (Maison, Bureau, etc.) */
    private TypeBatiment type;

    /** Superficie du bâtiment en mètres carrés */
    private double superficie;

    /** Nombre d'occupants habituels du bâtiment */
    private int nombreOccupants;

    /** Année de construction du bâtiment */
    private int anneeConstruction;

    /** Notes ou description supplémentaire */
    private String description;

    /**
     * Constructeur par défaut requis pour la désérialisation.
     */
    public Batiment() {}

    /**
     * Constructeur complet pour créer un nouveau bâtiment.
     *
     * @param nom               Le nom du bâtiment
     * @param adresse           L'adresse complète
     * @param type              Le type de bâtiment
     * @param superficie        La superficie en m²
     * @param nombreOccupants   Le nombre d'occupants
     * @param anneeConstruction L'année de construction
     * @param description       Une description ou remarque
     */
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

    /**
     * Crée une copie (clone) de ce bâtiment avec un nouveau nom.
     * Utile pour la fonctionnalité de duplication.
     *
     * @param nouveauNom Le nom à donner au bâtiment cloné
     * @return Un nouveau Batiment avec les mêmes propriétés mais un id différent
     */
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

    // ======================== GETTERS ET SETTERS ========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public TypeBatiment getType() {
        return type;
    }

    public void setType(TypeBatiment type) {
        this.type = type;
    }

    public double getSuperficie() {
        return superficie;
    }

    public void setSuperficie(double superficie) {
        this.superficie = superficie;
    }

    public int getNombreOccupants() {
        return nombreOccupants;
    }

    public void setNombreOccupants(int nombreOccupants) {
        this.nombreOccupants = nombreOccupants;
    }

    public int getAnneeConstruction() {
        return anneeConstruction;
    }

    public void setAnneeConstruction(int anneeConstruction) {
        this.anneeConstruction = anneeConstruction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Représentation textuelle du bâtiment pour l'affichage dans les listes.
     *
     * @return Le nom et le type du bâtiment
     */
    @Override
    public String toString() {
        return nom + " (" + (type != null ? type.getLibelle() : "?") + ")";
    }
}
