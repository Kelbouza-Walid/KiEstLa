package fr.m2.groupe7.kiestla;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    // ça c le test de la page login
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLoginPageRendersSuccessfully() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kiestla")))
                .andExpect(content().string(containsString("Identifiant")));
    }

    @Test
    void testLoginPageDisplaysErrorOnBadCredentials() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Identifiant ou mot de passe incorrect.")));
    }
}
