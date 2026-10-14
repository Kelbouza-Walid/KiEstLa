package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the user creation endpoint of AdminController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void index_exposesRoles() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("roles", Role.values()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_success() throws Exception {
        CreateUserRequest expected = new CreateUserRequest("Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        when(userService.createUser(expected)).thenReturn(new CreatedUser(
                new AdminUserSummary(42L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER), "Abc23xyz45Kq"));

        mockMvc.perform(post("/admin/users").with(csrf())
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet")
                .param("role", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("generatedPassword", "Abc23xyz45Kq"));

        verify(userService).createUser(expected);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_blankLogin() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", " ")
                .param("role", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_missingRole() throws Exception {
        mockMvc.perform(post("/admin/users").with(csrf())
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateLogin() throws Exception {
        when(userService.createUser(any())).thenThrow(new BusinessException("Ce login est déjà utilisé."));

        mockMvc.perform(post("/admin/users").with(csrf())
                .header("Referer", "/admin")
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet")
                .param("role", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "Ce login est déjà utilisé."));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotCreateUser() throws Exception {
        performValidCreation().andExpect(status().isForbidden());
        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    void secretaryCannotCreateUser() throws Exception {
        performValidCreation().andExpect(status().isForbidden());
        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotCreateUser() throws Exception {
        performValidCreation().andExpect(status().isForbidden());
        verify(userService, never()).createUser(any());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        performValidCreation()
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(userService, never()).createUser(any());
    }

    private org.springframework.test.web.servlet.ResultActions performValidCreation() throws Exception {
        return mockMvc.perform(post("/admin/users").with(csrf())
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet")
                .param("role", "TEACHER"));
    }
}
