package br.com.carro.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção personalizada para quando um arquivo não é encontrado.
 * Mapeia diretamente para um status HTTP 404 (NOT_FOUND).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArquivoNaoEncontradoException extends RuntimeException {

    public ArquivoNaoEncontradoException(String message) {
        super(message);
    }
}