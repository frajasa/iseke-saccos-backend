package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.dto.AuthPayloadDTO;
import tz.co.iseke.inputs.CreateUserInput;
import tz.co.iseke.dto.UserDto;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.entity.User;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.UserRepository;
import tz.co.iseke.security.JwtTokenProvider;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditService auditService;

    public AuthPayloadDTO login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Invalid username or password"));

        if (!user.getIsActive()) {
            throw new BusinessException("User account is deactivated");
        }

        // Check if account is locked
        if (passwordPolicyService.isAccountLocked(user)) {
            throw new BusinessException("Account is locked due to too many failed login attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Record failed login attempt
            passwordPolicyService.recordFailedLogin(user);
            userRepository.save(user);
            auditService.logAction("LOGIN_FAILED", "User", user.getId());
            throw new BusinessException("Invalid username or password");
        }

        // Successful login - reset failed attempts
        passwordPolicyService.recordSuccessfulLogin(user);

        // Check password expiry
        boolean forceChange = Boolean.TRUE.equals(user.getForcePasswordChange())
                || passwordPolicyService.isPasswordExpired(user);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate token
        String token = tokenProvider.generateToken(user);

        UserDto userDTO = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .branch(user.getBranch())
                .build();

        auditService.logAction("LOGIN_SUCCESS", "User", user.getId());

        return AuthPayloadDTO.builder()
                .token(token)
                .user(userDTO)
                .forcePasswordChange(forceChange)
                .build();
    }

    public Boolean changePassword(String oldPassword, String newPassword) {
        // Get current user from security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Validate password complexity
        passwordPolicyService.validatePasswordComplexity(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        passwordPolicyService.setPasswordExpiry(user);
        user.setForcePasswordChange(false);
        userRepository.save(user);

        auditService.logAction("PASSWORD_CHANGED", "User", user.getId());

        return true;
    }

    @Transactional
    public User createUser(CreateUserInput input) {
        // Validate password match
        if (!input.getPassword().equals(input.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        // Validate password complexity
        passwordPolicyService.validatePasswordComplexity(input.getPassword());

        // Ensure email is unique
        if (userRepository.existsByEmail(input.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        String firstInitial = input.getFirstName() != null && !input.getFirstName().isEmpty()
                ? String.valueOf(Character.toLowerCase(input.getFirstName().charAt(0)))
                : "x";

        String baseUsername = (firstInitial + input.getLastName())
                .toLowerCase()
                .replaceAll("[^a-z]", "");

        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        // Compose full name safely
        String fullName = String.join(" ",
                input.getFirstName(),
                input.getMiddleName() != null ? input.getMiddleName() : "",
                input.getLastName()
        ).replaceAll("\\s+", " ").trim();

        // Create entity
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(input.getPassword()))
                .firstName(input.getFirstName())
                .middleName(input.getMiddleName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .phoneNumber(input.getPhoneNumber())
                .role(input.getRole())
                .isActive(true)
                .forcePasswordChange(true)
                .build();

        // Set password expiry
        passwordPolicyService.setPasswordExpiry(user);

        // Attach branch if provided
        if (input.getBranchId() != null) {
            Branch branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            user.setBranch(branch);
        }

        // Save and return
        User savedUser = userRepository.save(user);

        auditService.logAction("USER_CREATED", "User", savedUser.getId());

        return savedUser;
    }

}
