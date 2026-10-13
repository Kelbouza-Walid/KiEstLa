package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModuleServiceSearchTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ModuleService moduleService;

    @Test
    void searchModulesByName_returnsMatchingModules() {
        when(moduleRepository.findByNameContainingIgnoreCaseOrderByName("math"))
                .thenReturn(List.of(module(1L, "Mathématiques")));

        List<ModuleSummary> result = moduleService.searchModulesByName("  math ");

        assertEquals(1, result.size());
        assertEquals("Mathématiques", result.get(0).name());
    }

    @Test
    void searchModulesByName_blankReturnsAllModules() {
        when(moduleRepository.findAll())
                .thenReturn(List.of(module(1L, "Maths"), module(2L, "Algo")));

        List<ModuleSummary> result = moduleService.searchModulesByName("  ");

        assertEquals(2, result.size());
        verify(moduleRepository, never()).findByNameContainingIgnoreCaseOrderByName(anyString());
    }

    private Module module(Long id, String name) {
        Module m = new Module();
        m.setId(id);
        m.setName(name);
        return m;
    }
}