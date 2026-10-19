package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "Login" (893).
 * "I get to retry" is shared with other features: see SharedSteps.
 */
public class LoginSteps {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LOGIN = "jdoe";
    private static final String PASSWORD = "testpassword";

    @Autowired
    private TestDataCleaner testDataCleaner;

    @Autowired
    private UserRepository userRepository;

    /** URL of the page displayed right after the login form is submitted. */
    private String landingUrl;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("I was already registered in the database")
    public void i_was_already_registered_in_the_database() {
        testDataCleaner.cleanDatabase();

        User teacher = new User();
        teacher.setFirstName("John");
        teacher.setLastName("Doe");
        teacher.setLogin(LOGIN);
        teacher.setPassword("{noop}" + PASSWORD);
        teacher.setRole(Role.TEACHER);
        userRepository.save(teacher);
    }

    @When("I enter my details")
    public void i_enter_my_details() {
        submitLoginForm(LOGIN, PASSWORD);
        getPage().waitForURL("**/teacher");
        landingUrl = getPage().url();
    }

    @When("I enter wrong credentials")
    public void i_enter_wrong_credentials() {
        submitLoginForm(LOGIN, "wrong-password");
    }

    @Then("I should gain access to the features I am allowed")
    public void i_should_gain_access_to_the_features_i_am_allowed() {
        // The teacher's pages are open, the other roles' pages are forbidden
        assertThat(getPage().getByTestId("btn-logout")).isVisible();
        assertEquals(403, getPage().navigate(BASE_URL + "/admin").status());
        assertEquals(403, getPage().navigate(BASE_URL + "/secretary").status());
        assertEquals(200, getPage().navigate(BASE_URL + "/teacher").status());
    }

    @Then("should be redirected to the right page")
    public void should_be_redirected_to_the_right_page() {
        // A teacher lands on the teacher's home page
        assertEquals(BASE_URL + "/teacher", landingUrl);
    }

    @Then("I should see a message indicating what\\/where the error is")
    public void i_should_see_a_message_indicating_what_where_the_error_is() {
        assertThat(getPage()).hasURL(Pattern.compile(".*/login\\?error.*"));
        assertThat(getPage().getByText("Identifiant ou mot de passe incorrect.")).isVisible();
    }

    private void submitLoginForm(String login, String password) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", login);
        getPage().fill("input[name='password']", password);
        getPage().click("button[type='submit']");
    }
}
