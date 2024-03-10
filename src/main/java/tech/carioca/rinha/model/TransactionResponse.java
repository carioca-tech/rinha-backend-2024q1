package tech.carioca.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionResponse(
        @JsonProperty("limite") long limite,
        @JsonProperty("saldo") long saldo) {
}
