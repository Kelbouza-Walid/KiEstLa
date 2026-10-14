package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModuleServiceDeleteTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @InjectMocks
    private ModuleService moduleService;

    @Test
    void deleteModule_removesEnrollmentsAbsencesAndModule() {
        Module module = new Module();
        module.setId(1L);
        module.setName("Mathématiques");
        User student = new User();
        student.setRole(Role.STUDENT);
        student.getModules().add(module);
        module.getEnrolledUsers().add(student);
        when(moduleRepository.findWithStudentsById(1L)).thenReturn(Optional.of(module));

        moduleService.deleteModule(1L);

        assertTrue(student.getModules().isEmpty());
        InOrder inOrder = inOrder(absenceRepository, moduleRepository);
        inOrder.verify(absenceRepository).deleteByModuleId(1L);
        inOrder.verify(moduleRepository).delete(module);
    }

    @Test
    void deleteModule_unknownModuleThrows() {
        when(moduleRepository.findWithStudentsById(42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.deleteModule(42L));

        verify(absenceRepository, never()).deleteByModuleId(any());
        verify(moduleRepository, never()).delete(any());
    }
}
