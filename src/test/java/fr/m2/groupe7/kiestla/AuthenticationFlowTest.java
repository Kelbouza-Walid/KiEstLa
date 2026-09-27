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
 * Teste le flux complet de l'authentification :
 * j'ai  dabord créer un utilisateur réel
 * puis j'ai fais une réele simulation de login ( un post avec son login et mdp )
 * après j'ai vérifie si SpringSecurity refuse ou accepte la connexion
 * et puis une vérification de la redirection selon le role (à travers CustomAuthenticationSuccessHandler)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // cette annotation annule les changements en base après chaque test, pour ne pas polluer les données
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // j'ai crée un vrai utilisateur et j'ai donné un login et pass avec setters
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
                .andExpect(authenticated())              // la je vérifie si lutilisateur est bien créé
                .andExpect(redirectedUrl("/student"));    // la je vérifie la redirection selon le role
    }

    @Test
    void testInvalidPasswordFailsAuthentication() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("login", "walid1")
                        .password("password", "mot de pass faux"))
                .andExpect(unauthenticated())             //  la je vérifie la connexion echoué
                .andExpect(redirectedUrl("/login?error=true"));
    }
}