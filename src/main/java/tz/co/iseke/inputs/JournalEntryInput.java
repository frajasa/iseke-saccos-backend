package tz.co.iseke.inputs;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class JournalEntryInput {
    private LocalDate postingDate;
    private String description;
    private String reference;
    private UUID branchId;
    private List<JournalEntryLineInput> lines;
}
