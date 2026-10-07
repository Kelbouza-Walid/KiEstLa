package fr.m2.groupe7.kiestla.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModuleDetails.
 */
class ModuleDetailsTest {

    @Test
    void from_basic() {
        Module module = new Module();
        module.setId(10L);
        module.setName("Test fonctionnel");

        User refTeacher = createUser(1L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        module.setReferenceTeacher(refTeacher);

        ModuleDetails details = ModuleDetails.from(module);

        assertEquals(10L, details.id());
        assertEquals("Test fonctionnel", details.name());
        assertEquals("Fabrice Bouquet", details.referenceTeacherName());
        assertEquals(1, details.teachers().size());
        assertEquals("Fabrice", details.teachers().get(0).firstName());
        assertEquals("Bouquet", details.teachers().get(0).lastName());
        assertTrue(details.students().isEmpty());
    }

    @Test
    void from_usersSplitAndSort() {
        Module module = new Module();
        module.setId(1L);
        module.setName("Test fonctionnel");

        User student1 = createUser(1L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        User student2 = createUser(2L, "Oam", "Faivre", "nfaivre", Role.STUDENT);
        User student3 = createUser(3L, "Gaspard", "Quentin", "gquentin", Role.STUDENT);
        User teacher = createUser(4L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER);

        module.setEnrolledUsers(new HashSet<>(Set.of(student1, student2, student3, teacher)));

        ModuleDetails details = ModuleDetails.from(module);

        assertEquals(3, details.students().size());
        assertEquals(1, details.teachers().size());

        // Verify sorting by lastName, then firstName:
        // 1. Faivre Noam
        // 2. Faivre Oam
        // 3. Quentin Gaspard
        assertEquals("Noam", details.students().get(0).firstName());
        assertEquals("Faivre", details.students().get(0).lastName());

        assertEquals("Oam", details.students().get(1).firstName());
        assertEquals("Faivre", details.students().get(1).lastName());

        assertEquals("Gaspard", details.students().get(2).firstName());
        assertEquals("Quentin", details.students().get(2).lastName());
    }

    @Test
    void from_referenceTeacherNull() {
        Module module = new Module();
        module.setId(3L);
        module.setName("Architecture Multi-tiers");
        module.setReferenceTeacher(null);

        User student = createUser(1L, "Mohamed Walid", "Kelbouza", "mkelbouza", Role.STUDENT);
        module.setEnrolledUsers(new HashSet<>(Set.of(student)));

        ModuleDetails details = ModuleDetails.from(module);

        assertNull(details.referenceTeacherName());
        assertEquals(1, details.students().size());
        assertTrue(details.teachers().isEmpty());
    }

    @Test
    void from_emptyModule() {
        Module module = new Module();
        module.setId(4L);
        module.setName("Projet Ingénierie logicielle avancé");
        module.setReferenceTeacher(null);
        module.setEnrolledUsers(new HashSet<>());

        ModuleDetails details = ModuleDetails.from(module);

        assertEquals(4L, details.id());
        assertEquals("Projet Ingénierie logicielle avancé", details.name());
        assertNull(details.referenceTeacherName());
        assertTrue(details.students().isEmpty());
        assertTrue(details.teachers().isEmpty());
    }

    private User createUser(Long id, String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setRole(role);
        return user;
    }
}
