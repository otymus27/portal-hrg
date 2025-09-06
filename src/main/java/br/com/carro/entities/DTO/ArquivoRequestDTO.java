package br.com.carro.entities.DTO;

import jakarta.validation.constraints.NotNull;

public record ArquivoRequestDTO(
        @NotNull Long pastaId
) {}
