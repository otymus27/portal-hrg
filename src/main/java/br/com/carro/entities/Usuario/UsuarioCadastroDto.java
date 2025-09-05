package br.com.carro.entities.Usuario;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
public record UsuarioCadastroDto(
        @NotBlank
        String username,
        @NotBlank
        String password,
        Set<Long> roleIds
) {
}
