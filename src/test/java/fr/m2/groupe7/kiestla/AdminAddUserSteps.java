package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the "add users" scenario of the user management feature (921).
 */
public class AdminAddUserSteps {

    private static final String BASE_URL = "http://localhost:8080";

    private static final String NEW_FIRST_NAME = "Fabrice";
    private static final String NEW_LAST_NAME = "Bouquet";
    private static final String NEW_LOGIN = "fbouquet";
    private static final Role NEW_ROLE = Role.TEACHER;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("The user information is valid")
    public void the_user_information_is_valid() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setLogin("admin");
        admin.setPassword("{noop}testpassword");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // Log in as the administrator
        login("admin", "testpassword");
        getPage().waitForURL("**/admin");
    }

    @When("I enter the user's personal information")
    public void i_enter_the_user_s_personal_information() {
        getPage().navigate(BASE_URL + "/admin");
        getPage().getByTestId("input-first-name").fill(NEW_FIRST_NAME);
        getPage().getByTestId("input-last-name").fill(NEW_LAST_NAME);
        getPage().getByTestId("input-login").fill(NEW_LOGIN);
    }

    @When("I set the user's role")
    public void i_set_the_user_s_role() {
        getPage().getByTestId("select-role").selectOption(NEW_ROLE.name());
    }

    @When("I confirm the creation")
    public void i_confirm_the_creation() {
        getPage().getByTestId("btn-create-user").click();
    }

    @Then("The user is registered in the system")
    public void the_user_is_registered_in_the_system() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        assertThat(getPage().getByTestId("user-login").filter(
                new Locator.FilterOptions().setHasText(NEW_LOGIN))).hasCount(1);

        String shownPassword = getPage().getByTestId("generated-password").textContent().trim();
        assertFalse(shownPassword.isEmpty());

        User created = userRepository.findByLogin(NEW_LOGIN).orElseThrow();
        assertEquals(NEW_FIRST_NAME, created.getFirstName());
        assertEquals(NEW_LAST_NAME, created.getLastName());
        assertEquals(NEW_ROLE, created.getRole());
        // The shown password is the real one, and it is not stored in clear
        assertTrue(passwordEncoder.matches(shownPassword, created.getPassword()));
        assertFalse(created.getPassword().contains(shownPassword));
    }

    // --- Scenario: new user logs in with the generated password ---

    @Given("A new user has been created by the administrator")
    public void a_new_user_has_been_created_by_the_administrator() {
        the_user_information_is_valid();
        i_enter_the_user_s_personal_information();
        i_set_the_user_s_role();
        i_confirm_the_creation();

        // Copy the password with the button, as an administrator would
        getPage().context().grantPermissions(List.of("clipboard-read", "clipboard-write"));
        getPage().getByTestId("btn-copy-password").click();
        assertThat(getPage().getByTestId("btn-copy-password")).containsText("Copied");

        getPage().getByTestId("btn-logout").click();
        getPage().waitForURL("**/login?logout=true");
    }

    @When("The new user logs in with the generated password")
    public void the_new_user_logs_in_with_the_generated_password() {
        // Paste the copied password in the login form
        getPage().navigate(BASE_URL + "/login");
        String copiedPassword = (String) getPage().evaluate("() => navigator.clipboard.readText()");
        login(NEW_LOGIN, copiedPassword);
    }

    @Then("The new user is redirected to their home page")
    public void the_new_user_is_redirected_to_their_home_page() {
        // NEW_ROLE is TEACHER, whose home page is /teacher
        getPage().waitForURL("**/teacher");
        assertThat(getPage().locator("body")).not().containsText("Identifiant ou mot de passe incorrect");
    }

    private void login(String login, String password) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", login);
        getPage().fill("input[name='password']", password);
        getPage().click("button[type='submit']");
    }
}
