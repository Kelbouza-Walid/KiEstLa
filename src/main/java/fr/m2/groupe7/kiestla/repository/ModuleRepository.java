package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    Optional<Module> findByName(String name);

    @EntityGraph(attributePaths = "students")
    Optional<Module> findWithStudentsById(Long id);
}
