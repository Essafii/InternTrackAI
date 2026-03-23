package com.internship.platform.controller;

import com.internship.platform.dto.user.UserDto;
import com.internship.platform.dto.user.UserUpdateRequest;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Lister tous les utilisateurs")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Lister les utilisateurs par rôle")
    public ResponseEntity<Page<UserDto>> getUsersByRole(@PathVariable Role role, Pageable pageable) {
        return ResponseEntity.ok(userService.getUsersByRole(role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'ENCADRANT')")
    @Operation(summary = "Obtenir un utilisateur par ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Activer/désactiver un utilisateur")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
