package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LogoutTest{

    @Autowired
    private TestDataCleaner testDataCleaner;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private String scenarioName;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // On mémorise seulement le nom du scénario : la page Playwright est
    // créée par PlaywrightHooks, on ne dépend donc pas de l'ordre des hooks.
    @Before
    public void rememberScenario(Scenario scenario) {
        this.scenarioName = scenario.getName();
    }

    @Given("that I am logged in")
    public void that_i_am_logged_in() {
        testDataCleaner.cleanDatabase();

        User secretary = new User();
        secretary.setFirstName("FirstName");
        secretary.setLastName("LastName");
        secretary.setLogin("secretary");
        secretary.setPassword("{noop}testpassword");
        secretary.setRole(Role.SECRETARY);
        userRepository.save(secretary);

        getPage().navigate("http://localhost:8080/login");
        getPage().fill("input[name='login']", "secretary");
        getPage().fill("input[name='password']", "testpassword");
        getPage().click("button[type='submit']");

        getPage().waitForURL("**/secretary");
    }

    @When("^I click (?:a|the) logout button$")
    public void i_click_the_logout_button() {
        // Le scénario d'échec n'a pas de step pour simuler l'erreur :
        // on fait échouer la requête POST /logout (et elle seule) ici.
        if ("Logout failed".equals(scenarioName)) {
            getPage().route("**/logout", (Route route) -> route.fulfill(
                    new Route.FulfillOptions()
                            .setStatus(500)
                            .setContentType("text/plain")
                            .setBody("Server error")));
        }
        getPage().getByTestId("btn-logout").click();
    }

    // --- Scenario: Logging out ---

    @Then("I shouldn't stay identified")
    public void i_shouldnt_stay_identified() {
        getPage().waitForURL(Pattern.compile(".*/login.*"));
        assertThat(getPage().getByTestId("btn-logout")).hasCount(0);
    }

    @Then("get my access revoked")
    public void get_my_access_revoked() {
        getPage().navigate("http://localhost:8080/secretary");
        assertThat(getPage()).hasURL(Pattern.compile(".*/login.*"));
    }

    @Then("should be redirected to the login page")
    public void should_be_redirected_to_the_login_page() {
        assertThat(getPage()).hasURL(Pattern.compile(".*/login.*"));
    }

    // --- Scenario: Logout failed ---

    @Then("I should see a message indicating what the error is")
    public void i_should_see_a_message_indicating_what_the_error_is() {
        assertThat(getPage().getByTestId("logout-error")).isVisible();
        assertThat(getPage().getByTestId("logout-error")).not().isEmpty();
    }

    // "I get to retry" is shared with the login and password change features: see SharedSteps
}