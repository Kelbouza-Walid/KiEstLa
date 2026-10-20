package fr.m2.groupe7.kiestla.dto;

import java.time.LocalDate;

import fr.m2.groupe7.kiestla.entity.AbsenceStatus;

public record TeacherAbsenceRow(
        Long id,
        String moduleName,
        String firstName,
        String lastName,
        LocalDate date,
        AbsenceStatus status,
        String motiveTitle,
        String motiveDescription) {
}