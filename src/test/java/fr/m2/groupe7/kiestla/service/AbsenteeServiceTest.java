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
class AbsenteeServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private ModuleService moduleService;
    @InjectMocks
    private AbsenteeService absenteeService;

    @Test
    void studentWithThreeUnjustifiedAbsences_isListed() {
        when(moduleService.getModulesForTeacher("teacher"))
                .thenReturn(List.of(new ModuleSummary(10L, "Programmation", "Teacher")));
        User alice = user(1L, "Alice", "Durand");
        when(absenceRepository.findByModuleId(10L))
                .thenReturn(absences(alice, 3, AbsenceStatus.UNJUSTIFIED));

        List<AbsenteeSummary> result = absenteeService.getAbsenteesForTeacher("teacher", SortBy.MODULE);

        assertEquals(1, result.size());
        assertEquals("Durand", result.get(0).lastName());
        assertEquals("Programmation", result.get(0).moduleName());
        assertEquals(3, result.get(0).absenceCount());
    }

    @Test
    void studentWithTwoUnjustifiedAbsences_isNotListed() {
        when(moduleService.getModulesForTeacher("teacher"))
                .thenReturn(List.of(new ModuleSummary(10L, "Programmation", "Teacher")));
        when(absenceRepository.findByModuleId(10L))
                .thenReturn(absences(user(1L, "Alice", "Durand"), 2, AbsenceStatus.UNJUSTIFIED));

        assertTrue(absenteeService.getAbsenteesForTeacher("teacher", SortBy.MODULE).isEmpty());
    }

    @Test
    void justifiedAbsences_areNotCounted() {
        when(moduleService.getModulesForTeacher("teacher"))
                .thenReturn(List.of(new ModuleSummary(10L, "Programmation", "Teacher")));
        User alice = user(1L, "Alice", "Durand");
        List<Absence> list = new ArrayList<>(absences(alice, 2, AbsenceStatus.UNJUSTIFIED));
        list.addAll(absences(alice, 5, AbsenceStatus.JUSTIFIED));
        when(absenceRepository.findByModuleId(10L)).thenReturn(list);

        assertTrue(absenteeService.getAbsenteesForTeacher("teacher", SortBy.MODULE).isEmpty());
    }

    @Test
    void sortByModuleAndByStudent() {
        when(moduleService.getModulesForTeacher("teacher")).thenReturn(List.of(
                new ModuleSummary(10L, "Maths", "Teacher"),
                new ModuleSummary(20L, "Algo", "Teacher")));
        User zoe = user(1L, "Zoe", "Adams");
        User bob = user(2L, "Bob", "Zimmer");
        when(absenceRepository.findByModuleId(10L)).thenReturn(absences(bob, 3, AbsenceStatus.UNJUSTIFIED));
        when(absenceRepository.findByModuleId(20L)).thenReturn(absences(zoe, 3, AbsenceStatus.UNJUSTIFIED));

        List<AbsenteeSummary> byModule = absenteeService.getAbsenteesForTeacher("teacher", SortBy.MODULE);
        List<AbsenteeSummary> byStudent = absenteeService.getAbsenteesForTeacher("teacher", SortBy.STUDENT);

        assertEquals("Algo", byModule.get(0).moduleName());   // Algo < Maths
        assertEquals("Adams", byStudent.get(0).lastName());   // Adams < Zimmer
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