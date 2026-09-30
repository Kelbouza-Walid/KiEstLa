package fr.m2.groupe7.kiestla;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

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

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SecretaryModuleDetailsSteps {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Page getPage() {
        return PlaywrightHooks.getPage();
    }

    // =====================================================================
    // Feature 949: Viewing the absences of a student in a module
    // =====================================================================

    @Given("The student has multiple absences registered in the module")
    public void the_student_has_multiple_absences_registered_in_the_module() {
        User student = new User();
        student.setFirstName("Gaspard");
        student.setLastName("Quentin");
        student.setLogin("gquentin");
        student.setPassword("pwd");
        student.setRole(Role.STUDENT);
        student = userRepository.save(student);

        Module module = new Module();
        module.setName("Functionnal testing");
        moduleRepository.save(module);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.now());
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
        Absence saved = absenceRepository.save(absence);

        Absence absence2 = new Absence();
        absence2.setStudent(student);
        absence2.setModule(module);
        absence2.setDate(LocalDate.now());
        absence2.setStatus(AbsenceStatus.UNJUSTIFIED);
        Absence saved2 = absenceRepository.save(absence2);
    }

    @When("I open the absence list for that student in that module")
    public void i_open_the_absence_list_for_that_student_in_that_module() {
        // TODO: Naviguer vers la page listant les absences d'un étudiant dans un module
        // Not implemented yet
    }

    @Then("I see all of his absences listed individually")
    public void i_see_all_of_his_absences_listed_individually() {
        // TODO: Vérifier que toutes les absences de l'étudiant sont affichées
        // Not implemented yet
    }

    // =====================================================================
    // Feature 1183: Absence list is empty for a module with no absences
    // =====================================================================

    @Given("The module has no absences registered")
    public void the_module_has_no_absences_registered() {
        Module module = new Module();
        module.setName("Functionnal testing");
        moduleRepository.save(module);
    }

    @When("I open the absence list for that module")
    public void i_open_the_absence_list_for_that_module() {
        getPage().navigate("http://localhost:8080/secretary");
        getPage().getByTestId("btn-module-details").click();
    }

    @Then("The list is empty")
    public void the_list_is_empty() { //
        assertThat(getPage().getByTestId("module-status")).hasText("No absences found for this module.");
    }

    // =====================================================================
    // Feature 959: Viewing module absences sorted by student
    // =====================================================================

    @Given("The module has multiple students with registered absences")
    public void the_module_has_multiple_students_with_registered_absences() {
        // TODO: Créer un module avec plusieurs étudiants ayant des absences
        throw new PendingException();
    }

    // Note: "I open the absence list for that module" est réutilisé depuis le bloc 1183

    @When("I sort the list by student")
    public void i_sort_the_list_by_student() {
        // TODO: Cliquer sur le tri par étudiant
        // Not implemented yet
        throw new PendingException();
    }

    @Then("I see all absences of the module ordered by student")
    public void i_see_all_absences_of_the_module_ordered_by_student() {
        // TODO: Vérifier que les absences sont triées par étudiant
        // Not implemented yet
        throw new PendingException();
    }

    // =====================================================================
    // Feature 1168: Viewing absences sorted by module
    // =====================================================================

    @Given("Several modules have students with registered absences")
    public void several_modules_have_students_with_registered_absences() {
        // TODO: Créer plusieurs modules avec des étudiants ayant des absences
        throw new PendingException();
    }

    @When("I open the absence list")
    public void i_open_the_absence_list() {
        // TODO: Naviguer vers la page listant toutes les absences
        throw new PendingException();
    }

    @When("I sort the list by module")
    public void i_sort_the_list_by_module() {
        // TODO: Cliquer sur le tri par module
        throw new PendingException();
    }

    @Then("I see all absences ordered by module")
    public void i_see_all_absences_ordered_by_module() {
        // TODO: Vérifier que les absences sont triées par module
        throw new PendingException();
    }
}
