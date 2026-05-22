package com.smartenergy.util;

import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire pour l'import et l'export de données de consommation au format CSV.
 *
 * <p>Format CSV attendu pour l'import (séparateur : virgule) :
 * <pre>batiment_id,date_heure,type_energie,quantite,note</pre>
 * Exemple :
 * <pre>1,2024-03-15 08:30:00,ELECTRICITE,45.2,Mesure matinale</pre>
 * </p>
 *
 * <p>Le coût estimé est calculé automatiquement depuis le type d'énergie.</p>
 */
public class CsvParser {

    /** En-tête du fichier CSV d'export */
    private static final String ENTETE_CSV =
        "batiment_id,date_heure,type_energie,quantite,cout_estime,source,note";

    /** Format de date attendu dans le CSV */
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Importe des enregistrements depuis un fichier CSV.
     *
     * <p>Les lignes mal formatées ou avec des valeurs invalides sont ignorées
     * et un message d'erreur est affiché pour chacune.</p>
     *
     * @param fichier Le fichier CSV à importer
     * @return La liste des enregistrements valides extraits du fichier
     */
    public static List<EnergyRecord> importer(File fichier) {
        List<EnergyRecord> records = new ArrayList<>();
        int numLigne = 0;
        int erreurs = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8))) {

            String ligne;
            while ((ligne = reader.readLine()) != null) {
                numLigne++;

                // Ignorer la ligne d'en-tête et les lignes vides
                if (numLigne == 1 || ligne.isBlank() || ligne.startsWith("#")) {
                    continue;
                }

                try {
                    EnergyRecord record = parseLigne(ligne, numLigne);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.printf("Ligne %d ignorée : %s%n", numLigne, e.getMessage());
                    erreurs++;
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Fichier CSV introuvable : " + fichier.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier CSV : " + e.getMessage());
        }

        System.out.printf("Import CSV terminé : %d enregistrements valides, %d erreurs.%n",
            records.size(), erreurs);
        return records;
    }

    /**
     * Exporte une liste d'enregistrements vers un fichier CSV.
     *
     * @param records   La liste des enregistrements à exporter
     * @param fichier   Le fichier de destination
     * @return true si l'export a réussi
     */
    public static boolean exporter(List<EnergyRecord> records, File fichier) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {

            // Écriture de l'en-tête
            writer.write(ENTETE_CSV);
            writer.newLine();

            // Écriture de chaque enregistrement
            for (EnergyRecord record : records) {
                writer.write(String.format("%d,%s,%s,%.4f,%.2f,%s,%s",
                    record.getBatimentId(),
                    record.getDateHeure().format(FORMATTER),
                    record.getTypeEnergie().name(),
                    record.getQuantite(),
                    record.getCoutEstime(),
                    record.getSource() != null ? record.getSource() : "MANUEL",
                    record.getNote() != null ? record.getNote().replace(",", ";") : ""
                ));
                writer.newLine();
            }

            System.out.println("✓ Export CSV réussi : " + records.size() + " enregistrements.");
            return true;

        } catch (IOException e) {
            System.err.println("Erreur lors de l'export CSV : " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse une ligne CSV en objet EnergyRecord.
     * Format attendu : batiment_id,date_heure,type_energie,quantite[,note]
     *
     * @param ligne    La ligne CSV à parser
     * @param numLigne Le numéro de la ligne pour les messages d'erreur
     * @return L'EnergyRecord correspondant
     * @throws IllegalArgumentException si le format est invalide
     */
    private static EnergyRecord parseLigne(String ligne, int numLigne) {
        String[] parties = ligne.split(",", -1);

        if (parties.length < 4) {
            throw new IllegalArgumentException(
                "Format insuffisant (attendu: batiment_id,date,type,quantite)");
        }

        try {
            int batimentId = Integer.parseInt(parties[0].trim());
            LocalDateTime dateHeure = LocalDateTime.parse(parties[1].trim(), FORMATTER);
            TypeEnergie typeEnergie = TypeEnergie.valueOf(parties[2].trim().toUpperCase());
            double quantite = Double.parseDouble(parties[3].trim());

            if (batimentId <= 0) {
                throw new IllegalArgumentException("L'ID du bâtiment doit être positif");
            }
            if (quantite < 0) {
                throw new IllegalArgumentException("La quantité ne peut pas être négative");
            }

            EnergyRecord record = new EnergyRecord(batimentId, dateHeure, typeEnergie,
                quantite, "CSV");

            // Note optionnelle (5ème colonne)
            if (parties.length >= 5 && !parties[4].isBlank()) {
                record.setNote(parties[4].trim());
            }

            return record;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur numérique invalide : " + e.getMessage());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format de date invalide (attendu: yyyy-MM-dd HH:mm:ss)");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No enum constant")) {
                throw new IllegalArgumentException(
                    "Type d'énergie inconnu. Valeurs acceptées : " +
                    "ELECTRICITE, EAU, GAZ, CHAUFFAGE, CLIMATISATION");
            }
            throw e;
        }
    }
}
