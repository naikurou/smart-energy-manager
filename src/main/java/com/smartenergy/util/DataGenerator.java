package com.smartenergy.util;

import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Génère des données réalistes avec variations horaires et saisonnières
public class DataGenerator {

    // Graine fixe pour que les données soient reproductibles
    private static final Random RANDOM = new Random(42);

    public static List<EnergyRecord> generer(int batimentId,
                                              int nombreJours,
                                              int mesuresParJour) {
        List<EnergyRecord> records = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();

        for (int j = nombreJours - 1; j >= 0; j--) {
            LocalDateTime jourBase = maintenant.minusDays(j);

            for (int m = 0; m < mesuresParJour; m++) {
                int heure = (24 / mesuresParJour) * m;
                LocalDateTime dateHeure = jourBase.withHour(heure).withMinute(0).withSecond(0);

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

    // Quantité de base selon le type + variation aléatoire ±15%
    private static double genererQuantite(TypeEnergie type, int heure, int mois) {
        double base = switch (type) {
            case ELECTRICITE -> genererElectricite(heure);
            case EAU         -> genererEau(heure);
            case GAZ         -> genererGaz(heure, mois);
            case CHAUFFAGE   -> genererChauffage(heure, mois);
            case CLIMATISATION -> genererClimatisation(heure, mois);
        };
        double variation = 1.0 + (RANDOM.nextDouble() * 0.30 - 0.15);
        return Math.max(0, Math.round(base * variation * 100.0) / 100.0);
    }

    // Élec : pics matin (7-9h) et soir (18-21h)
    private static double genererElectricite(int heure) {
        if (heure >= 7 && heure <= 9) return 8.5 + RANDOM.nextDouble() * 3;
        if (heure >= 12 && heure <= 14) return 5.0 + RANDOM.nextDouble() * 2;
        if (heure >= 18 && heure <= 21) return 9.0 + RANDOM.nextDouble() * 4;
        if (heure >= 0 && heure <= 5) return 1.0 + RANDOM.nextDouble() * 0.5;
        return 3.0 + RANDOM.nextDouble() * 2;
    }

    // Eau : pics matin et soir
    private static double genererEau(int heure) {
        if (heure >= 6 && heure <= 9) return 0.5 + RANDOM.nextDouble() * 0.3;
        if (heure >= 19 && heure <= 22) return 0.4 + RANDOM.nextDouble() * 0.2;
        if (heure >= 0 && heure <= 5) return 0.02 + RANDOM.nextDouble() * 0.01;
        return 0.15 + RANDOM.nextDouble() * 0.1;
    }

    // Gaz : facteur saisonnier (x2 en hiver, x0.3 en été)
    private static double genererGaz(int heure, int mois) {
        double facteurSaisonnier = estEnHiver(mois) ? 2.0 : 0.3;
        if (heure >= 6 && heure <= 9) return 1.2 * facteurSaisonnier;
        if (heure >= 18 && heure <= 22) return 1.0 * facteurSaisonnier;
        return 0.3 * facteurSaisonnier;
    }

    // Chauffage quasi nul en été, actif en hiver
    private static double genererChauffage(int heure, int mois) {
        if (!estEnHiver(mois)) return 0.1 + RANDOM.nextDouble() * 0.05;
        if (heure >= 6 && heure <= 22) return 5.0 + RANDOM.nextDouble() * 2.5;
        return 2.0 + RANDOM.nextDouble() * 1.0;
    }

    // Clim quasi nulle en hiver, active en été (pic 11-19h)
    private static double genererClimatisation(int heure, int mois) {
        if (!estEnEte(mois)) return 0.05 + RANDOM.nextDouble() * 0.02;
        if (heure >= 11 && heure <= 19) return 4.0 + RANDOM.nextDouble() * 2.0;
        return 1.0 + RANDOM.nextDouble() * 0.5;
    }

    private static boolean estEnHiver(int mois) {
        return mois == 12 || mois == 1 || mois == 2 || mois == 3 || mois == 10 || mois == 11;
    }

    private static boolean estEnEte(int mois) {
        return mois >= 6 && mois <= 9;
    }
}
