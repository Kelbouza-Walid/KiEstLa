package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.dto.AdminUserDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.UpdateUserRequest;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
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
        when(userService.searchUsers(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("roles", Role.values()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void index_searchesUsers() throws Exception {
        List<AdminUserSummary> found = List.of(new AdminUserSummary(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT));
        when(userService.searchUsers("faiv", Role.STUDENT)).thenReturn(found);

        mockMvc.perform(get("/admin").param("q", "faiv").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("users", found))
                .andExpect(model().attribute("searchQuery", "faiv"))
                .andExpect(model().attribute("searchRole", Role.STUDENT));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void index_emptyRoleMeansAllRoles() throws Exception {
        when(userService.searchUsers("faiv", null)).thenReturn(List.of());

        mockMvc.perform(get("/admin").param("q", "faiv").param("role", ""))
                .andExpect(status().isOk());

        verify(userService).searchUsers("faiv", null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewUser_showsDetails() throws Exception {
        AdminUserDetails details = new AdminUserDetails(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT,
                List.of(new ModuleSummary(1L, "Algorithmique", "Bouquet")), List.of(), 2L);
        when(userService.getUserDetails(5L)).thenReturn(details);

        mockMvc.perform(get("/admin/users/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-details"))
                .andExpect(model().attribute("userDetails", details));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewUser_notFound() throws Exception {
        when(userService.getUserDetails(99L)).thenThrow(new ResourceNotFoundException("User not found."));

        mockMvc.perform(get("/admin/users/99").header("Referer", "/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "User not found."));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotViewUser() throws Exception {
        mockMvc.perform(get("/admin/users/5")).andExpect(status().isForbidden());
        verify(userService, never()).getUserDetails(any());
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
        when(userService.createUser(any())).thenThrow(new BusinessException("This login is already used."));

        mockMvc.perform(post("/admin/users").with(csrf())
                .header("Referer", "/admin")
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet")
                .param("role", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "This login is already used."));
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void editUser_showsPrefilledForm() throws Exception {
        AdminUserSummary user = new AdminUserSummary(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        when(userService.getUser(5L)).thenReturn(user);

        mockMvc.perform(get("/admin/users/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-edit"))
                .andExpect(model().attribute("editedUser", user))
                .andExpect(model().attribute("roles", Role.values()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editUser_notFound() throws Exception {
        when(userService.getUser(99L)).thenThrow(new ResourceNotFoundException("User not found."));

        mockMvc.perform(get("/admin/users/99/edit").header("Referer", "/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "User not found."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_success() throws Exception {
        UpdateUserRequest expected = new UpdateUserRequest("Noa", "Faivre", "nfaivre2", Role.STUDENT);
        when(userService.updateUser(5L, expected, "admin"))
                .thenReturn(new AdminUserSummary(5L, "Noa", "Faivre", "nfaivre2", Role.STUDENT));

        performValidUpdate()
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("successMessage", "User nfaivre2 has been updated."));

        verify(userService).updateUser(5L, expected, "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_blankLogin() throws Exception {
        mockMvc.perform(post("/admin/users/5").with(csrf())
                .param("firstName", "Noa")
                .param("lastName", "Faivre")
                .param("login", " ")
                .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/5/edit"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(userService, never()).updateUser(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_duplicateLogin() throws Exception {
        when(userService.updateUser(any(), any(), any()))
                .thenThrow(new BusinessException("This login is already used."));

        mockMvc.perform(post("/admin/users/5").with(csrf())
                .header("Referer", "/admin/users/5/edit")
                .param("firstName", "Noa")
                .param("lastName", "Faivre")
                .param("login", "skacmaz")
                .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/5/edit"))
                .andExpect(flash().attribute("errorMessage", "This login is already used."));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotUpdateUser() throws Exception {
        performValidUpdate().andExpect(status().isForbidden());
        verify(userService, never()).updateUser(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotEditUser() throws Exception {
        mockMvc.perform(get("/admin/users/5/edit")).andExpect(status().isForbidden());
        verify(userService, never()).getUser(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_success() throws Exception {
        AdminUserSummary user = new AdminUserSummary(10L, "John", "Doe", "jdoe", Role.STUDENT);
        when(userService.deleteUser(10L, "admin")).thenReturn(user);

        mockMvc.perform(post("/admin/users/10/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("deleteMessage", "l'utilisateur john doe a bien été supprimer"))
                .andExpect(flash().attribute("deleteSuccessMessage", "l'utilisateur john doe a bien été supprimer"));

        verify(userService).deleteUser(10L, "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_notFound() throws Exception {
        when(userService.deleteUser(99L, "admin")).thenThrow(new ResourceNotFoundException("User not found."));

        mockMvc.perform(post("/admin/users/99/delete").with(csrf()).header("Referer", "/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "User not found."));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotDeleteUser() throws Exception {
        mockMvc.perform(post("/admin/users/10/delete").with(csrf()))
                .andExpect(status().isForbidden());
        verify(userService, never()).deleteUser(any(), any());
    }

    private org.springframework.test.web.servlet.ResultActions performValidUpdate() throws Exception {
        return mockMvc.perform(post("/admin/users/5").with(csrf())
                .param("firstName", "Noa")
                .param("lastName", "Faivre")
                .param("login", "nfaivre2")
                .param("role", "STUDENT"));
    }

    private org.springframework.test.web.servlet.ResultActions performValidCreation() throws Exception {
        return mockMvc.perform(post("/admin/users").with(csrf())
                .param("firstName", "Fabrice")
                .param("lastName", "Bouquet")
                .param("login", "fbouquet")
                .param("role", "TEACHER"));
    }
}
