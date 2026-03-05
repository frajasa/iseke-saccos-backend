package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.UserRole;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserInput {
        private String firstName;
        private String middleName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String password;
        private String confirmPassword;
        private UserRole role;
        private UUID branchId;
}