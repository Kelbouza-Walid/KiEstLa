package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
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
    public List<AdminUserSummary> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAvailableStudentsForModule(ModuleDetails module) {
        Set<Long> enrolledStudentIds = module.students().stream()
                .map(UserSummary::id)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.STUDENT).stream()
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .map(UserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAvailableTeachersForModule(ModuleDetails module) {
        Set<Long> enrolledTeacherIds = module.teachers().stream()
                .map(UserSummary::id)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.TEACHER).stream()
                .filter(teacher -> !enrolledTeacherIds.contains(teacher.getId()))
                .map(UserSummary::from)
                .toList();
    }
}
