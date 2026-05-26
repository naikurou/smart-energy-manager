package com.smartenergy.service;

import com.smartenergy.dao.DatabaseManager;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BatimentServiceTest {

    private BatimentService service;

    // Base SQLite en mémoire pour isoler les tests
    @BeforeAll
    static void initDatabase() {
        DatabaseManager.setDatabaseUrl("jdbc:sqlite::memory:");
        DatabaseManager.getInstance().initialiserBase();
    }

    @BeforeEach
    void setUp() {
        service = new BatimentService();
    }

    @Test
    @Order(1)
    @DisplayName("Créer un bâtiment valide")
    void testCreerBatimentValide() {
        Batiment batiment = new Batiment(
            "Résidence Test",
            "12 Rue de la Paix",
            TypeBatiment.APPARTEMENT,
            85.5,
            3,
            2010,
            "Test unitaire"
        );

        assertDoesNotThrow(() -> service.creer(batiment),
            "La création d'un bâtiment valide ne doit pas échouer");
        assertTrue(batiment.getId() > 0,
            "L'ID doit être positif après insertion en base");
    }

    @Test
    @Order(2)
    @DisplayName("Rejeter un bâtiment sans nom")
    void testCreerBatimentSansNom() {
        Batiment batiment = new Batiment(
            "", "12 Rue de la Paix",
            TypeBatiment.MAISON,
            100.0, 2, 2000, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Un nom vide doit lever une IllegalArgumentException");
    }

    @Test
    @Order(3)
    @DisplayName("Rejeter un bâtiment sans type")
    void testCreerBatimentSansType() {
        Batiment batiment = new Batiment(
            "Bureau Principal",
            "1 Avenue des Champs",
            null,
            200.0, 10, 2015, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Un type null doit lever une IllegalArgumentException");
    }

    @Test
    @Order(4)
    @DisplayName("Rejeter une superficie négative")
    void testSuperficieNegative() {
        Batiment batiment = new Batiment(
            "Maison Test", "Adresse", TypeBatiment.MAISON,
            -10.0, 2, 2000, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Une superficie négative doit lever une IllegalArgumentException");
    }

    @Test
    @Order(5)
    @DisplayName("Cloner un bâtiment avec succès")
    void testClonerBatiment() {
        Batiment original = new Batiment(
            "Original", "Adresse", TypeBatiment.BUREAU,
            150.0, 5, 2012, "Original"
        );
        service.creer(original);

        Batiment clone = service.cloner(original, "Copie de l'original");

        assertNotNull(clone, "Le clone ne doit pas être null");
        assertEquals("Copie de l'original", clone.getNom(),
            "Le nom du clone doit correspondre");
        assertEquals(original.getType(), clone.getType(),
            "Le type du clone doit être identique à l'original");
        assertNotEquals(original.getId(), clone.getId(),
            "Les IDs de l'original et du clone doivent être différents");
    }

    @Test
    @Order(6)
    @DisplayName("Rejeter le clonage avec nom vide")
    void testClonerAvecNomVide() {
        Batiment original = new Batiment(
            "Test Clone", "Adresse", TypeBatiment.MAISON,
            80.0, 2, 2005, ""
        );
        service.creer(original);

        assertThrows(IllegalArgumentException.class,
            () -> service.cloner(original, ""),
            "Un nom de clone vide doit lever une exception");
    }

    @Test
    @Order(7)
    @DisplayName("Récupérer la liste des bâtiments")
    void testTrouverTous() {
        var liste = service.trouverTous();
        assertNotNull(liste, "La liste ne doit pas être null");
    }
}
