package fr.m2.groupe7.kiestla.dto;

import java.time.LocalDate;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;

public record StudentAbsenceSummary(Long id, String moduleName, LocalDate date,
                                    AbsenceStatus status, String justification) {

    public static StudentAbsenceSummary from(Absence a) {
        return new StudentAbsenceSummary(a.getId(), a.getModule().getName(),
                a.getDate(), a.getStatus(), a.getJustification());
    }
}