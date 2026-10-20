package fr.m2.groupe7.kiestla;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;

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
 * Steps for the secretary follow-up list (ticket TFKIES26-38, features 1177, 1178, 1179, 1181).
 * The Background "I have already logged in" and the steps "I sort the list by ..." are shared
 * and defined elsewhere.
 */
public class SecretaryAbsenteesSteps {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Module module;
    private User student;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // ---------- Given ----------

    @Given("One or more students have 3 or more unjustified absences in the module")
    public void students_have_3_or_more_in_the_module() {
        module = createModule("Maths");
        addAbsences(createStudent("Bob", "Zimmer", "bzimmer"), module, 3, AbsenceStatus.UNJUSTIFIED);
        addAbsences(createStudent("Zoe", "Adams", "zadams"), module, 4, AbsenceStatus.UNJUSTIFIED);
    }

    @Given("One or more students have 3 or more unjustified absences in a module")
    public void students_have_3_or_more_in_a_module() {
        module = createModule("Maths");
        Module algo = createModule("Algo");
        addAbsences(createStudent("Zoe", "Adams", "zadams"), module, 3, AbsenceStatus.UNJUSTIFIED);
        addAbsences(createStudent("Bob", "Zimmer", "bzimmer"), algo, 3, AbsenceStatus.UNJUSTIFIED);
    }

    @Given("A student has less than 3 unjustified absences in the module")
    public void student_has_less_than_3() {
        module = createModule("Maths");
        student = createStudent("Alice", "Durand", "adurand");
        addAbsences(student, module, 2, AbsenceStatus.UNJUSTIFIED);
        // control student: keeps the list non-empty so we know the page is loaded
        addAbsences(createStudent("Bob", "Zimmer", "bzimmer"), module, 3, AbsenceStatus.UNJUSTIFIED);
    }

    @Given("A student has 2 unjustified absences and 2 justified absences in the module")
    public void student_has_mixed_absences() {
        module = createModule("Maths");
        student = createStudent("Alice", "Durand", "adurand");
        addAbsences(student, module, 2, AbsenceStatus.UNJUSTIFIED);
        addAbsences(student, module, 2, AbsenceStatus.JUSTIFIED);
        addAbsences(createStudent("Bob", "Zimmer", "bzimmer"), module, 3, AbsenceStatus.UNJUSTIFIED);
    }

    @Given("No student in the module has 3 or more unjustified absences")
    public void no_student_reaches_the_threshold() {
        module = createModule("Maths");
        addAbsences(createStudent("Alice", "Durand", "adurand"), module, 2, AbsenceStatus.UNJUSTIFIED);
    }

    // ---------- When ----------

    @When("I open the list of students with 3 or more unjustified absences")
    public void i_open_the_list() {
        getPage().navigate(BASE_URL + "/secretary");
    }

    @When("I open the list of students with 3 or more unjustified absences for that module")
    public void i_open_the_list_for_that_module() {
        getPage().navigate(BASE_URL + "/secretary");
        getPage().getByTestId("select-absentee-module").selectOption(String.valueOf(module.getId()));
        getPage().getByTestId("btn-filter-absentees").click();
        getPage().waitForURL(url -> url.contains("moduleId="));
    }

    // ---------- Then ----------

    @Then("I see all students with 3 or more unjustified absences ordered by student")
    public void ordered_by_student() {
        assertThat(getPage().getByTestId("absentee-row")).hasCount(2);
        assertEquals(List.of("Adams", "Zimmer"), getPage().getByTestId("absentee-last-name").allTextContents());
    }

    @Then("I see all students with 3 or more unjustified absences ordered by module")
    public void ordered_by_module() {
        assertThat(getPage().getByTestId("absentee-row")).hasCount(2);
        assertEquals(List.of("Algo", "Maths"), getPage().getByTestId("absentee-module").allTextContents());
    }

    @Then("That student does not appear in the list")
    public void that_student_does_not_appear() {
        // only the control student (Zimmer) is listed
        assertThat(getPage().getByTestId("absentee-row")).hasCount(1);
        List<String> lastNames = getPage().getByTestId("absentee-last-name").allTextContents();
        assertEquals(List.of("Zimmer"), lastNames);
        assertFalse(lastNames.contains(student.getLastName()));
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
        return moduleRepository.save(m);
    }

    private void addAbsences(User student, Module module, int count, AbsenceStatus status) {
        for (int i = 0; i < count; i++) {
            Absence absence = new Absence();
            absence.setStudent(student);
            absence.setModule(module);
            absence.setDate(LocalDate.of(2026, 10, 1).plusDays(i));
            absence.setStatus(status);
            absenceRepository.save(absence);
        }
    }
}