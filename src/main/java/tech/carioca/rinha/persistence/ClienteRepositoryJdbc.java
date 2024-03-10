package tech.carioca.rinha.persistence;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tech.carioca.rinha.model.StatementTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class ClienteRepositoryJdbc {

    public static final BiFunction<Row, RowMetadata, CustomerEntity> CUSTOMER_MAPPER = (row, rowMetaData) ->
            new CustomerEntity(
                    row.get("id", Integer.class),
                    row.get("limite", Integer.class),
                    row.get("saldo", Integer.class)
            );
    private static final BiFunction<Row, RowMetadata, StatementTransaction> TRANSACTION_MAPPER = (row, rowMetaData) -> new StatementTransaction(
            row.get("valor", Integer.class),
            row.get("operacao", String.class),
            row.get("descricao", String.class),
            row.get("criacao", LocalDateTime.class)
    );
    private final DatabaseClient databaseClient;
    private final ReactiveTransactionManager transactionManager;

    public ClienteRepositoryJdbc(DatabaseClient databaseClient, ReactiveTransactionManager transactionManager) {
        this.databaseClient = databaseClient;
        this.transactionManager = transactionManager;
    }

    public Mono<CustomerEntity> credit(long id, long valor, String description) {
        var transactionalOperator = TransactionalOperator.create(transactionManager);
        var transactionEntry = createTransactionEntry(id, 'c', valor, description);
        return addBalance(id, valor)
                .flatMap(transactionEntry::thenReturn)
                .as(transactionalOperator::transactional);
    }

    public Mono<CustomerEntity> debit(long id, long valor, String description) {
        var transactionalOperator = TransactionalOperator.create(transactionManager);
        var transactionEntry = createTransactionEntry(id, 'd', valor, description);
        return addBalance(id, -valor)
                .flatMap(transactionEntry::thenReturn)
                .as(transactionalOperator::transactional);
    }

    private Mono<CustomerEntity> addBalance(long id, long valor) {
        return this.databaseClient
                .sql("update cliente set saldo = saldo + :valor  where id=:id returning id, limite, saldo")
                .bind("id", id)
                .bind("valor", valor)
                .map(CUSTOMER_MAPPER)
                .one();
    }

    private Mono<Long> createTransactionEntry(long id, char operacao, long valor, String description) {
        return this.databaseClient
                .sql("insert into transacao(cliente_id, operacao, valor, descricao) values (:cliente_id, :operacao, :valor, :descricao)")
                .bind("cliente_id", id)
                .bind("operacao", operacao)
                .bind("valor", valor)
                .bind("descricao", description)
                .fetch()
                .rowsUpdated();
    }

    public <T> Mono<T>  recuperarExtrato(long id, BiFunction<CustomerEntity, List<StatementTransaction>,T> mapper) {

        var cliente = this.databaseClient
                .sql("select id,limite,saldo from cliente where id=:id")
                .bind("id", id)
                .map(CUSTOMER_MAPPER)
                .one();

        var transactions = this.databaseClient
                .sql("select valor, operacao, descricao, criacao from transacao where cliente_id=:cliente_id order by criacao desc limit 10")
                .bind("cliente_id", id)
                .map(TRANSACTION_MAPPER)
                .all().collectList();

        return cliente.zipWith(transactions, mapper);
    }




}
