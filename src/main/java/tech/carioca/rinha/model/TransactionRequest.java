package tech.carioca.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record TransactionRequest(
        @JsonProperty("valor") BigDecimal valor,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("descricao") String descricao) {
}
