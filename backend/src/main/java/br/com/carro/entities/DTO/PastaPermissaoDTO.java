package br.com.carro.entities.DTO;

import java.util.Set;

public record PastaPermissaoDTO(
        Long pastaId,
        Set<Long> usuariosIds
) {}