package tz.co.iseke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResultDTO {
    private Boolean success;
    private String reference;
    private Integer entriesPosted;
    private LocalDate postingDate;
}
