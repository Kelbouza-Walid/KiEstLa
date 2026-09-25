package fr.m2.groupe7.kiestla.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String status;
}
