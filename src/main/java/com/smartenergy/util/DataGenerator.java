package com.smartenergy.util;

import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Générateur de données de test pour l'application Smart Energy Manager.
 * Produit des enregistrements réalistes simulant la consommation énergétique
 * d'un bâtiment sur une période donnée.
 *
 * <p>Les données générées imitent les variations naturelles de consommation :
 * pics matinaux et vespéraux pour l'électricité, consommation d'eau étalée,
 * chauffage plus élevé en hiver, etc.</p>
 */
public class DataGenerator {

    /** Générateur de nombres aléatoires avec graine fixe pour reproductibilité */
    private static final Random RANDOM = new Random(42);

    /**
     * Génère des données de consommation simulées pour un bâtiment.
     *
     * @param batimentId      L'identifiant du bâtiment cible
     * @param nombreJours     Le nombre de jours à générer
     * @param mesuresParJour  Le nombre de mesures par jour (ex: 4 = toutes les 6h)
     * @return La liste des enregistrements générés, marqués source "GENERE"
     */
    public static List<EnergyRecord> generer(int batimentId,
                                              int nombreJours,
                                              int mesuresParJour) {
        List<EnergyRecord> records = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();

        for (int j = nombreJours - 1; j >= 0; j--) {
            LocalDateTime jourBase = maintenant.minusDays(j);

            for (int m = 0; m < mesuresParJour; m++) {
                // Heure de la mesure répartie dans la journée
                int heure = (24 / mesuresParJour) * m;
                LocalDateTime dateHeure = jourBase.withHour(heure).withMinute(0).withSecond(0);

                // Génération d'une mesure pour chaque type d'énergie
                for (TypeEnergie type : TypeEnergie.values()) {
                    double quantite = genererQuantite(type, heure, jourBase.getMonthValue());
                    EnergyRecord record = new EnergyRecord(batimentId, dateHeure, type,
                        quantite, "GENERE");
                    records.add(record);
                }
            }
        }

        System.out.printf("✓ %d enregistrements générés pour le bâtiment #%d.%n",
            records.size(), batimentId);
        return records;
    }

    /**
     * Génère une quantité réaliste pour un type d'énergie en tenant compte
     * de l'heure de la journée et du mois (saisonnalité).
     *
     * @param type  Le type d'énergie
     * @param heure L'heure de la journée (0-23)
     * @param mois  Le mois de l'année (1-12)
     * @return La quantité simulée avec variation aléatoire
     */
    private static double genererQuantite(TypeEnergie type, int heure, int mois) {
        double base = switch (type) {
            case ELECTRICITE -> genererElectricite(heure);
            case EAU         -> genererEau(heure);
            case GAZ         -> genererGaz(heure, mois);
            case CHAUFFAGE   -> genererChauffage(heure, mois);
            case CLIMATISATION -> genererClimatisation(heure, mois);
        };

        // Ajout d'une variation aléatoire de ±15% pour le réalisme
        double variation = 1.0 + (RANDOM.nextDouble() * 0.30 - 0.15);
        return Math.max(0, Math.round(base * variation * 100.0) / 100.0);
    }

    /**
     * Simule la consommation électrique avec des pics matin/soir.
     * Pic du matin : 7h-9h, pic du soir : 18h-21h.
     *
     * @param heure L'heure de la journée
     * @return La quantité en kWh
     */
    private static double genererElectricite(int heure) {
        if (heure >= 7 && heure <= 9) return 8.5 + RANDOM.nextDouble() * 3;
        if (heure >= 12 && heure <= 14) return 5.0 + RANDOM.nextDouble() * 2;
        if (heure >= 18 && heure <= 21) return 9.0 + RANDOM.nextDouble() * 4;
        if (heure >= 0 && heure <= 5) return 1.0 + RANDOM.nextDouble() * 0.5;
        return 3.0 + RANDOM.nextDouble() * 2;
    }

    /**
     * Simule la consommation d'eau avec des pics de début et fin de journée.
     *
     * @param heure L'heure de la journée
     * @return La quantité en m³
     */
    private static double genererEau(int heure) {
        if (heure >= 6 && heure <= 9) return 0.5 + RANDOM.nextDouble() * 0.3;
        if (heure >= 19 && heure <= 22) return 0.4 + RANDOM.nextDouble() * 0.2;
        if (heure >= 0 && heure <= 5) return 0.02 + RANDOM.nextDouble() * 0.01;
        return 0.15 + RANDOM.nextDouble() * 0.1;
    }

    /**
     * Simule la consommation de gaz avec saisonnalité (hiver > été).
     *
     * @param heure L'heure de la journée
     * @param mois  Le mois (1-12)
     * @return La quantité en m³
     */
    private static double genererGaz(int heure, int mois) {
        // Facteur saisonnier : maximum en hiver, minimum en été
        double facteurSaisonnier = estEnHiver(mois) ? 2.0 : 0.3;

        if (heure >= 6 && heure <= 9) return 1.2 * facteurSaisonnier;
        if (heure >= 18 && heure <= 22) return 1.0 * facteurSaisonnier;
        return 0.3 * facteurSaisonnier;
    }

    /**
     * Simule la consommation de chauffage (uniquement en hiver).
     *
     * @param heure L'heure de la journée
     * @param mois  Le mois (1-12)
     * @return La quantité en kWh
     */
    private static double genererChauffage(int heure, int mois) {
        if (!estEnHiver(mois)) return 0.1 + RANDOM.nextDouble() * 0.05;
        if (heure >= 6 && heure <= 22) return 5.0 + RANDOM.nextDouble() * 2.5;
        return 2.0 + RANDOM.nextDouble() * 1.0;
    }

    /**
     * Simule la consommation de climatisation (uniquement en été).
     *
     * @param heure L'heure de la journée
     * @param mois  Le mois (1-12)
     * @return La quantité en kWh
     */
    private static double genererClimatisation(int heure, int mois) {
        if (!estEnEte(mois)) return 0.05 + RANDOM.nextDouble() * 0.02;
        if (heure >= 11 && heure <= 19) return 4.0 + RANDOM.nextDouble() * 2.0;
        return 1.0 + RANDOM.nextDouble() * 0.5;
    }

    /**
     * Détermine si le mois donné est en période hivernale (oct-mar).
     *
     * @param mois Le mois (1-12)
     * @return true si le mois est en hiver
     */
    private static boolean estEnHiver(int mois) {
        return mois == 12 || mois == 1 || mois == 2 || mois == 3 || mois == 10 || mois == 11;
    }

    /**
     * Détermine si le mois donné est en période estivale (juin-sep).
     *
     * @param mois Le mois (1-12)
     * @return true si le mois est en été
     */
    private static boolean estEnEte(int mois) {
        return mois >= 6 && mois <= 9;
    }
}
