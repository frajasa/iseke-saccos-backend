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
public class AddCollateralInput {
    private UUID loanId;
    private String collateralType;
    private String description;
    private BigDecimal estimatedValue;
    private String registrationNumber;
    private String location;
}