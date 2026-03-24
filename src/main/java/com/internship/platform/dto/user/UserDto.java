package com.internship.platform.dto.user;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean enabled;
    private String phone;
    private String department;
}
