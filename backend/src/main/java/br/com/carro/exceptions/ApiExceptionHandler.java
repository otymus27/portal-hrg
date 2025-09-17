package br.com.carro.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@ControllerAdvice
public class ApiExceptionHandler implements AuthenticationEntryPoint {

    @ResponseBody
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessage handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String mensagem = "Operação não permitida: o recurso está em uso ou viola uma restrição.";
        String message = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        if (message != null && message.toLowerCase().contains("duplicate entry")) {
            String campo = "campo";
            try {
                int keyIndex = message.toLowerCase().indexOf("for key");
                if (keyIndex != -1) {
                    String key = message.substring(keyIndex).replace("for key", "").replace("'", "").trim();
                    if (key.contains(".")) {
                        campo = key.split("\\.")[1];
                    } else {
                        campo = key;
                    }
                }
            } catch (Exception e) {
                campo = "desconhecido";
            }
            mensagem = "Valor duplicado para o campo '" + campo + "'.";
        }

        return new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                "Violação de integridade",
                mensagem,
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(DadosInvalidosException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleDadosInvalidos(DadosInvalidosException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Validação falhou",
                erros,
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessage handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                "Credenciais inválidas",
                "Usuário ou senha incorretos.",
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorMessage handleAuthorizationDenied(AuthorizationDeniedException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                "Você não tem permissão para acessar este recurso.",
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                "Endpoint não encontrado",
                "O caminho '" + ex.getRequestURL() + "' não existe.",
                ex.getRequestURL()
        );
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Tipo de argumento inválido",
                "O valor '" + ex.getValue() + "' não é um formato válido. Espera-se um número.",
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorMessage handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Arquivo muito grande",
                "O tamanho máximo permitido para upload é 20MB.",
                request.getRequestURI()
        );
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessage handleGenericException(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();
        return new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno no servidor",
                "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.",
                request.getRequestURI()
        );
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        new ObjectMapper().writeValue(response.getOutputStream(),
                new ErrorMessage(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Não autorizado",
                        "Você precisa estar autenticado para acessar este recurso.",
                        request.getRequestURI()
                ));
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

}
