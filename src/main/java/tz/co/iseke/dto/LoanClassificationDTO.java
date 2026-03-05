package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanClassificationDTO {
    private String classification;
    private Integer count;
    private BigDecimal outstandingAmount;
    private BigDecimal provisionRate;
    private BigDecimal provisionAmount;
}
