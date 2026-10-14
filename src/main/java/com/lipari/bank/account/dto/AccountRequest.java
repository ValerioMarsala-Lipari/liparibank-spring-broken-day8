package com.lipari.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "L'IBAN non può essere vuoto")
        @Size(min = 15, max = 34, message = "L'IBAN deve avere tra 15 e 34 caratteri")
        String iban,

        @NotNull(message = "Il saldo iniziale è obbligatorio")
        @Positive(message = "Il saldo iniziale deve essere positivo")
        BigDecimal balance,

        @NotBlank(message = "Il nome del titolare non può essere vuoto")
        String ownerName
) {}
