package tz.co.iseke.service;

import org.springframework.stereotype.Service;
import tz.co.iseke.entity.User;
import tz.co.iseke.exception.BusinessException;

import java.time.LocalDateTime;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    public void validatePasswordComplexity(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BusinessException("Password must contain at least one special character");
        }
    }

    public boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }
        if (user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            return true;
        }
        return false;
    }

    public boolean isPasswordExpired(User user) {
        if (user.getPasswordExpiresAt() == null) {
            return false;
        }
        return user.getPasswordExpiresAt().isBefore(LocalDateTime.now());
    }

    public void recordFailedLogin(User user) {
        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(attempts);
        user.setLastFailedLogin(LocalDateTime.now());

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        }
    }

    public void recordSuccessfulLogin(User user) {
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        user.setAccountLockedUntil(null);
    }

    public void setPasswordExpiry(User user) {
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(PASSWORD_EXPIRY_DAYS));
        user.setPasswordChangedAt(LocalDateTime.now());
    }
}
