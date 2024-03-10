package tech.carioca.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Statement(

        @JsonProperty("saldo") StatementBalance saldo,

        @JsonProperty("ultimas_transacoes") List<StatementTransaction> ultimasTransacoes
) {


}
