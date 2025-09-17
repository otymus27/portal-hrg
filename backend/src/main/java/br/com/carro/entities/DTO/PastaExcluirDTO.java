package br.com.carro.entities.DTO;

import java.util.List;

public record PastaExcluirDTO(
        List<Long> idsPastas,
        boolean excluirConteudo
) {
}
