package fr.m2.groupe7.kiestla.service;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;

import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

import com.microsoft.playwright.Page;

import fr.m2.groupe7.kiestla.PlaywrightHooks;
import fr.m2.groupe7.kiestla.TestDataCleaner;
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
    private TestDataCleaner testDataCleaner;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    // Background of feature 1247

    @Given("I am in several different modules")
    public void i_am_in_several_different_modules() {
        // The shared Background step "I am logged in" (TeacherAddStudentTest) has already run:
        // we start from a clean database and log in again on our own page.
        testDataCleaner.cleanDatabase();

        User teacher = createUser("Ref", "Teacher", "testteacher", Role.TEACHER);
        User zoe = createUser("Zoe", "Adams", "zadams", Role.STUDENT);
        User bob = createUser("Bob", "Zimmer", "bzimmer", Role.STUDENT);
        User carl = createUser("Carl", "Zorro", "czorro", Role.STUDENT);
        User otherTeacher = createUser("Other", "Prof", "other", Role.TEACHER);

        Module maths = createModule("Maths", teacher);
        Module algo = createModule("Algo", teacher);
        Module physique = createModule("Physique", otherTeacher);

        enroll(teacher, maths);
        enroll(teacher, algo);
        enroll(otherTeacher, physique);
        enroll(zoe, maths);
        enroll(zoe, algo);
        enroll(bob, algo);
        enroll(carl, physique);

        addAbsences(zoe, maths, 1, AbsenceStatus.UNJUSTIFIED);
        addAbsences(zoe, algo, 1, AbsenceStatus.UNJUSTIFIED);
        addAbsences(bob, algo, 1, AbsenceStatus.JUSTIFIED);
        // Carl is not in any of my modules: his absences must never be shown
        addAbsences(carl, physique, 2, AbsenceStatus.UNJUSTIFIED);

        login("testteacher", "testpassword");
    }


    @When("I open the list of all the absences")
    public void i_open_the_list_of_all_the_absences() {
        getPage().navigate(BASE_URL + "/teacher/absences");
    }

    @Then("I should see the list of absences sorted by student")
    public void i_should_see_the_list_of_absences_sorted_by_student() {
        assertThat(getPage().getByTestId("absence-row")).hasCount(3);
        assertEquals(List.of("Adams", "Adams", "Zimmer"), getPage().getByTestId("absence-last-name").allTextContents());
        assertEquals(List.of("Algo", "Maths", "Algo"), getPage().getByTestId("absence-module").allTextContents());
    }

    @Then("I should not see the absences of students who are not in my modules")
    public void i_should_not_see_the_absences_of_students_who_are_not_in_my_modules() {
        assertThat(getPage().getByTestId("absence-row")).hasCount(3);
        assertFalse(getPage().getByTestId("absence-last-name").allTextContents().contains("Zorro"));
        assertFalse(getPage().getByTestId("absence-module").allTextContents().contains("Physique"));
    }

    private boolean isGlobalAbsencesPage() {
        return getPage().url().contains("/teacher/absences");
    }

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // The Background of the feature has no step, so the data is prepared here.
    @When("I open the list of absenteist students")
    public void i_open_the_list_of_absenteist_students() {
        testDataCleaner.cleanDatabase();

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

    // Shared by 1247 (all the absences of my modules) and 1249 (students with 3 or more unjustified absences)
    @Then("I should see the list of absences sorted by module")
    public void i_should_see_the_list_of_absences_sorted_by_module() {
        if (isGlobalAbsencesPage()) {
            // 1247: every absence of my modules, Algo (Zoe, Bob) then Maths (Zoe)
            assertThat(getPage().getByTestId("absence-row")).hasCount(3);
            assertEquals(List.of("Algo", "Algo", "Maths"), getPage().getByTestId("absence-module").allTextContents());
            assertEquals(List.of("Adams", "Zimmer", "Adams"), getPage().getByTestId("absence-last-name").allTextContents());
            return;
        }
        // 1249: Alice has only 2 unjustified absences and is excluded. Module order: Algo (Bob) then Maths (Zoe)
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