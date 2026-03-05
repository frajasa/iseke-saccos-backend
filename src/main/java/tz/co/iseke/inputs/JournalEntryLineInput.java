package tz.co.iseke.inputs;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class JournalEntryLineInput {
    private UUID accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
}
