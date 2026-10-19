package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.entity.PasswordResetToken;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordService passwordService;

    @Test
    @WithMockUser(username = "walid", roles = "STUDENT")
    void changePasswordPage_authenticated_returnsOk() throws Exception {
        mockMvc.perform(get("/change-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Change my password")));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void requestPasswordReset_authenticatedStudent_redirectsToStudentHome() throws Exception {
        mockMvc.perform(post("/request-password-reset").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"))
                .andExpect(flash().attribute("successMessage",
                        "A password reset link has been sent to your email address."));

        verify(passwordService).requestPasswordReset(eq("student1"), anyString());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void requestPasswordReset_authenticatedTeacher_redirectsToTeacherHome() throws Exception {
        mockMvc.perform(post("/request-password-reset").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher"))
                .andExpect(flash().attribute("successMessage",
                        "A password reset link has been sent to your email address."));

        verify(passwordService).requestPasswordReset(eq("teacher1"), anyString());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void requestPasswordReset_withReferer_redirectsToReferer() throws Exception {
        mockMvc.perform(post("/request-password-reset")
                        .with(csrf())
                        .header("Referer", "http://localhost:8080/change-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:8080/change-password"))
                .andExpect(flash().attribute("successMessage",
                        "A password reset link has been sent to your email address."));
    }

    @Test
    void forgotPasswordPage_unauthenticated_returnsOk() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reset your password")))
                .andExpect(content().string(containsString("Username or Email")));
    }

    @Test
    void processForgotPassword_success_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("identifier", "john.doe@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage",
                        "A password reset link has been sent to your email address."));

        verify(passwordService).requestPasswordReset(eq("john.doe@example.com"), anyString());
    }

    @Test
    void processForgotPassword_error_redirectsToForgotPassword() throws Exception {
        doThrow(new BusinessException("No account found with this identifier."))
                .when(passwordService).requestPasswordReset(eq("unknown"), anyString());

        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("identifier", "unknown"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("errorMessage", "No account found with this identifier."));
    }

    @Test
    void resetPasswordPage_validToken_rendersForm() throws Exception {
        User user = new User();
        user.setLogin("walid");
        PasswordResetToken token = new PasswordResetToken("token-xyz", user, LocalDateTime.now().plusMinutes(20));

        when(passwordService.validateResetToken("token-xyz")).thenReturn(token);

        mockMvc.perform(get("/reset-password").param("token", "token-xyz"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("token", "token-xyz"))
                .andExpect(content().string(containsString("Set a new password")));
    }

    @Test
    void resetPasswordPage_invalidToken_displaysError() throws Exception {
        doThrow(new BusinessException("This password reset link has expired."))
                .when(passwordService).validateResetToken("token-expired");

        mockMvc.perform(get("/reset-password").param("token", "token-expired"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "This password reset link has expired."))
                .andExpect(content().string(containsString("This password reset link has expired.")));
    }

    @Test
    void resetPasswordPage_missingToken_displaysError() throws Exception {
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Invalid password reset link."));
    }

    @Test
    void processResetPassword_success_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "token-123")
                        .param("newPassword", "ValidPassword123")
                        .param("confirmPassword", "ValidPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage",
                        "Your password has been successfully reset. You can now log in."));

        verify(passwordService).resetPassword("token-123", "ValidPassword123", "ValidPassword123");
    }

    @Test
    void processResetPassword_error_redirectsToResetPage() throws Exception {
        doThrow(new BusinessException("The confirmation does not match the new password."))
                .when(passwordService).resetPassword("token-123", "Pass1", "Pass2");

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "token-123")
                        .param("newPassword", "Pass1")
                        .param("confirmPassword", "Pass2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?token=token-123"))
                .andExpect(flash().attribute("errorMessage",
                        "The confirmation does not match the new password."));
    }
}
