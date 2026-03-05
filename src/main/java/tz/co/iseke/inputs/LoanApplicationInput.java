package tz.co.iseke.inputs;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationInput {
    @NotNull
    private UUID memberId;
    @NotNull
    private UUID productId;
    private UUID branchId;
    @NotNull
    @DecimalMin(value = "0.01", message = "Requested amount must be positive")
    private BigDecimal requestedAmount;
    @NotNull
    @Min(value = 1, message = "Term must be at least 1 month")
    private Integer termMonths;
    private String purpose;
    private String loanOfficer;
}