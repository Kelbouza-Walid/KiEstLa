package fr.m2.groupe7.kiestla.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleDTO {
    private Long id;
    private String name;
    private Long referenceTeacherId;
    private String referenceTeacherName;
    private Set<Long> studentIds;
}
