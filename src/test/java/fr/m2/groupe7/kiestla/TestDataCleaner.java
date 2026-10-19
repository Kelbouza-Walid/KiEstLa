package fr.m2.groupe7.kiestla;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

/**
 * Empties the test database before an acceptance scenario.
 * The Cucumber scenarios share the same database: deleting the modules with
 * moduleRepository.deleteAll() fails as soon as a previous scenario left an enrollment,
 * so the rows are removed here in the order required by the foreign keys.
 */
@Component
public class TestDataCleaner {

    private final NotifRepository notifRepository;
    private final CommentRepository commentRepository;
    private final AbsenceRepository absenceRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    public TestDataCleaner(NotifRepository notifRepository, CommentRepository commentRepository,
            AbsenceRepository absenceRepository, ModuleRepository moduleRepository,
            UserRepository userRepository) {
        this.notifRepository = notifRepository;
        this.commentRepository = commentRepository;
        this.absenceRepository = absenceRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void cleanDatabase() {
        notifRepository.deleteAll();
        commentRepository.deleteAll();
        absenceRepository.deleteAll();
        // User owns the enrollments join table, and modules reference their reference teacher
        userRepository.findAll().forEach(user -> user.getModules().clear());
        moduleRepository.findAll().forEach(module -> module.setReferenceTeacher(null));
        userRepository.flush();
        moduleRepository.flush();
        moduleRepository.deleteAll();
        userRepository.deleteAll();
    }
}
