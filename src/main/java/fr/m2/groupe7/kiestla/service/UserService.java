package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> getAvailableStudentsForModule(Module module) {
        Set<Long> enrolledStudentIds = module.getStudents().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .map(User::getId)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.STUDENT).stream()
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<User> getAvailableTeachersForModule(Module module) {
        Set<Long> enrolledTeacherIds = module.getStudents().stream()
                .filter(u -> u.getRole() == Role.TEACHER)
                .map(User::getId)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.TEACHER).stream()
                .filter(teacher -> !enrolledTeacherIds.contains(teacher.getId()))
                .toList();
    }
}
