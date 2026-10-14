package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModuleServiceCreateTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @InjectMocks
    private ModuleService moduleService;

    @Test
    void createModule_savesModuleWithTrimmedNameAndReferenceTeacher() {
        User teacher = user(Role.TEACHER);
        when(moduleRepository.existsByNameIgnoreCase("Mathématiques")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(teacher));

        moduleService.createModule("  Mathématiques ", 5L);

        ArgumentCaptor<Module> saved = ArgumentCaptor.forClass(Module.class);
        verify(moduleRepository).save(saved.capture());
        assertEquals("Mathématiques", saved.getValue().getName());
        assertSame(teacher, saved.getValue().getReferenceTeacher());
    }

    @Test
    void createModule_blankNameThrows() {
        assertThrows(BusinessException.class, () -> moduleService.createModule("   ", 5L));
        assertThrows(BusinessException.class, () -> moduleService.createModule(null, 5L));

        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_missingReferenceTeacherThrows() {
        assertThrows(BusinessException.class, () -> moduleService.createModule("Mathématiques", null));

        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_duplicateNameThrows() {
        when(moduleRepository.existsByNameIgnoreCase("Mathématiques")).thenReturn(true);

        assertThrows(BusinessException.class, () -> moduleService.createModule("Mathématiques", 5L));

        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_unknownReferenceTeacherThrows() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.createModule("Mathématiques", 5L));

        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_referenceTeacherMustBeATeacher() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(Role.STUDENT)));

        assertThrows(BusinessException.class, () -> moduleService.createModule("Mathématiques", 5L));

        verify(moduleRepository, never()).save(any());
    }

    private User user(Role role) {
        User u = new User();
        u.setId(5L);
        u.setRole(role);
        return u;
    }
}
