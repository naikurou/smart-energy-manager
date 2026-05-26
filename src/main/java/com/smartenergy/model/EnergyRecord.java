package com.smartenergy.model;

import java.time.LocalDateTime;

public class EnergyRecord {

    private int id;
    private int batimentId;
    private Batiment batiment;
    private LocalDateTime dateHeure;
    private TypeEnergie typeEnergie;
    private double quantite;
    private double coutEstime;
    private String source;
    private String note;

    public EnergyRecord() {
        this.source = "MANUEL";
    }

    public EnergyRecord(int batimentId, LocalDateTime dateHeure,
                        TypeEnergie typeEnergie, double quantite, String source) {
        this.batimentId = batimentId;
        this.dateHeure = dateHeure;
        this.typeEnergie = typeEnergie;
        this.quantite = quantite;
        // Calcul du coût estimé selon le type d'énergie
        this.coutEstime = typeEnergie.calculerCout(quantite);
        this.source = source;
    }

    // Recalcule le coût si la quantité ou le type change
    public void recalculerCout() {
        if (typeEnergie != null) {
            this.coutEstime = typeEnergie.calculerCout(quantite);
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBatimentId() { return batimentId; }
    public void setBatimentId(int batimentId) { this.batimentId = batimentId; }
    public Batiment getBatiment() { return batiment; }

    public void setBatiment(Batiment batiment) {
        this.batiment = batiment;
        if (batiment != null) {
            this.batimentId = batiment.getId();
        }
    }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
    public TypeEnergie getTypeEnergie() { return typeEnergie; }

    public void setTypeEnergie(TypeEnergie typeEnergie) {
        this.typeEnergie = typeEnergie;
        recalculerCout();
    }

    public double getQuantite() { return quantite; }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
        recalculerCout();
    }

    public double getCoutEstime() { return coutEstime; }
    public void setCoutEstime(double coutEstime) { this.coutEstime = coutEstime; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

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
