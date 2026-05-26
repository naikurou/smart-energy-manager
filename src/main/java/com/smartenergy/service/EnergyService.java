package com.smartenergy.service;

import com.smartenergy.dao.EnergyRecordDAO;
import com.smartenergy.model.Alerte;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EnergyService {

    private final EnergyRecordDAO recordDAO;

    // Seuil pour détecter les anomalies (2x la moyenne)
    private static final double SEUIL_ANOMALIE = 2.0;

    public EnergyService() {
        this.recordDAO = new EnergyRecordDAO();
    }

    public boolean enregistrer(EnergyRecord record) {
        valider(record);
        return recordDAO.inserer(record);
    }

    // Import batch — on filtre les invalides avant d'insérer en lot
    public int importerEnLot(List<EnergyRecord> records) {
        List<EnergyRecord> valides = records.stream()
            .filter(r -> {
                try {
                    valider(r);
                    return true;
                } catch (IllegalArgumentException e) {
                    System.err.println("Enregistrement ignoré (invalide) : " + e.getMessage());
                    return false;
                }
            })
            .collect(Collectors.toList());
        return recordDAO.insererEnLot(valides);
    }

    public boolean supprimer(int id) {
        return recordDAO.supprimer(id);
    }

    public List<EnergyRecord> getRecordsBatiment(int batimentId) {
        return recordDAO.trouverParBatiment(batimentId);
    }

    public List<EnergyRecord> getRecordsPeriode(int batimentId,
                                                  LocalDateTime debut,
                                                  LocalDateTime fin) {
        return recordDAO.trouverParBatimentEtPeriode(batimentId, debut, fin);
    }

    public double getConsommationJour() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);
        return getTotalGlobal(debut, fin);
    }

    public double getConsommationMois() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return getTotalGlobal(debut.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    public double getConsommationAnnee() {
        LocalDate debut = LocalDate.now().withDayOfYear(1);
        LocalDate fin = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        return getTotalGlobal(debut.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    // Estimation basée sur les 30 derniers jours
    public double estimerFactureMensuelle(int batimentId) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusDays(30);
        return recordDAO.getCoutTotalBatiment(batimentId, debut, fin);
    }

    // Détection des pics : tout ce qui dépasse SEUIL_ANOMALIE × la moyenne
    public List<EnergyRecord> detecterPics(int batimentId) {
        List<EnergyRecord> tous = recordDAO.trouverParBatiment(batimentId);
        if (tous.isEmpty()) return Collections.emptyList();

        double moyenne = tous.stream()
            .mapToDouble(EnergyRecord::getQuantite)
            .average()
            .orElse(0.0);
        double seuil = moyenne * SEUIL_ANOMALIE;

        return tous.stream()
            .filter(r -> r.getQuantite() > seuil)
            .collect(Collectors.toList());
    }

    // Détection d'anomalies avec la règle des 2 sigmas par type d'énergie
    public List<Alerte> detecterAnomalies(List<Batiment> batiments) {
        List<Alerte> alertes = new ArrayList<>();

        for (Batiment batiment : batiments) {
            List<EnergyRecord> records = recordDAO.trouverParBatiment(batiment.getId());

            for (TypeEnergie type : TypeEnergie.values()) {
                List<EnergyRecord> parType = records.stream()
                    .filter(r -> r.getTypeEnergie() == type)
                    .collect(Collectors.toList());

                if (parType.size() < 3) continue;

                double moyenne = parType.stream()
                    .mapToDouble(EnergyRecord::getQuantite)
                    .average().orElse(0);

                double variance = parType.stream()
                    .mapToDouble(r -> Math.pow(r.getQuantite() - moyenne, 2))
                    .average().orElse(0);

                double ecartType = Math.sqrt(variance);
                double seuil = moyenne + (2 * ecartType);

                EnergyRecord dernier = parType.get(parType.size() - 1);
                if (dernier.getQuantite() > seuil && seuil > 0) {
                    String msg = String.format(
                        "⚠ Pic de %s détecté pour '%s' : %.1f %s (moyenne: %.1f)",
                        type.getLibelle(), batiment.getNom(),
                        dernier.getQuantite(), type.getUnite(), moyenne
                    );

                    // > 3x la moyenne → critique, sinon avertissement
                    Alerte.NiveauAlerte niveau = dernier.getQuantite() > (moyenne * 3)
                        ? Alerte.NiveauAlerte.CRITIQUE
                        : Alerte.NiveauAlerte.AVERTISSEMENT;

                    alertes.add(new Alerte(msg, batiment, type, niveau,
                        dernier.getQuantite(), seuil));
                }
            }
        }

        return alertes;
    }

    // Trouve le type d'énergie le plus cher pour un bâtiment
    public TypeEnergie getEnergieDominante(int batimentId) {
        List<EnergyRecord> records = recordDAO.trouverParBatiment(batimentId);

        return records.stream()
            .collect(Collectors.groupingBy(
                EnergyRecord::getTypeEnergie,
                Collectors.summingDouble(EnergyRecord::getCoutEstime)
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    // Tendance : compare la moyenne des 30 derniers jours vs les 30 d'avant
    public double calculerTendance(int batimentId) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime il30Jours = maintenant.minusDays(30);
        LocalDateTime il60Jours = maintenant.minusDays(60);

        List<EnergyRecord> periode1 = recordDAO.trouverParBatimentEtPeriode(
            batimentId, il60Jours, il30Jours);
        List<EnergyRecord> periode2 = recordDAO.trouverParBatimentEtPeriode(
            batimentId, il30Jours, maintenant);

        double moy1 = periode1.stream().mapToDouble(EnergyRecord::getQuantite).average().orElse(0);
        double moy2 = periode2.stream().mapToDouble(EnergyRecord::getQuantite).average().orElse(0);

        if (moy1 == 0) return 0;
        return ((moy2 - moy1) / moy1) * 100;
    }

    private double getTotalGlobal(LocalDateTime debut, LocalDateTime fin) {
        Map<TypeEnergie, Double> totaux = recordDAO.getTotauxParEnergie(debut, fin);
        return totaux.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private void valider(EnergyRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("L'enregistrement ne peut pas être null.");
        }
        if (record.getBatimentId() <= 0) {
            throw new IllegalArgumentException("Un bâtiment valide est requis.");
        }
        if (record.getTypeEnergie() == null) {
            throw new IllegalArgumentException("Le type d'énergie est obligatoire.");
        }
        if (record.getQuantite() < 0) {
            throw new IllegalArgumentException("La quantité consommée ne peut pas être négative.");
        }
        if (record.getDateHeure() == null) {
            throw new IllegalArgumentException("La date/heure de la mesure est obligatoire.");
        }
        if (record.getDateHeure().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("La date de mesure ne peut pas être dans le futur.");
        }
    }
}
