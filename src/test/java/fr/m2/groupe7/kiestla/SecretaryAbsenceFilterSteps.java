package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
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
 * Steps for filtering the "All Absences" list of the secretary page
 * (FilterAbsences.feature).
 * The Background "I have already logged in" is shared and defined elsewhere.
 */
public class SecretaryAbsenceFilterSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private final Map<String, Module> modules = new HashMap<>();

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // ---------- Given ----------

    @Given("Several students have absences in several modules")
    public void several_students_have_absences_in_several_modules() {
        Module info = createModule("Informatique Responsable");
        Module test = createModule("Test fonctionnel");
        User gaspard = createStudent("Gaspard", "Quentin", "gquentin");
        User noam = createStudent("Noam", "Faivre", "nfaivre");

        addAbsence(gaspard, info, LocalDate.of(2026, 10, 1));
        addAbsence(gaspard, test, LocalDate.of(2026, 10, 2));
        addAbsence(noam, info, LocalDate.of(2026, 10, 3));
        addAbsence(noam, test, LocalDate.of(2026, 10, 4));
        addAbsence(noam, test, LocalDate.of(2026, 10, 5));
    }

    // ---------- When ----------

    @When("I filter the absence list by module {string}")
    public void i_filter_by_module(String moduleName) {
        getPage().navigate(BASE_URL + "/secretary");
        getPage().getByTestId("select-absence-module").selectOption(String.valueOf(modules.get(moduleName).getId()));
        getPage().getByTestId("btn-filter-absences").click();
        getPage().waitForURL(url -> url.contains("absenceModuleId=" + modules.get(moduleName).getId()));
    }

    @When("I filter the absence list by student {string}")
    public void i_filter_by_student(String studentQuery) {
        getPage().navigate(BASE_URL + "/secretary");
        getPage().getByTestId("input-absence-student").fill(studentQuery);
        getPage().getByTestId("btn-filter-absences").click();
        getPage().waitForURL(url -> url.contains("absenceStudent=" + studentQuery));
    }

    // ---------- Then ----------

    @Then("I only see absences from module {string}")
    public void i_only_see_absences_from_module(String moduleName) {
        // the table has no module column: check the absences of that module, newest
        // first
        List<String> expected = absenceRepository.findByModuleIdOrderByDateDesc(modules.get(moduleName).getId())
                .stream()
                .map(a -> a.getStudent().getFirstName() + " " + a.getStudent().getLastName())
                .toList();
        assertThat(getPage().getByTestId("absence-student")).hasCount(expected.size());
        assertEquals(expected, getPage().getByTestId("absence-student").allTextContents());
    }

    @Then("I only see absences of student {string}")
    public void i_only_see_absences_of_student(String studentName) {
        assertThat(getPage().getByTestId("absence-student")).hasCount(3);
        assertEquals(List.of(studentName, studentName, studentName),
                getPage().getByTestId("absence-student").allTextContents());
    }

    // ---------- Helpers ----------

    private User createStudent(String firstName, String lastName, String login) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(Role.STUDENT);
        return userRepository.save(user);
    }

    private Module createModule(String name) {
        Module m = new Module();
        m.setName(name);
        m = moduleRepository.save(m);
        modules.put(name, m);
        return m;
    }

    private void addAbsence(User student, Module module, LocalDate date) {
        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(date);
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
        absenceRepository.save(absence);
    }
}
