package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    @EntityGraph(attributePaths = "modules")
    Optional<User> findModulesByLogin(String login);
    @EntityGraph(attributePaths = "modules")
    Optional<User> findWithModulesById(Long id);
    List<User> findByRole(Role role);
    boolean existsByLogin(String login);
    boolean existsByLoginAndIdNot(String login, Long id);

    /**
     * Finds the users whose first name, last name or login matches the pattern (lower case, with % wildcards),
     * optionally restricted to a role.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (LOWER(u.firstName) LIKE :pattern
                   OR LOWER(u.lastName) LIKE :pattern
                   OR LOWER(u.login) LIKE :pattern)
            ORDER BY u.lastName, u.firstName
            """)
    List<User> search(@Param("pattern") String pattern, @Param("role") Role role);
}
