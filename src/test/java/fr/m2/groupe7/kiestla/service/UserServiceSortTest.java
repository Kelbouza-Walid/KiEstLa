package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceSortTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsersSortedByRole_groupsByRole_thenByLastName() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "Alice", "Durand", "adurand", Role.STUDENT),
                user(2L, "Bob", "Martin", "bmartin", Role.TEACHER),
                user(3L, "Root", "Admin", "admin", Role.ADMIN),
                user(4L, "Zoe", "Adams", "zadams", Role.STUDENT)));

        List<AdminUserSummary> result = userService.getAllUsersSortedByRole();

        assertEquals(List.of(Role.ADMIN, Role.TEACHER, Role.STUDENT, Role.STUDENT),
                result.stream().map(AdminUserSummary::role).toList());
        assertEquals("Adams", result.get(2).lastName());
        assertEquals("Durand", result.get(3).lastName());
    }

    private User user(Long id, String firstName, String lastName, String login, Role role) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setLogin(login);
        u.setRole(role);
        return u;
    }
}