package br.com.carro.entities.DTO;

import java.util.Set;

public record PastaPermissaoAcaoDTO(
        Long pastaId,
        Set<Long> adicionarUsuariosIds,
        Set<Long> removerUsuariosIds
) {}
