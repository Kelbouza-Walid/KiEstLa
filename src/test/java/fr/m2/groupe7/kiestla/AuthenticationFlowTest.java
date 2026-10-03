package fr.m2.groupe7.kiestla;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Tests the complete authentication flow:
 * First I created a real user
 * Then I simulated an actual login (a POST request with login and password)
 * Then I verified if SpringSecurity refuses or accepts the connection
 * And finally checked the redirection based on role (through CustomAuthenticationSuccessHandler)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // this annotation rolls back database changes after each test, to not pollute data
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // created a real user and gave it a login and pass using setters
        User user = new User();
        user.setFirstName("kelbouza");
        user.setLastName("walid");
        user.setLogin("walid1");
        user.setPassword(passwordEncoder.encode("walid123"));
        user.setRole(Role.STUDENT);
        userRepository.save(user);
    }

    @Test
    void testValidLoginRedirectsToStudentPage() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("login", "walid1")
                        .password("password", "walid123"))
                .andExpect(authenticated())              // checking if the user is authenticated
                .andExpect(redirectedUrl("/student"));    // checking redirection based on role
    }

    @Test
    void testInvalidPasswordFailsAuthentication() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("login", "walid1")
                        .password("password", "mot de pass faux"))
                .andExpect(unauthenticated())             // checking failed connection
                .andExpect(redirectedUrl("/login?error=true"));
    }
}