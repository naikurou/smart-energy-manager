package com.smartenergy.model;

import java.time.LocalDateTime;

/**
 * Classe représentant un enregistrement de consommation énergétique.
 * Chaque EnergyRecord correspond à une mesure ponctuelle de consommation
 * d'un type d'énergie pour un bâtiment donné.
 *
 * <p>Un enregistrement contient la date/heure de la mesure, le type d'énergie,
 * la quantité consommée et le coût estimé associé.</p>
 */
public class EnergyRecord {

    /** Identifiant unique en base de données */
    private int id;

    /** Identifiant du bâtiment auquel appartient cet enregistrement */
    private int batimentId;

    /** Référence vers l'objet Batiment associé (chargé à la demande) */
    private Batiment batiment;

    /** Date et heure de la mesure de consommation */
    private LocalDateTime dateHeure;

    /** Type d'énergie mesurée */
    private TypeEnergie typeEnergie;

    /** Quantité consommée (dans l'unité définie par le TypeEnergie) */
    private double quantite;

    /** Coût estimé en euros pour cette consommation */
    private double coutEstime;

    /** Source de la donnée : MANUEL, CSV ou GENERE */
    private String source;

    /** Note ou commentaire optionnel */
    private String note;

    /**
     * Constructeur par défaut.
     */
    public EnergyRecord() {
        this.source = "MANUEL";
    }

    /**
     * Constructeur complet pour créer un enregistrement de consommation.
     *
     * @param batimentId    L'identifiant du bâtiment
     * @param dateHeure     La date et heure de la mesure
     * @param typeEnergie   Le type d'énergie consommée
     * @param quantite      La quantité consommée
     * @param source        La source de la donnée (MANUEL, CSV, GENERE)
     */
    public EnergyRecord(int batimentId, LocalDateTime dateHeure,
                        TypeEnergie typeEnergie, double quantite, String source) {
        this.batimentId = batimentId;
        this.dateHeure = dateHeure;
        this.typeEnergie = typeEnergie;
        this.quantite = quantite;
        // Calcul automatique du coût estimé selon le type d'énergie
        this.coutEstime = typeEnergie.calculerCout(quantite);
        this.source = source;
    }

    /**
     * Recalcule le coût estimé à partir de la quantité et du prix unitaire.
     * Doit être appelée si la quantité est modifiée après construction.
     */
    public void recalculerCout() {
        if (typeEnergie != null) {
            this.coutEstime = typeEnergie.calculerCout(quantite);
        }
    }

    // ======================== GETTERS ET SETTERS ========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBatimentId() {
        return batimentId;
    }

    public void setBatimentId(int batimentId) {
        this.batimentId = batimentId;
    }

    public Batiment getBatiment() {
        return batiment;
    }

    public void setBatiment(Batiment batiment) {
        this.batiment = batiment;
        if (batiment != null) {
            this.batimentId = batiment.getId();
        }
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public TypeEnergie getTypeEnergie() {
        return typeEnergie;
    }

    public void setTypeEnergie(TypeEnergie typeEnergie) {
        this.typeEnergie = typeEnergie;
        recalculerCout();
    }

    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
        recalculerCout();
    }

    public double getCoutEstime() {
        return coutEstime;
    }

    public void setCoutEstime(double coutEstime) {
        this.coutEstime = coutEstime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %.2f %s (%.2f€)",
            dateHeure != null ? dateHeure.toLocalDate() : "?",
            typeEnergie != null ? typeEnergie.getLibelle() : "?",
            quantite,
            typeEnergie != null ? typeEnergie.getUnite() : "",
            coutEstime
        );
    }
}
