package com.smartenergy.model;

/**
 * Classe représentant une alerte de consommation énergétique.
 * Une alerte est générée automatiquement lorsqu'une consommation
 * dépasse un seuil défini (pic de consommation, anomalie détectée, etc.).
 */
public class Alerte {

    /** Identifiant unique de l'alerte */
    private int id;

    /** Niveau de sévérité de l'alerte */
    public enum NiveauAlerte {
        INFO, AVERTISSEMENT, CRITIQUE
    }

    /** Message descriptif de l'alerte */
    private String message;

    /** Bâtiment concerné par l'alerte */
    private Batiment batiment;

    /** Type d'énergie concerné (peut être null si l'alerte est globale) */
    private TypeEnergie typeEnergie;

    /** Niveau de sévérité */
    private NiveauAlerte niveau;

    /** Valeur qui a déclenché l'alerte */
    private double valeurDetectee;

    /** Seuil qui a été dépassé */
    private double seuil;

    /**
     * Constructeur complet d'une alerte.
     *
     * @param message        Le message descriptif
     * @param batiment       Le bâtiment concerné
     * @param typeEnergie    Le type d'énergie (ou null)
     * @param niveau         Le niveau de sévérité
     * @param valeurDetectee La valeur anormale détectée
     * @param seuil          Le seuil dépassé
     */
    public Alerte(String message, Batiment batiment, TypeEnergie typeEnergie,
                  NiveauAlerte niveau, double valeurDetectee, double seuil) {
        this.message = message;
        this.batiment = batiment;
        this.typeEnergie = typeEnergie;
        this.niveau = niveau;
        this.valeurDetectee = valeurDetectee;
        this.seuil = seuil;
    }

    // ======================== GETTERS ET SETTERS ========================

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
