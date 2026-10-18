package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.ModuleMembers;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModuleServiceMembersTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @InjectMocks
    private ModuleService moduleService;

    @Test
    void getModuleMembers_flagsEnrolledUsersAndSkipsReference() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        User teacher = createUser(2L, "Marilleau", Role.TEACHER);
        User enrolledStudent = createUser(3L, "Faivre", Role.STUDENT);
        User otherStudent = createUser(4L, "Kacmaz", Role.STUDENT);
        Module module = createModule(reference);
        enroll(enrolledStudent, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findByRole(Role.TEACHER)).thenReturn(List.of(reference, teacher));
        when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(otherStudent, enrolledStudent));

        ModuleMembers result = moduleService.getModuleMembers(10L);

        assertEquals(1L, result.referenceTeacherId());
        assertEquals(List.of(UserSummary.from(reference), UserSummary.from(teacher)), result.referenceTeacherChoices());
        assertEquals(List.of(new ModuleMembers.Member(UserSummary.from(teacher), false)), result.teachers());
        assertEquals(List.of(
                new ModuleMembers.Member(UserSummary.from(enrolledStudent), true),
                new ModuleMembers.Member(UserSummary.from(otherStudent), false)), result.students());
    }

    @Test
    void getModuleMembers_moduleWithoutReferenceTeacher() {
        User teacher = createUser(2L, "Marilleau", Role.TEACHER);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(createModule(null)));
        when(userRepository.findByRole(Role.TEACHER)).thenReturn(List.of(teacher));
        when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of());

        ModuleMembers result = moduleService.getModuleMembers(10L);

        assertNull(result.referenceTeacherId());
        // every teacher can be checked when there is no reference teacher
        assertEquals(List.of(new ModuleMembers.Member(UserSummary.from(teacher), false)), result.teachers());
    }

    @Test
    void getModuleMembers_unknownModule() {
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.getModuleMembers(10L));
    }

    @Test
    void updateMembers_unknownReferenceTeacher() {
        User oldReference = createUser(1L, "Bouquet", Role.TEACHER);
        Module module = createModule(oldReference);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateMembers(10L, 99L, null, null));
        assertSame(oldReference, module.getReferenceTeacher());
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void getModulesForTeacher_returnsBothEnrolledAndReferenceModulesWithoutDuplicates() {
        User teacher = createUser(1L, "Turing", Role.TEACHER);
        teacher.setLogin("aturing");

        Module enrolledModule = new Module();
        enrolledModule.setId(10L);
        enrolledModule.setName("Algorithms");

        Module referenceModule = new Module();
        referenceModule.setId(20L);
        referenceModule.setName("Cryptography");
        referenceModule.setReferenceTeacher(teacher);

        Module bothModule = new Module();
        bothModule.setId(30L);
        bothModule.setName("Logic");
        bothModule.setReferenceTeacher(teacher);

        teacher.getModules().add(enrolledModule);
        teacher.getModules().add(bothModule);

        when(userRepository.findModulesByLogin("aturing")).thenReturn(Optional.of(teacher));
        when(moduleRepository.findByReferenceTeacherIdOrderByName(1L)).thenReturn(List.of(referenceModule, bothModule));

        List<fr.m2.groupe7.kiestla.dto.ModuleSummary> result = moduleService.getModulesForTeacher("aturing");

        assertEquals(3, result.size());
        List<String> names = result.stream().map(fr.m2.groupe7.kiestla.dto.ModuleSummary::name).toList();
        assertEquals(List.of("Algorithms", "Cryptography", "Logic"), names);
    }

    @Test
    void updateMembers_enrollsSelectedAndUnenrollsOthers() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        User oldTeacher = createUser(2L, "Marilleau", Role.TEACHER);
        User newStudent = createUser(3L, "Faivre", Role.STUDENT);
        Module module = createModule(reference);
        enroll(oldTeacher, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(userRepository.findAllById(any())).thenReturn(List.of(newStudent));

        // the reference teacher is not enrolled as a regular teacher
        moduleService.updateMembers(10L, 1L, List.of(1L), List.of(3L));

        assertTrue(oldTeacher.getModules().isEmpty());
        assertTrue(newStudent.getModules().contains(module));
        assertEquals(List.of(newStudent), List.copyOf(module.getEnrolledUsers()));
        verify(userRepository).save(oldTeacher);
        verify(userRepository).save(newStudent);
        verify(userRepository, never()).save(reference);
    }

    @Test
    void updateMembers_keepsMembersStillSelected() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        User student = createUser(3L, "Faivre", Role.STUDENT);
        Module module = createModule(reference);
        enroll(student, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(userRepository.findAllById(any())).thenReturn(List.of(student));

        moduleService.updateMembers(10L, 1L, null, List.of(3L));

        assertTrue(student.getModules().contains(module));
        assertSame(reference, module.getReferenceTeacher());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_studentSelectedAsTeacher() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        Module module = createModule(reference);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(userRepository.findAllById(any())).thenReturn(List.of(createUser(3L, "Faivre", Role.STUDENT)));

        assertThrows(BusinessException.class, () -> moduleService.updateMembers(10L, 1L, List.of(3L), null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_unknownUser() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        Module module = createModule(reference);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reference));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateMembers(10L, 1L, null, List.of(99L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_unknownModule() {
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateMembers(10L, 1L, null, null));
    }

    @Test
    void updateMembers_changesReferenceTeacher() {
        User oldReference = createUser(1L, "Bouquet", Role.TEACHER);
        User newReference = createUser(2L, "Marilleau", Role.TEACHER);
        Module module = createModule(oldReference);
        // the new reference teacher was a regular teacher of the module
        enroll(newReference, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newReference));

        moduleService.updateMembers(10L, 2L, List.of(2L), null);

        assertSame(newReference, module.getReferenceTeacher());
        // the new reference teacher is not a regular teacher anymore, the old one is not enrolled
        assertTrue(newReference.getModules().isEmpty());
        assertTrue(oldReference.getModules().isEmpty());
        verify(moduleRepository).save(module);
    }

    @Test
    void updateMembers_referenceTeacherRequired() {
        Module module = createModule(createUser(1L, "Bouquet", Role.TEACHER));
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));

        assertThrows(BusinessException.class, () -> moduleService.updateMembers(10L, null, null, null));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void updateMembers_referenceTeacherMustBeATeacher() {
        User oldReference = createUser(1L, "Bouquet", Role.TEACHER);
        Module module = createModule(oldReference);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findById(3L)).thenReturn(Optional.of(createUser(3L, "Faivre", Role.STUDENT)));

        assertThrows(BusinessException.class, () -> moduleService.updateMembers(10L, 3L, null, null));
        assertSame(oldReference, module.getReferenceTeacher());
        verify(moduleRepository, never()).save(any());
    }

    private void enroll(User user, Module module) {
        user.getModules().add(module);
        module.getEnrolledUsers().add(user);
    }

    private Module createModule(User reference) {
        Module module = new Module();
        module.setId(10L);
        module.setName("Test fonctionnel");
        module.setReferenceTeacher(reference);
        return module;
    }

    private User createUser(Long id, String lastName, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("First");
        user.setLastName(lastName);
        user.setLogin(lastName.toLowerCase());
        user.setRole(role);
        return user;
    }
}
