package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

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
 * Steps for the "lookup users" scenario of the user management feature (921).
 */
public class AdminLookupUserSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    @Given("Users exist in the system")
    public void users_exist_in_the_system() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        createUser("System", "Admin", "admin", Role.ADMIN);
        createUser("Noam", "Faivre", "nfaivre", Role.STUDENT);
        createUser("Suleyman", "Kacmaz", "skacmaz", Role.STUDENT);
        createUser("Fabrice", "Bouquet", "fbouquet", Role.TEACHER);

        // Log in as the administrator
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", "admin");
        getPage().fill("input[name='password']", "testpassword");
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/admin");
    }

    @When("I search for a user by name or role")
    public void i_search_for_a_user_by_name_or_role() {
        // "f" matches Noam Faivre and Fabrice Bouquet, the role keeps only the student
        getPage().getByTestId("input-user-search").fill("f");
        getPage().getByTestId("select-search-role").selectOption(Role.STUDENT.name());
        getPage().getByTestId("btn-search-user").click();
        getPage().waitForURL("**/admin?q=f&role=STUDENT");
    }

    @Then("The matching users are displayed")
    public void the_matching_users_are_displayed() {
        assertThat(getPage().getByTestId("user-row")).hasCount(1);
        assertThat(getPage().getByTestId("user-login")).hasText("nfaivre");

        // Clicking on the user opens their details
        getPage().getByTestId("user-row").click();
        getPage().waitForURL("**/admin/users/*");
        assertThat(getPage().getByTestId("details-full-name")).hasText("Noam Faivre");
        assertThat(getPage().getByTestId("details-login")).hasText("nfaivre");
        assertThat(getPage().getByTestId("details-role")).hasText("STUDENT");
        assertThat(getPage().getByTestId("details-absence-count")).hasText("0");
    }

    private void createUser(String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        userRepository.save(user);
    }
}
