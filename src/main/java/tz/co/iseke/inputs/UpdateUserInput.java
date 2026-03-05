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
public class UpdateUserInput {
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;
    private UUID branchId;
    private String updatedBy;
}