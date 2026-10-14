package fr.m2.groupe7.kiestla.service;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.PlaywrightHooks;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the feature "See which students have 3 or more absences" (ticket TFKIES26-32).
 */
public class TeacherAbsenteesSteps {

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

    // The Background of the feature has no step, so the data is prepared here.
    @When("I open the list of absenteist students")
    public void i_open_the_list_of_absenteist_students() {
        absenceRepository.deleteAll();
        moduleRepository.deleteAll();
        userRepository.deleteAll();

        User teacher = createUser("Ref", "Teacher", "teacher", Role.TEACHER);
        User zoe = createUser("Zoe", "Adams", "zadams", Role.STUDENT);
        User bob = createUser("Bob", "Zimmer", "bzimmer", Role.STUDENT);
        User alice = createUser("Alice", "Durand", "adurand", Role.STUDENT);

        Module maths = createModule("Maths", teacher);
        Module algo = createModule("Algo", teacher);

        for (User u : List.of(teacher, zoe, bob, alice)) {
            enroll(u, maths);
        }
        enroll(teacher, algo);
        enroll(bob, algo);

        // Zoe: 3 unjustified in Maths -> listed
        addAbsences(zoe, maths, 3, AbsenceStatus.UNJUSTIFIED);
        // Bob: 3 unjustified in Algo -> listed
        addAbsences(bob, algo, 3, AbsenceStatus.UNJUSTIFIED);
        // Alice: 2 unjustified + 5 justified in Maths -> NOT listed
        addAbsences(alice, maths, 2, AbsenceStatus.UNJUSTIFIED);
        addAbsences(alice, maths, 5, AbsenceStatus.JUSTIFIED);

        login("teacher", "testpassword");
        getPage().navigate(BASE_URL + "/teacher/absentees");
    }

    @When("I select {string}")
    public void i_select(String option) {
        getPage().getByText(option, new Page.GetByTextOptions().setExact(true)).click();
    }

    @Then("I should see the list of absences sorted by module")
    public void i_should_see_the_list_of_absences_sorted_by_module() {
        // Alice is excluded, so only 2 rows. Module order: Algo (Bob) then Maths (Zoe)
        assertThat(getPage().getByTestId("absentee-row")).hasCount(2);
        assertEquals(List.of("Algo", "Maths"), getPage().getByTestId("absentee-module").allTextContents());
        assertEquals(List.of("Zimmer", "Adams"), getPage().getByTestId("absentee-last-name").allTextContents());
    }

    // The typo "absenteit" comes from the .feature file, it must stay identical
    @Then("I should see the list of absenteit students")
    public void i_should_see_the_list_of_absenteit_students() {
        // Student order: Adams (Zoe) then Zimmer (Bob)
        assertThat(getPage().getByTestId("absentee-row")).hasCount(2);
        assertEquals(List.of("Adams", "Zimmer"), getPage().getByTestId("absentee-last-name").allTextContents());
        assertEquals(List.of("Maths", "Algo"), getPage().getByTestId("absentee-module").allTextContents());
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

    private Module createModule(String name, User referenceTeacher) {
        Module module = new Module();
        module.setName(name);
        module.setReferenceTeacher(referenceTeacher);
        return moduleRepository.save(module);
    }

    private void enroll(User user, Module moduleToJoin) {
        User withModules = userRepository.findWithModulesById(user.getId()).orElseThrow();
        withModules.getModules().add(moduleToJoin);
        userRepository.save(withModules);
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

    private void login(String login, String password) {
        getPage().navigate(BASE_URL + "/login");
        getPage().fill("input[name='login']", login);
        getPage().fill("input[name='password']", password);
        getPage().click("button[type='submit']");
        getPage().waitForURL("**/teacher");
    }
}