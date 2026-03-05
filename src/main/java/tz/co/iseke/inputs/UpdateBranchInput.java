package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.co.iseke.enums.BranchStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBranchInput {
    private String branchName;
    private String address;
    private String phoneNumber;
    private String email;
    private String managerName;
    private BranchStatus status;
}