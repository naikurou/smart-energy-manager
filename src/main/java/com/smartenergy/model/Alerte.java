package com.smartenergy.model;

public class Alerte {

    private int id;

    // Niveaux de sévérité pour classifier les alertes
    public enum NiveauAlerte {
        INFO, AVERTISSEMENT, CRITIQUE
    }

    private String message;
    private Batiment batiment;
    private TypeEnergie typeEnergie;
    private NiveauAlerte niveau;
    private double valeurDetectee;
    private double seuil;

    public Alerte(String message, Batiment batiment, TypeEnergie typeEnergie,
                  NiveauAlerte niveau, double valeurDetectee, double seuil) {
        this.message = message;
        this.batiment = batiment;
        this.typeEnergie = typeEnergie;
        this.niveau = niveau;
        this.valeurDetectee = valeurDetectee;
        this.seuil = seuil;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Batiment getBatiment() { return batiment; }
    public void setBatiment(Batiment batiment) { this.batiment = batiment; }
    public TypeEnergie getTypeEnergie() { return typeEnergie; }
    public void setTypeEnergie(TypeEnergie typeEnergie) { this.typeEnergie = typeEnergie; }
    public NiveauAlerte getNiveau() { return niveau; }
    public void setNiveau(NiveauAlerte niveau) { this.niveau = niveau; }
    public double getValeurDetectee() { return valeurDetectee; }
    public void setValeurDetectee(double valeurDetectee) { this.valeurDetectee = valeurDetectee; }
    public double getSeuil() { return seuil; }
    public void setSeuil(double seuil) { this.seuil = seuil; }

    @Override
    public String toString() {
        return String.format("[%s] %s", niveau, message);
    }
}
