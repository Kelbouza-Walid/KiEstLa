package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



//fichier juste la pour ajouter des utilisateurs/module/absence
@Component
public class AddBDD {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final PasswordEncoder passwordEncoder;

    public AddBDD(
            UserRepository userRepository,
            ModuleRepository moduleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createUserAtStartup() {
        String login = "teacher";

        User teacher = userRepository.findByLogin(login).orElseGet(() -> {
            User newTeacher = new User();
            newTeacher.setLastName("nom");
            newTeacher.setFirstName("prenom");
            newTeacher.setLogin(login);
            newTeacher.setPassword(passwordEncoder.encode("teacher"));
            newTeacher.setRole(Role.TEACHER);
            return userRepository.save(newTeacher);
        });

        String loginT = "prof";

        User teacher2 = userRepository.findByLogin(loginT).orElseGet(() -> {
            User newTeacher = new User();
            newTeacher.setLastName("cher");
            newTeacher.setFirstName("tea");
            newTeacher.setLogin(loginT);
            newTeacher.setPassword(passwordEncoder.encode("teacher"));
            newTeacher.setRole(Role.TEACHER);
            return userRepository.save(newTeacher);
        });

        String loginS = "student";

        User student = userRepository.findByLogin(loginS).orElseGet(() -> {
            User newStudent = new User();
            newStudent.setLastName("stu");
            newStudent.setFirstName("dent");
            newStudent.setLogin(loginS);
            newStudent.setPassword(passwordEncoder.encode("student"));
            newStudent.setRole(Role.STUDENT);
            return userRepository.save(newStudent);
        });
        student.setRole(Role.STUDENT);

        String moduleName = "Mathématiques";
        Module module = moduleRepository.findByName(moduleName).orElseGet(() -> {
            Module newModule = new Module();
            newModule.setName(moduleName);
            newModule.setReferenceTeacher(teacher);
            return moduleRepository.save(newModule);
        });

        teacher.getModules().add(module);
        userRepository.save(teacher);
        userRepository.save(teacher2);
        userRepository.save(student);
    }
}