package br.com.carro.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Component
@ControllerAdvice
public class ApiExceptionHandler implements AuthenticationEntryPoint {

    // Tratamento para violação de integridade de dados (ex: valor duplicado)
    @ResponseBody
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Violação de integridade");

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
            error.put("campo", campo);
            error.put("mensagem", "Valor duplicado para o campo '" + campo + "'.");
        } else {
            error.put("mensagem", "Operação não permitida: o recurso está em uso ou viola uma restrição.");
        }
        return error;
    }

    // Handler para erros de recursos não encontrados (404)
    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Recurso não encontrado");
        error.put("mensagem", ex.getMessage());
        return error;
    }

    // Handler para erros de dados inválidos (400)
    @ResponseBody
    @ExceptionHandler(DadosInvalidosException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleDadosInvalidos(DadosInvalidosException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Dados inválidos");
        error.put("mensagem", ex.getMessage());
        return error;
    }

    // Handler para erros de validação de DTOs (anotações @Valid)
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    // Handler para falha na autenticação (usuário/senha incorretos)
    @ResponseBody
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentials(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Credenciais inválidas");
        error.put("mensagem", "Usuário ou senha incorretos.");
        return error;
    }

    // Handler para falha na autorização (usuário não tem permissão)
    @ResponseBody
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Acesso negado");
        error.put("mensagem", "Você não tem permissão para acessar este recurso.");
        return error;
    }

    // Handler para Endpoint não encontrado (404)
    @ResponseBody
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Endpoint não encontrado");
        error.put("mensagem", "O caminho '" + ex.getRequestURL() + "' não existe.");
        error.put("path", ex.getRequestURL());
        return error;
    }

    // Handler para tipo de argumento de URL inválido (ex: ID que não é um número)
    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Tipo de argumento inválido");
        error.put("mensagem", "O ID fornecido '" + ex.getValue() + "' não é um formato válido. Espera-se um número.");
        return error;
    }

    // Handler genérico para capturar qualquer outra exceção não tratada
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("erro", "Erro interno no servidor");
        error.put("mensagem", "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.");
        ex.printStackTrace(); // Boa prática para debugar em ambiente de desenvolvimento
        return error;
    }

    // Método para tratar quando o usuário não está autenticado
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                "erro", "Não autorizado",
                "mensagem", "Você precisa estar autenticado para acessar este recurso.",
                "path", request.getRequestURI()
        ));
    }
}
