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

        assertEquals("First Bouquet", result.referenceTeacherName());
        assertEquals(List.of(new ModuleMembers.Member(UserSummary.from(teacher), false)), result.teachers());
        assertEquals(List.of(
                new ModuleMembers.Member(UserSummary.from(enrolledStudent), true),
                new ModuleMembers.Member(UserSummary.from(otherStudent), false)), result.students());
    }

    @Test
    void updateMembers_enrollsSelectedAndUnenrollsOthers() {
        User reference = createUser(1L, "Bouquet", Role.TEACHER);
        User oldTeacher = createUser(2L, "Marilleau", Role.TEACHER);
        User newStudent = createUser(3L, "Faivre", Role.STUDENT);
        Module module = createModule(reference);
        enroll(oldTeacher, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findAllById(any())).thenReturn(List.of(newStudent));

        // the reference teacher id is ignored
        moduleService.updateMembers(10L, List.of(1L), List.of(3L));

        assertTrue(oldTeacher.getModules().isEmpty());
        assertTrue(newStudent.getModules().contains(module));
        assertEquals(List.of(newStudent), List.copyOf(module.getEnrolledUsers()));
        verify(userRepository).save(oldTeacher);
        verify(userRepository).save(newStudent);
        verify(userRepository, never()).save(reference);
    }

    @Test
    void updateMembers_keepsMembersStillSelected() {
        User student = createUser(3L, "Faivre", Role.STUDENT);
        Module module = createModule(null);
        enroll(student, module);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findAllById(any())).thenReturn(List.of(student));

        moduleService.updateMembers(10L, null, List.of(3L));

        assertTrue(student.getModules().contains(module));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_studentSelectedAsTeacher() {
        Module module = createModule(null);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findAllById(any())).thenReturn(List.of(createUser(3L, "Faivre", Role.STUDENT)));

        assertThrows(BusinessException.class, () -> moduleService.updateMembers(10L, List.of(3L), null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_unknownUser() {
        Module module = createModule(null);
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.of(module));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateMembers(10L, null, List.of(99L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMembers_unknownModule() {
        when(moduleRepository.findWithStudentsById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateMembers(10L, null, null));
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
