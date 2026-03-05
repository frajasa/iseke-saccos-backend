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
public class AddGuarantorInput {
    private UUID loanId;
    private UUID guarantorMemberId;
    private String guarantorName;
    private String guarantorNationalId;
    private String guarantorPhone;
    private BigDecimal guaranteedAmount;
    private String relationship;
}