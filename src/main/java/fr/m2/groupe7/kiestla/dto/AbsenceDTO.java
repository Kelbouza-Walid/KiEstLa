package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long moduleId;
    private String moduleName;
    private LocalDate date;
    private AbsenceStatus status;
    private String justification;
}
