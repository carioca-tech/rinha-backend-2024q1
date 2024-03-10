package tech.carioca.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@SpringBootApplication
@EnableR2dbcRepositories
public class RinhaApp {

    public static void main(String[] args) {
        SpringApplication.run(RinhaApp.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> route(ClienteHandler clienteHandler) {
        return RouterFunctions
                .route(POST("/clientes/{cliente-id}/transacoes").and(contentType(MediaType.APPLICATION_JSON)).and(accept(MediaType.APPLICATION_JSON)), clienteHandler::handlePost)
                .andRoute(GET("/clientes/{cliente-id}/extrato").and(accept(MediaType.APPLICATION_JSON)), clienteHandler::handleGet);
    }
}