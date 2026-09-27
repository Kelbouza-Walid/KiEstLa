package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Role;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String lastName;
    private String firstName;
    private String login;
    private Role role;
    private Set<Long> moduleIds;
}
