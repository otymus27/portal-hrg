package br.com.carro.entities.DTO;

import jakarta.validation.constraints.NotNull;

public record PermissaoDTO(
        @NotNull(message = "O ID do usuário não pode ser nulo.")
        Long usuarioId,

        @NotNull(message = "O ID da pasta não pode ser nulo.")
        Long pastaId
) {
}

