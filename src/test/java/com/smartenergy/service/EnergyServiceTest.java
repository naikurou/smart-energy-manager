package com.smartenergy.service;

import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service de gestion des consommations énergétiques.
 * Vérifie la validation des enregistrements et le calcul automatique des coûts.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnergyServiceTest {

    private EnergyService service;

    @BeforeEach
    void setUp() {
        service = new EnergyService();
    }

    /**
     * Vérifie que le calcul automatique du coût est correct.
     */
    @Test
    @Order(1)
    @DisplayName("Calcul automatique du coût")
    void testCalculCoutAutomatique() {
        // 50 kWh d'électricité à 0.18 €/kWh = 9.00 €
        EnergyRecord record = new EnergyRecord(1,
            LocalDateTime.now().minusHours(1),
            TypeEnergie.ELECTRICITE, 50.0, "MANUEL");

        double coutAttendu = 50.0 * TypeEnergie.ELECTRICITE.getPrixUnitaire();
        assertEquals(coutAttendu, record.getCoutEstime(), 0.001,
            "Le coût doit être calculé automatiquement comme quantité × prix unitaire");
    }

    /**
     * Vérifie qu'un enregistrement avec quantité négative est rejeté.
     */
    @Test
    @Order(2)
    @DisplayName("Rejeter une quantité négative")
    void testQuantiteNegativeRejetee() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(1);
        record.setDateHeure(LocalDateTime.now().minusHours(1));
        record.setTypeEnergie(TypeEnergie.EAU);
        record.setQuantite(-5.0);  // Invalide

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Une quantité négative doit être rejetée");
    }

    /**
     * Vérifie qu'un enregistrement sans bâtiment est rejeté.
     */
    @Test
    @Order(3)
    @DisplayName("Rejeter un enregistrement sans bâtiment")
    void testSansBatimentRejete() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(0);  // ID invalide
        record.setDateHeure(LocalDateTime.now().minusHours(1));
        record.setTypeEnergie(TypeEnergie.GAZ);
        record.setQuantite(10.0);

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Un enregistrement sans bâtiment valide doit être rejeté");
    }

    /**
     * Vérifie qu'une date dans le futur est rejetée.
     */
    @Test
    @Order(4)
    @DisplayName("Rejeter une date dans le futur")
    void testDateFutureRejetee() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(1);
        record.setDateHeure(LocalDateTime.now().plusDays(1));  // Futur
        record.setTypeEnergie(TypeEnergie.CHAUFFAGE);
        record.setQuantite(25.0);

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Une date dans le futur doit être rejetée");
    }

    /**
     * Vérifie que le prix unitaire de chaque type d'énergie est positif.
     */
    @Test
    @Order(5)
    @DisplayName("Prix unitaires positifs pour tous les types")
    void testPrixUnitairesPositifs() {
        for (TypeEnergie type : TypeEnergie.values()) {
            assertTrue(type.getPrixUnitaire() > 0,
                "Le prix unitaire de " + type.getLibelle() + " doit être positif");
        }
    }

    /**
     * Vérifie que le recalcul du coût fonctionne après modification.
     */
    @Test
    @Order(6)
    @DisplayName("Recalcul du coût après modification de quantité")
    void testRecalculCout() {
        EnergyRecord record = new EnergyRecord();
        record.setTypeEnergie(TypeEnergie.EAU);
        record.setQuantite(10.0);
        record.recalculerCout();

        double coutInitial = record.getCoutEstime();

        // Modifier la quantité
        record.setQuantite(20.0);  // Le setter doit recalculer automatiquement
        double coutNouveau = record.getCoutEstime();

        assertEquals(coutInitial * 2, coutNouveau, 0.001,
            "Le coût doit doubler quand la quantité double");
    }

    /**
     * Vérifie que getEnergieDominante() retourne null sans données.
     */
    @Test
    @Order(7)
    @DisplayName("Énergie dominante null sans données")
    void testEnergieDominanteVide() {
        TypeEnergie dominante = service.getEnergieDominante(99999);  // ID inexistant
        assertNull(dominante, "Sans données, l'énergie dominante doit être null");
    }
}
