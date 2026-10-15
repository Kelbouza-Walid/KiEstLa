package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.AbsenteeSummary;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;

@ExtendWith(MockitoExtension.class)
class AbsenteeSecretaryTest {

    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private ModuleService moduleService;
    @InjectMocks
    private AbsenteeService service;

    private void givenTwoModules() {
        when(moduleService.getAllModulesSummary()).thenReturn(List.of(
                new ModuleSummary(10L, "Maths", "Teacher"),
                new ModuleSummary(20L, "Algo", "Teacher")));
        User zoe = user(1L, "Zoe", "Adams");
        User bob = user(2L, "Bob", "Zimmer");
        lenient().when(absenceRepository.findByModuleId(10L)).thenReturn(absences(zoe, 3, AbsenceStatus.UNJUSTIFIED));
        lenient().when(absenceRepository.findByModuleId(20L)).thenReturn(absences(bob, 3, AbsenceStatus.UNJUSTIFIED));
    }

    @Test
    void allModules_sortedByModule() {
        givenTwoModules();

        List<AbsenteeSummary> result = service.getAbsenteesForSecretary(null, null, SortBy.MODULE);

        assertEquals(List.of("Algo", "Maths"), result.stream().map(AbsenteeSummary::moduleName).toList());
    }

    @Test
    void allModules_sortedByStudent() {
        givenTwoModules();

        List<AbsenteeSummary> result = service.getAbsenteesForSecretary(null, null, SortBy.STUDENT);

        assertEquals(List.of("Adams", "Zimmer"), result.stream().map(AbsenteeSummary::lastName).toList());
    }

    @Test
    void filterByModule() {
        givenTwoModules();

        List<AbsenteeSummary> result = service.getAbsenteesForSecretary(10L, null, SortBy.MODULE);

        assertEquals(1, result.size());
        assertEquals("Adams", result.get(0).lastName());
    }

    @Test
    void filterByStudentName_isCaseInsensitive() {
        givenTwoModules();

        List<AbsenteeSummary> result = service.getAbsenteesForSecretary(null, " bob ", SortBy.MODULE);

        assertEquals(1, result.size());
        assertEquals("Zimmer", result.get(0).lastName());
    }

    @Test
    void mixedAbsences_onlyUnjustifiedCount_andEmptyList() {
        when(moduleService.getAllModulesSummary()).thenReturn(List.of(new ModuleSummary(10L, "Maths", "Teacher")));
        User alice = user(3L, "Alice", "Durand");
        List<Absence> list = new ArrayList<>(absences(alice, 2, AbsenceStatus.UNJUSTIFIED));
        list.addAll(absences(alice, 2, AbsenceStatus.JUSTIFIED));
        when(absenceRepository.findByModuleId(10L)).thenReturn(list);

        assertTrue(service.getAbsenteesForSecretary(null, null, SortBy.MODULE).isEmpty());
    }

    private User user(Long id, String firstName, String lastName) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        return u;
    }

    private List<Absence> absences(User student, int count, AbsenceStatus status) {
        List<Absence> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Absence a = new Absence();
            a.setStudent(student);
            a.setDate(LocalDate.of(2026, 10, 1).plusDays(i));
            a.setStatus(status);
            list.add(a);
        }
        return list;
    }
}