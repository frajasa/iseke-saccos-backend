package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelinquencyReportDTO {
    private LocalDate date;
    private List<DelinquencyRangeDTO> ranges;
    private BigDecimal totalOutstanding;
    private BigDecimal totalAtRisk;
}