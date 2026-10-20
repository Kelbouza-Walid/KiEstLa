package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "Password change" (945).
 * The three scenarios share the sentence "I enter my new password": the password typed
 * depends on the scenario (valid, identical to the current one, wrong confirmation).
 * "I get to retry" is shared with other features: see SharedSteps.
 */
public class PasswordChangeSteps {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LOGIN = "pwduser";
    private static final String CURRENT_PASSWORD = "testpassword";
    private static final String NEW_PASSWORD = "NewPassword123";

    @Autowired
    private TestDataCleaner testDataCleaner;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String scenarioName;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Before
    public void rememberScenario(Scenario scenario) {
        this.scenarioName = scenario.getName();
    }

    @Given("I was already registered in the database and I am authenticated")
    public void i_was_already_registered_in_the_database_and_i_am_authenticated() {
        testDataCleaner.cleanDatabase();

        User student = new User();
        student.setFirstName("Paula");
        student.setLastName("Word");
        student.setLogin(LOGIN);
        student.setPassword("{noop}" + CURRENT_PASSWORD);
        student.setRole(Role.STUDENT);
        userRepository.save(student);

        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", LOGIN);
        getPage().fill("input[name='password']", CURRENT_PASSWORD);
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/student");
    }

    @When("I enter my new password")
    public void i_enter_my_new_password() {
        String newPassword = NEW_PASSWORD;
        String confirmation = NEW_PASSWORD;
        if ("New password is identical to old password".equals(scenarioName)) {
            newPassword = CURRENT_PASSWORD;
            confirmation = CURRENT_PASSWORD;
        } else if ("Password change failed".equals(scenarioName)) {
            confirmation = "AnotherPassword123";
        }

        getPage().navigate(BASE_URL + "/change-password");
        getPage().getByTestId("input-current-password").fill(CURRENT_PASSWORD);
        getPage().getByTestId("input-new-password").fill(newPassword);
        getPage().getByTestId("input-confirm-password").fill(confirmation);
    }

    @When("I click a button to confirm")
    public void i_click_a_button_to_confirm() {
        getPage().getByTestId("btn-change-password").click();
    }

    @Then("I should get a message notifying me of the successful operation")
    public void i_should_get_a_message_notifying_me_of_the_successful_operation() {
        assertThat(getPage().getByTestId("password-success")).isVisible();
        User user = userRepository.findByLogin(LOGIN).orElseThrow();
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, user.getPassword()));
    }

    @Then("I should see a message indicating that the two passwords are identical")
    public void i_should_see_a_message_indicating_that_the_two_passwords_are_identical() {
        assertThat(getPage().getByTestId("password-error"))
                .containsText("Le nouveau mot de passe doit être différent de l'ancien");
        assertPasswordUnchanged();
    }

    @Then("I should see a message indicating what the error was")
    public void i_should_see_a_message_indicating_what_the_error_was() {
        assertThat(getPage().getByTestId("password-error"))
                .containsText("La confirmation ne correspond pas au nouveau mot de passe");
        assertPasswordUnchanged();
    }

    private void assertPasswordUnchanged() {
        User user = userRepository.findByLogin(LOGIN).orElseThrow();
        assertTrue(passwordEncoder.matches(CURRENT_PASSWORD, user.getPassword()));
    }
}
