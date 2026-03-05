package tz.co.iseke.inputs;

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
public class CreateSavingsAccountInput {
    private UUID memberId;
    private UUID productId;
    private UUID branchId;
    private BigDecimal openingDeposit;
    private String beneficiaryName;
    private String beneficiaryRelationship;
}