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

/**
 * Service métier pour la gestion des consommations énergétiques.
 * Centralise la logique d'analyse, de détection d'anomalies,
 * et de calcul des indicateurs de performance.
 */
public class EnergyService {

    /** DAO pour l'accès aux données de consommation */
    private final EnergyRecordDAO recordDAO;

    /** Multiplicateur pour le seuil d'anomalie (ex: 2.0 = 200% de la moyenne) */
    private static final double SEUIL_ANOMALIE = 2.0;

    /**
     * Constructeur — instancie le DAO associé.
     */
    public EnergyService() {
        this.recordDAO = new EnergyRecordDAO();
    }

    /**
     * Enregistre une nouvelle mesure de consommation.
     *
     * @param record L'enregistrement à sauvegarder
     * @return true si l'enregistrement a réussi
     * @throws IllegalArgumentException si les données sont invalides
     */
    public boolean enregistrer(EnergyRecord record) {
        valider(record);
        return recordDAO.inserer(record);
    }

    /**
     * Importe une liste d'enregistrements issus d'un fichier CSV.
     * Utilise l'insertion en lot pour de meilleures performances.
     *
     * @param records Les enregistrements à importer
     * @return Le nombre d'enregistrements importés avec succès
     */
    public int importerEnLot(List<EnergyRecord> records) {
        // Filtrage des enregistrements invalides avant l'import
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

    /**
     * Supprime un enregistrement de consommation.
     *
     * @param id L'identifiant de l'enregistrement à supprimer
     * @return true si la suppression a réussi
     */
    public boolean supprimer(int id) {
        return recordDAO.supprimer(id);
    }

    /**
     * Récupère tous les enregistrements d'un bâtiment.
     *
     * @param batimentId L'identifiant du bâtiment
     * @return La liste des enregistrements du bâtiment
     */
    public List<EnergyRecord> getRecordsBatiment(int batimentId) {
        return recordDAO.trouverParBatiment(batimentId);
    }

    /**
     * Récupère les enregistrements d'un bâtiment sur une période.
     *
     * @param batimentId L'identifiant du bâtiment
     * @param debut      Date de début
     * @param fin        Date de fin
     * @return Les enregistrements de la période
     */
    public List<EnergyRecord> getRecordsPeriode(int batimentId,
                                                  LocalDateTime debut,
                                                  LocalDateTime fin) {
        return recordDAO.trouverParBatimentEtPeriode(batimentId, debut, fin);
    }

    /**
     * Calcule la consommation totale du jour en cours pour tous les bâtiments.
     *
     * @return La consommation totale en unités composites
     */
    public double getConsommationJour() {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);
        return getTotalGlobal(debut, fin);
    }

    /**
     * Calcule la consommation totale du mois en cours.
     *
     * @return La consommation totale mensuelle
     */
    public double getConsommationMois() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return getTotalGlobal(debut.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    /**
     * Calcule la consommation totale de l'année en cours.
     *
     * @return La consommation totale annuelle
     */
    public double getConsommationAnnee() {
        LocalDate debut = LocalDate.now().withDayOfYear(1);
        LocalDate fin = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        return getTotalGlobal(debut.atStartOfDay(), fin.atTime(23, 59, 59));
    }

    /**
     * Estime la facture mensuelle d'un bâtiment basée sur les 30 derniers jours.
     *
     * @param batimentId L'identifiant du bâtiment
     * @return Le coût mensuel estimé en euros
     */
    public double estimerFactureMensuelle(int batimentId) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime debut = fin.minusDays(30);
        return recordDAO.getCoutTotalBatiment(batimentId, debut, fin);
    }

    /**
     * Identifie les pics de consommation dans l'historique d'un bâtiment.
     * Un pic est défini comme une valeur dépassant SEUIL_ANOMALIE × la moyenne.
     *
     * @param batimentId L'identifiant du bâtiment
     * @return La liste des enregistrements constituant des pics
     */
    public List<EnergyRecord> detecterPics(int batimentId) {
        List<EnergyRecord> tous = recordDAO.trouverParBatiment(batimentId);
        if (tous.isEmpty()) return Collections.emptyList();

        // Calcul de la moyenne globale
        double moyenne = tous.stream()
            .mapToDouble(EnergyRecord::getQuantite)
            .average()
            .orElse(0.0);

        double seuil = moyenne * SEUIL_ANOMALIE;

        // Filtrage des enregistrements au-dessus du seuil
        return tous.stream()
            .filter(r -> r.getQuantite() > seuil)
            .collect(Collectors.toList());
    }

    /**
     * Détecte les anomalies de consommation pour tous les bâtiments.
     * Compare chaque enregistrement récent à la moyenne historique du même type.
     *
     * @param batiments La liste des bâtiments à analyser
     * @return La liste des alertes générées
     */
    public List<Alerte> detecterAnomalies(List<Batiment> batiments) {
        List<Alerte> alertes = new ArrayList<>();

        for (Batiment batiment : batiments) {
            List<EnergyRecord> records = recordDAO.trouverParBatiment(batiment.getId());

            // Analyse par type d'énergie
            for (TypeEnergie type : TypeEnergie.values()) {
                List<EnergyRecord> parType = records.stream()
                    .filter(r -> r.getTypeEnergie() == type)
                    .collect(Collectors.toList());

                if (parType.size() < 3) continue; // Pas assez de données

                // Calcul de la moyenne et de l'écart-type
                double moyenne = parType.stream()
                    .mapToDouble(EnergyRecord::getQuantite)
                    .average().orElse(0);

                double variance = parType.stream()
                    .mapToDouble(r -> Math.pow(r.getQuantite() - moyenne, 2))
                    .average().orElse(0);

                double ecartType = Math.sqrt(variance);
                double seuil = moyenne + (2 * ecartType); // Règle des 2 sigmas

                // Vérification du dernier enregistrement
                EnergyRecord dernier = parType.get(parType.size() - 1);
                if (dernier.getQuantite() > seuil && seuil > 0) {
                    String msg = String.format(
                        "⚠ Pic de %s détecté pour '%s' : %.1f %s (moyenne: %.1f)",
                        type.getLibelle(), batiment.getNom(),
                        dernier.getQuantite(), type.getUnite(), moyenne
                    );

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

    /**
     * Détermine le type d'énergie dominant pour un bâtiment (le plus consommé en coût).
     *
     * @param batimentId L'identifiant du bâtiment
     * @return Le TypeEnergie dominant, ou null si aucune donnée
     */
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

    /**
     * Calcule la tendance de consommation (hausse ou baisse) pour un bâtiment.
     * Compare la moyenne des 30 derniers jours à celle des 30 jours précédents.
     *
     * @param batimentId L'identifiant du bâtiment
     * @return Un pourcentage d'évolution (positif = hausse, négatif = baisse)
     */
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

    /**
     * Calcule le total de consommation global sur une période.
     *
     * @param debut Date de début
     * @param fin   Date de fin
     * @return La somme totale des quantités consommées
     */
    private double getTotalGlobal(LocalDateTime debut, LocalDateTime fin) {
        Map<TypeEnergie, Double> totaux = recordDAO.getTotauxParEnergie(debut, fin);
        return totaux.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Valide un enregistrement de consommation avant insertion.
     *
     * @param record L'enregistrement à valider
     * @throws IllegalArgumentException si les données sont invalides
     */
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
