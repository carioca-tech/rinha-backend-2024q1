package tech.carioca.rinha.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record StatementTransaction(
        @JsonProperty("valor") int valor,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("descricao") String descricao,
        @JsonProperty("realizada_em") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        LocalDateTime realizada_em
) {
}
