package com.internship.platform.entity;

import com.internship.platform.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column
    private String phone;

    @Column
    private String department;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Stagiaire stagiaire;

    @OneToMany(mappedBy = "encadrant", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Stagiaire> stagiairesEncadres = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
