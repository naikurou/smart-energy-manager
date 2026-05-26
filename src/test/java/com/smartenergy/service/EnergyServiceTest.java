package com.smartenergy.service;

import com.smartenergy.dao.DatabaseManager;
import com.smartenergy.model.EnergyRecord;
import com.smartenergy.model.TypeEnergie;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnergyServiceTest {

    private EnergyService service;

    @BeforeAll
    static void initDatabase() {
        DatabaseManager.setDatabaseUrl("jdbc:sqlite::memory:");
        DatabaseManager.getInstance().initialiserBase();
    }

    @BeforeEach
    void setUp() {
        service = new EnergyService();
    }

    @Test
    @Order(1)
    @DisplayName("Calcul automatique du coût")
    void testCalculCoutAutomatique() {
        // 50 kWh × prix unitaire élec → on vérifie le calcul auto
        EnergyRecord record = new EnergyRecord(1,
            LocalDateTime.now().minusHours(1),
            TypeEnergie.ELECTRICITE, 50.0, "MANUEL");

        double coutAttendu = 50.0 * TypeEnergie.ELECTRICITE.getPrixUnitaire();
        assertEquals(coutAttendu, record.getCoutEstime(), 0.001,
            "Le coût doit être calculé automatiquement comme quantité × prix unitaire");
    }

    @Test
    @Order(2)
    @DisplayName("Rejeter une quantité négative")
    void testQuantiteNegativeRejetee() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(1);
        record.setDateHeure(LocalDateTime.now().minusHours(1));
        record.setTypeEnergie(TypeEnergie.EAU);
        record.setQuantite(-5.0);

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Une quantité négative doit être rejetée");
    }

    @Test
    @Order(3)
    @DisplayName("Rejeter un enregistrement sans bâtiment")
    void testSansBatimentRejete() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(0);
        record.setDateHeure(LocalDateTime.now().minusHours(1));
        record.setTypeEnergie(TypeEnergie.GAZ);
        record.setQuantite(10.0);

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Un enregistrement sans bâtiment valide doit être rejeté");
    }

    @Test
    @Order(4)
    @DisplayName("Rejeter une date dans le futur")
    void testDateFutureRejetee() {
        EnergyRecord record = new EnergyRecord();
        record.setBatimentId(1);
        record.setDateHeure(LocalDateTime.now().plusDays(1));
        record.setTypeEnergie(TypeEnergie.CHAUFFAGE);
        record.setQuantite(25.0);

        assertThrows(IllegalArgumentException.class,
            () -> service.enregistrer(record),
            "Une date dans le futur doit être rejetée");
    }

    @Test
    @Order(5)
    @DisplayName("Prix unitaires positifs pour tous les types")
    void testPrixUnitairesPositifs() {
        for (TypeEnergie type : TypeEnergie.values()) {
            assertTrue(type.getPrixUnitaire() > 0,
                "Le prix unitaire de " + type.getLibelle() + " doit être positif");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Recalcul du coût après modification de quantité")
    void testRecalculCout() {
        EnergyRecord record = new EnergyRecord();
        record.setTypeEnergie(TypeEnergie.EAU);
        record.setQuantite(10.0);
        record.recalculerCout();
        double coutInitial = record.getCoutEstime();

        // On double la quantité → le coût doit suivre
        record.setQuantite(20.0);
        double coutNouveau = record.getCoutEstime();

        assertEquals(coutInitial * 2, coutNouveau, 0.001,
            "Le coût doit doubler quand la quantité double");
    }

    @Test
    @Order(7)
    @DisplayName("Énergie dominante null sans données")
    void testEnergieDominanteVide() {
        TypeEnergie dominante = service.getEnergieDominante(99999);
        assertNull(dominante, "Sans données, l'énergie dominante doit être null");
    }
}
