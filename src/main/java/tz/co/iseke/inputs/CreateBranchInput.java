package tz.co.iseke.inputs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBranchInput {
    private String branchCode;
    private String branchName;
    private String address;
    private String phoneNumber;
    private String email;
    private String managerName;
    private LocalDate openingDate;
}