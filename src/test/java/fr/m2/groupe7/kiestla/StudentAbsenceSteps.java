package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import fr.m2.groupe7.kiestla.service.ModuleService;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class StudentAbsenceSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleService moduleService;

    private User student;
    private Page page;
    private boolean studentScenarioInitialized;

    @Given("I am logged in as a student with username {string}")
    public void i_am_logged_in_as_a_student(String username) {
        studentScenarioInitialized = true;
        absenceRepository.deleteAll();
        moduleRepository.findAll().forEach(module -> moduleService.deleteModule(module.getId()));
        userRepository.deleteAll();

        student = new User();
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setLogin(username);
        student.setPassword("{noop}testpassword");
        student.setRole(Role.STUDENT);
        student = userRepository.save(student);

        page = PlaywrightHooks.getPage();
        page.navigate(BASE_URL + "/login");
        page.fill("input[name='login']", username);
        page.fill("input[name='password']", "testpassword");
        page.click("button[type='submit']");
        page.waitForURL("**/student");
    }

    @After
    public void cleanUpStudentScenario() {
        if (!studentScenarioInitialized) {
            return;
        }
        absenceRepository.deleteAll();
        moduleRepository.findAll().forEach(module -> moduleService.deleteModule(module.getId()));
        userRepository.deleteAll();
    }

    @Given("I am enrolled in module {string} and module {string}")
    public void i_am_enrolled_in_modules(String firstModuleName, String secondModuleName) {
        enrollIn(createModule(firstModuleName), student);
        enrollIn(createModule(secondModuleName), student);
    }

    @Given("I have absences recorded in both modules")
    public void i_have_absences_recorded_in_both_modules() {
        addAbsence("M1", AbsenceStatus.UNJUSTIFIED, LocalDate.of(2026, 10, 1));
        addAbsence("M2", AbsenceStatus.UNJUSTIFIED, LocalDate.of(2026, 10, 2));
    }

    @Given("I have no absences recorded")
    public void i_have_no_absences_recorded() {
        absenceRepository.deleteAll();
    }

    @Given("I have an absence in module {string} with status {string}")
    public void i_have_an_absence_in_module_with_status(String moduleName, String status) {
        Module module = moduleRepository.findByName(moduleName).orElseGet(() -> createModule(moduleName));
        enrollIn(module, student);
        addAbsence(module, AbsenceStatus.valueOf(status.toUpperCase()), LocalDate.of(2026, 10, 1));
    }

    @When("I navigate to my absences page")
    public void i_navigate_to_my_absences_page() {
        page.navigate(BASE_URL + "/student");
    }

    @When("I navigate to my absences status page")
    public void i_navigate_to_my_absences_status_page() {
        i_navigate_to_my_absences_page();
    }

    @When("I select {string} option")
    public void i_select_option(String option) {
        if ("Sort by module".equals(option)) {
            page.getByTestId("absence-sort").selectOption("MODULE");
            page.waitForURL("**/student?*sortBy=MODULE*");
            return;
        }
        throw new IllegalArgumentException("Unsupported absence option: " + option);
    }

    @When("I filter by module {string}")
    public void i_filter_by_module(String moduleName) {
        page.getByTestId("absence-module-filter").selectOption(moduleName);
        page.waitForURL("**/student?*module=" + moduleName + "*");
    }

    @Then("I should see a list of my absences")
    public void i_should_see_a_list_of_my_absences() {
        assertThat(page.getByTestId("student-absence")).hasCount(2);
    }

    @Then("the absences should be sorted alphabetically by module name")
    public void the_absences_should_be_sorted_alphabetically_by_module_name() {
        assertEquals(List.of("M1", "M2"), page.getByTestId("absence-module").allTextContents());
    }

    @Then("I should see a message {string}")
    public void i_should_see_a_message(String message) {
        assertThat(page.getByTestId("no-absences")).hasText(message);
    }

    @Then("I should only see absences from module {string}")
    public void i_should_only_see_absences_from_module(String moduleName) {
        assertThat(page.getByTestId("student-absence")).hasCount(1);
        assertThat(page.getByTestId("absence-module")).hasText(moduleName);
    }

    @Then("I should see my absences sorted by module name")
    public void i_should_see_my_absences_sorted_by_module_name() {
        assertEquals(List.of("M1", "M2"), page.getByTestId("absence-module").allTextContents());
    }

    @Then("^each absence should display its status \\(\"justified\" or \"unjustified\"\\)$")
    public void each_absence_should_display_its_status() {
        List<String> expected = List.of("JUSTIFIED", "UNJUSTIFIED");
        List<Locator> rows = page.getByTestId("student-absence").all();
        assertEquals(expected.size(), rows.size());
        for (int i = 0; i < rows.size(); i++) {
            assertEquals(expected.get(i), rows.get(i).getByTestId("absence-status").textContent().trim());
        }
    }

    @Then("I should see the absence in {string} marked as {string}")
    public void i_should_see_absence_marked_as(String moduleName, String status) {
        List<String> modules = page.getByTestId("absence-module").allTextContents();
        int index = modules.indexOf(moduleName);
        if (index < 0) {
            throw new AssertionError("No absence displayed for module " + moduleName);
        }
        assertEquals(status.toUpperCase(),
                page.getByTestId("student-absence").nth(index).getByTestId("absence-status").textContent().trim());
    }

    private Module createModule(String name) {
        Module module = new Module();
        module.setName(name);
        return moduleRepository.save(module);
    }

    private void enrollIn(Module module, User user) {
        User enrolledStudent = userRepository.findWithModulesById(user.getId()).orElseThrow();
        if (enrolledStudent.getModules().stream().noneMatch(existing -> existing.getId().equals(module.getId()))) {
            enrolledStudent.getModules().add(module);
            userRepository.save(enrolledStudent);
        }
    }

    private void addAbsence(String moduleName, AbsenceStatus status, LocalDate date) {
        Module module = moduleRepository.findByName(moduleName).orElseThrow();
        addAbsence(module, status, date);
    }

    private void addAbsence(Module module, AbsenceStatus status, LocalDate date) {
        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setStatus(status);
        absence.setDate(date);
        absenceRepository.save(absence);
    }
}
