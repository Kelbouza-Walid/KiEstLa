package fr.m2.groupe7.kiestla.service;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.PlaywrightHooks;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "manage modules with CRUD operations" (admin).
 * One class for the whole feature: the steps are shared between the tickets.
 */
public class AdminModuleSteps {

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

    // ---------- lookup modules (ticket TFKIES26-97) ----------

    @Given("Modules exist in the system")
    public void modules_exist_in_the_system() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        createUser("System", "Admin", "admin", Role.ADMIN);
        createModule("Mathématiques");
        createModule("Algorithmique");
        createModule("Physique");

        login("admin", "testpassword");
    }

    @When("I search for a module by its name")
    public void i_search_for_a_module_by_its_name() {
        getPage().navigate(BASE_URL + "/admin/modules");
        getPage().getByTestId("input-module-search").fill("math");
        getPage().getByTestId("btn-search-module").click();
    }

    @Then("The matching modules are displayed")
    public void the_matching_modules_are_displayed() {
        assertThat(getPage().getByTestId("module-row")).hasCount(1);
        assertThat(getPage().getByTestId("module-name")).hasText("Mathématiques");
    }

    // ---------- add modules (ticket TFKIES26-85) ----------

    @Given("The module information is valid")
    public void the_module_information_is_valid() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        createUser("System", "Admin", "admin", Role.ADMIN);
        createUser("Marie", "Curie", "mcurie", Role.TEACHER);

        login("admin", "testpassword");
        getPage().navigate(BASE_URL + "/admin/modules");
    }

    @When("I enter the module name")
    public void i_enter_the_module_name() {
        getPage().getByTestId("input-module-name").fill("Mathématiques");
    }

    @When("I select a reference teacher")
    public void i_select_a_reference_teacher() {
        getPage().getByTestId("select-reference-teacher").selectOption("Marie Curie (mcurie)");
    }

    @When("I confirm the module creation")
    public void i_confirm_the_creation() {
        getPage().getByTestId("btn-add-module").click();
    }

    @Then("The module is registered in the system")
    public void the_module_is_registered_in_the_system() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        assertThat(getPage().getByTestId("module-row")).hasCount(1);
        assertThat(getPage().getByTestId("module-name")).hasText("Mathématiques");
        assertTrue(moduleRepository.findByName("Mathématiques").isPresent());
    }

    // ---------- remove modules ----------

    @Given("The module exists")
    public void the_module_exists() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        createUser("System", "Admin", "admin", Role.ADMIN);
        Module mathematics = createModule("Mathématiques");
        mathematics.setReferenceTeacher(createUser("Ada", "Lovelace", "alovelace", Role.TEACHER));
        moduleRepository.save(mathematics);
        createModule("Algorithmique");

        login("admin", "testpassword");
    }

    @When("I select the module to remove")
    public void i_select_the_module_to_remove() {
        getPage().navigate(BASE_URL + "/admin/modules");
        getPage().getByTestId("input-module-search").fill("Mathématiques");
        getPage().getByTestId("btn-search-module").click();
        assertThat(getPage().getByTestId("module-row")).hasCount(1);
    }

    // Shared by the users (921) and modules (923) features: once the item to remove is selected,
    // a single delete button is displayed, for a module or for a user
    @When("I confirm the deletion")
    public void i_confirm_the_deletion() {
        getPage().onceDialog(dialog -> dialog.accept());
        getPage().locator("[data-testid='btn-delete-module'], [data-testid='btn-delete-user']").click();
    }

    @Then("The module is no longer registered in the system")
    public void the_module_is_no_longer_registered_in_the_system() {
        assertThat(getPage().getByTestId("success-message")).isVisible();
        assertThat(getPage().getByTestId("module-row")).hasCount(1);
        assertThat(getPage().getByTestId("module-name")).hasText("Algorithmique");
        assertTrue(moduleRepository.findByName("Mathématiques").isEmpty());
    }

    // ---------- Helpers ----------

    private User createUser(String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Module createModule(String name) {
        Module module = new Module();
        module.setName(name);
        return moduleRepository.save(module);
    }

    private void login(String login, String password) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", login);
        getPage().fill("input[name='password']", password);
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/admin");
    }
}