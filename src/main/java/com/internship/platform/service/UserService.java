package com.internship.platform.service;

import com.internship.platform.dto.user.UserDto;
import com.internship.platform.dto.user.UserUpdateRequest;
import com.internship.platform.entity.User;
import com.internship.platform.entity.enums.Role;
import com.internship.platform.exception.BusinessException;
import com.internship.platform.exception.ResourceNotFoundException;
import com.internship.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    public Page<UserDto> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::toDto);
    }

    public UserDto getUserById(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        User user = findById(id);
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getDepartment() != null)
            user.setDepartment(request.getDepartment());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = findById(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        if (user.getStagiaire() != null) {
            throw new BusinessException("Impossible de supprimer un utilisateur avec un profil stagiaire actif");
        }
        userRepository.delete(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé: " + email));
    }

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole().name());
        dto.setEnabled(user.isEnabled());
        dto.setPhone(user.getPhone());
        dto.setDepartment(user.getDepartment());
        return dto;
    }
}
