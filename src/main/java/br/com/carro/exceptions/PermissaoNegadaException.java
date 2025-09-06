package br.com.carro.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção personalizada para quando um usuário não tem permissão para uma ação.
 * Mapeia diretamente para um status HTTP 403 (FORBIDDEN).
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissaoNegadaException extends RuntimeException {

    public PermissaoNegadaException(String message) {
        super(message);
    }
}