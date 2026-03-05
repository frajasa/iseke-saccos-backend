package tz.co.iseke.inputs;

import lombok.Data;

import java.util.UUID;

@Data
public class ReverseTransactionInput {
    private UUID transactionId;
    private String reason;
}
