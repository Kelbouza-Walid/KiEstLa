package fr.m2.groupe7.kiestla.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.entity.Module;

/**
 * ModuleService
 */
@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Retrieves all modules
     */
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    public Optional<Module> getModuleById(Long id){ return moduleRepository.findById(id); }
}
