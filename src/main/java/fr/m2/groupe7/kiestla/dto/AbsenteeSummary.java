package fr.m2.groupe7.kiestla.dto;

public record AbsenteeSummary(
        Long studentId,
        String firstName,
        String lastName,
        Long moduleId,
        String moduleName,
        int absenceCount) {
}