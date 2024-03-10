package tech.carioca.rinha;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import tech.carioca.rinha.model.*;
import tech.carioca.rinha.persistence.ClienteRepositoryJdbc;
import tech.carioca.rinha.persistence.CustomerEntity;

import java.time.Instant;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding(value = {TransactionRequest.class, TransactionResponse.class, Statement.class, StatementBalance.class, StatementTransaction.class})
public class ClienteHandler {

    private final ClienteRepositoryJdbc clienteRepository;

    public ClienteHandler(ClienteRepositoryJdbc pessoaRepository, ConnectionFactory connectionFactory) {
        this.clienteRepository = pessoaRepository;
    }

    private static TransactionRequest validateRequestOrThrow(TransactionRequest input) {
        if (input.descricao() == null || input.descricao().length() > 10 || input.descricao().length() < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (input.valor() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (input.valor().signum() != 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (input.valor().scale() != 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (input.tipo() == null || input.tipo().length() != 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        var tipo = input.tipo().charAt(0);
        if (tipo != 'c' && tipo != 'd') {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return input;
    }

    public Mono<ServerResponse> handlePost(ServerRequest request) {
        var clienteId = Long.parseLong(request.pathVariable("cliente-id"));
        return request
                .bodyToMono(TransactionRequest.class)
                .map(ClienteHandler::validateRequestOrThrow)
                .flatMap(body -> handleStatementTransaction(body.tipo().charAt(0), clienteId, body))
                .onErrorMap(DataIntegrityViolationException.class, it -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY))
                .map(cliente -> new TransactionResponse(cliente.limite(), cliente.saldo()))
                .flatMap(cliente -> ServerResponse.status(HttpStatus.OK).bodyValue(cliente))
                .switchIfEmpty(ServerResponse.notFound().build());

    }

    public Mono<ServerResponse> handleGet(ServerRequest request) {
        var clienteId = Long.parseLong(request.pathVariable("cliente-id"));
        return clienteRepository.recuperarExtrato(clienteId,ClienteHandler::mapToStatement)
                .flatMap(cliente -> ServerResponse.status(HttpStatus.OK).bodyValue(cliente))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<CustomerEntity> handleStatementTransaction(char tipo, long clienteId, TransactionRequest transactionRequest) {
        if ('c' == tipo) {
            return clienteRepository.credit(clienteId, transactionRequest.valor().longValue(), transactionRequest.descricao());
        }
        return clienteRepository.debit(clienteId, transactionRequest.valor().longValue(), transactionRequest.descricao())
                .onErrorMap(DataIntegrityViolationException.class, it -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY));

    }


    private static Statement mapToStatement(CustomerEntity c, List<StatementTransaction> t) {
        return new Statement(new StatementBalance(c.saldo(), Instant.now(), c.limite()), t);
    }
}
