package com.smartenergy.service;

import com.smartenergy.dao.DatabaseManager;
import com.smartenergy.model.Batiment;
import com.smartenergy.model.TypeBatiment;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service de gestion des bâtiments.
 * Vérifie les règles de validation métier de BatimentService.
 *
 * <p>Note : ces tests utilisent une base SQLite temporaire en mémoire
 * pour isoler les tests de la base de production.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BatimentServiceTest {

    private BatimentService service;

    @BeforeAll
    static void initDatabase() {
        DatabaseManager.setDatabaseUrl("jdbc:sqlite::memory:");
        DatabaseManager.getInstance().initialiserBase();
    }

    @BeforeEach
    void setUp() {
        service = new BatimentService();
    }

    /**
     * Vérifie qu'un bâtiment valide peut être créé sans erreur.
     */
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

        // La méthode creer() ne doit pas lancer d'exception
        assertDoesNotThrow(() -> service.creer(batiment),
            "La création d'un bâtiment valide ne doit pas échouer");

        // L'ID doit être assigné après insertion
        assertTrue(batiment.getId() > 0,
            "L'ID doit être positif après insertion en base");
    }

    /**
     * Vérifie qu'un bâtiment sans nom est rejeté par la validation.
     */
    @Test
    @Order(2)
    @DisplayName("Rejeter un bâtiment sans nom")
    void testCreerBatimentSansNom() {
        Batiment batiment = new Batiment(
            "",   // Nom vide — invalide
            "12 Rue de la Paix",
            TypeBatiment.MAISON,
            100.0, 2, 2000, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Un nom vide doit lever une IllegalArgumentException");
    }

    /**
     * Vérifie qu'un bâtiment sans type est rejeté.
     */
    @Test
    @Order(3)
    @DisplayName("Rejeter un bâtiment sans type")
    void testCreerBatimentSansType() {
        Batiment batiment = new Batiment(
            "Bureau Principal",
            "1 Avenue des Champs",
            null,   // Type null — invalide
            200.0, 10, 2015, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Un type null doit lever une IllegalArgumentException");
    }

    /**
     * Vérifie que la superficie négative est rejetée.
     */
    @Test
    @Order(4)
    @DisplayName("Rejeter une superficie négative")
    void testSuperficieNegative() {
        Batiment batiment = new Batiment(
            "Maison Test", "Adresse", TypeBatiment.MAISON,
            -10.0,   // Superficie négative — invalide
            2, 2000, ""
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.creer(batiment),
            "Une superficie négative doit lever une IllegalArgumentException");
    }

    /**
     * Vérifie la fonctionnalité de clonage d'un bâtiment.
     */
    @Test
    @Order(5)
    @DisplayName("Cloner un bâtiment avec succès")
    void testClonerBatiment() {
        // Création du bâtiment original
        Batiment original = new Batiment(
            "Original", "Adresse", TypeBatiment.BUREAU,
            150.0, 5, 2012, "Original"
        );
        service.creer(original);

        // Clonage
        Batiment clone = service.cloner(original, "Copie de l'original");

        assertNotNull(clone, "Le clone ne doit pas être null");
        assertEquals("Copie de l'original", clone.getNom(),
            "Le nom du clone doit correspondre");
        assertEquals(original.getType(), clone.getType(),
            "Le type du clone doit être identique à l'original");
        assertNotEquals(original.getId(), clone.getId(),
            "Les IDs de l'original et du clone doivent être différents");
    }

    /**
     * Vérifie que le clonage avec un nom vide est rejeté.
     */
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

    /**
     * Vérifie que la liste des bâtiments est bien retournée.
     */
    @Test
    @Order(7)
    @DisplayName("Récupérer la liste des bâtiments")
    void testTrouverTous() {
        var liste = service.trouverTous();
        assertNotNull(liste, "La liste ne doit pas être null");
    }
}
