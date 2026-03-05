package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.UpdateUserInput;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.entity.User;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Find all users
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Find user by ID
     */
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Update user details
     */
    public User updateUser(UUID id, UpdateUserInput input) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Get current user for audit trail
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // Update email if changed and validate uniqueness
        if (input.getEmail() != null && !input.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(input.getEmail())) {
                throw new BusinessException("Email already exists");
            }
            user.setEmail(input.getEmail());
        }

        // Update other fields
        if (input.getFirstName() != null) {
            user.setFirstName(input.getFirstName());
        }
        if (input.getMiddleName() != null) {
            user.setMiddleName(input.getMiddleName());
        }
        if (input.getLastName() != null) {
            user.setLastName(input.getLastName());
        }
        if (input.getPhoneNumber() != null) {
            user.setPhoneNumber(input.getPhoneNumber());
        }
        if (input.getRole() != null) {
            user.setRole(input.getRole());
        }
        if (input.getIsActive() != null) {
            user.setIsActive(input.getIsActive());
        }

        // Update branch if provided
        if (input.getBranchId() != null) {
            Branch branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            user.setBranch(branch);
        }

        // Set audit fields
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUsername);

        User savedUser = userRepository.save(user);
        auditService.logAction("USER_UPDATED", "User", savedUser.getId());
        return savedUser;
    }

    /**
     * Deactivate user (soft delete)
     */
    public Boolean deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Prevent self-deactivation
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessException("You cannot deactivate your own account");
        }

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUsername);
        userRepository.save(user);

        auditService.logAction("USER_DEACTIVATED", "User", user.getId());

        return true;
    }

    /**
     * Hard delete user (use with caution - only for testing/admin purposes)
     */
    public Boolean deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Prevent self-deletion
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw new BusinessException("You cannot delete your own account");
        }

        userRepository.delete(user);
        return true;
    }

    /**
     * Activate user
     */
    public Boolean activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        user.setUpdatedBy(currentUsername);

        userRepository.save(user);
        return true;
    }

    /**
     * Reset user password (admin function)
     */
    public Boolean resetPassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Validate password complexity
        passwordPolicyService.validatePasswordComplexity(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(true);
        passwordPolicyService.setPasswordExpiry(user);
        user.setUpdatedAt(LocalDateTime.now());

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        user.setUpdatedBy(currentUsername);

        userRepository.save(user);
        auditService.logAction("PASSWORD_RESET", "User", user.getId());
        return true;
    }
}
