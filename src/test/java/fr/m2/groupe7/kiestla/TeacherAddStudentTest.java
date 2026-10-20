package fr.m2.groupe7.kiestla;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TeacherAddStudentTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Autowired
    private TestDataCleaner testDataCleaner;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    // Shared state between steps and scenario
    private User referenceTeacher;
    private User teacherToAdd;
    private User studentToAdd;
    private User student;
    private Module module;
    private boolean testDataInitialized;

    // Playwright setup
    private Playwright playwright;
    private Browser browser;
    private Page page;

    @Before
    public void setupBrowser() {
        playwright = Playwright.create();
        boolean isCI = System.getenv("CI") != null || System.getProperty("CI") != null;
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(isCI)
                        .setSlowMo(isCI ? 0 : 1000));
        page = browser.newPage();
    }

    @After
    public void closeBrowser() {
        if (browser != null)
            browser.close();
        if (playwright != null)
            playwright.close();
    }

    // ---------- Background ----------

    @Given("I am a reference teacher")
    public void i_am_a_reference_teacher() {
        initializeTestData();
    }

    private void initializeTestData() {
        if (testDataInitialized) {
            return;
        }

        testDataCleaner.cleanDatabase();

        referenceTeacher = createUser("Ref", "Teacher", "testteacher", Role.TEACHER);
        student = createUser("Alice", "Durand", "student1", Role.STUDENT);

        referenceTeacher.setPassword("testpassword");

        Module newModule = new Module();
        newModule.setName("Programmation");
        newModule.setReferenceTeacher(referenceTeacher);
        module = moduleRepository.save(newModule);

        // User is the proprietary side of the relation : we will register via the user
        enroll(referenceTeacher, module);
        enroll(student, module);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.now());
        absence.setStatus(AbsenceStatus.values()[0]); // replace by the desired value
        absenceRepository.save(absence);
        testDataInitialized = true;
    }

    @Given("I am logged in")
    public void i_am_logged_in() {
        initializeTestData();
        login("testteacher", "testpassword");
    }

    // ---------- Scenario ----------

    @Given("The teacher exists in the database")
    public void the_teacher_exists_in_the_database() {
        teacherToAdd = createUser("Second", "Martin", "teacher2", Role.TEACHER);
    }

    @When("I select the teacher")
    public void i_select_the_teacher() {
        page.navigate(BASE_URL + "/module-details/" + module.getId());
        page.selectOption("select[name='teacherId']", String.valueOf(teacherToAdd.getId()));
        page.click("form[action$='/teachers'] button[type='submit']");
    }

    @Then("They are added to the list of teachers in my module")
    public void they_are_added_to_the_list_of_teachers_in_my_module() {
        assertThat(page.locator("body")).containsText("Le professeur a été ajouté au module.");
        assertThat(page.locator("body")).containsText("Martin");
    }

    @Then("The teacher can now see all absences of the student of my module")
    public void the_teacher_can_now_see_all_absences_of_the_student_of_my_module() {
        page.close();
        page = browser.newPage();
        login("teacher2", "testpassword");

        page.navigate(BASE_URL + "/module-details/" + module.getId());
        assertThat(page.locator("body")).containsText("Durand");
    }

    // test 1242

    @Given("The student exists in the database")
    public void the_student_exists_in_the_database() {
        // Existing student but not yet registerd to the module
        studentToAdd = createUser("Bob", "Leroy", "student2", Role.STUDENT);
    }

    @When("I select the student")
    public void i_select_the_student() {
        page.navigate(BASE_URL + "/module-details/" + module.getId());
        page.selectOption("select[name='studentId']", String.valueOf(studentToAdd.getId()));
        page.click("form[action$='/students'] button[type='submit']");
    }

    @Then("They are added to the list of students in my module")
    public void they_are_added_to_the_list_of_students_in_my_module() {
        assertThat(page.locator("body")).containsText("L’étudiant a été ajouté au module.");
        assertThat(page.locator("body")).containsText("Leroy");
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

    private void enroll(User user, Module moduleToJoin) {
        User withModules = userRepository.findWithModulesById(user.getId()).orElseThrow();
        withModules.getModules().add(moduleToJoin);
        userRepository.save(withModules);
    }

    private void login(String login, String password) {
        page.navigate(BASE_URL + "/login");
        page.fill("input[name='login']", login);
        page.fill("input[name='password']", password);
        page.click("button[type='submit']");
        if (!page.url().endsWith("/teacher")) {
            throw new AssertionError("Login failed for '" + login + "'. Page content: "
                    + page.locator("body").innerText());
        }
    }
}
