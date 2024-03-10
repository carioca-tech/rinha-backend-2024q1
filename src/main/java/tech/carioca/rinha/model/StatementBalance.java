package tech.carioca.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record StatementBalance(
        @JsonProperty("total") long total,
        @JsonProperty("data_extrato") Instant instant,
        @JsonProperty("limite") long limite
) {
}
