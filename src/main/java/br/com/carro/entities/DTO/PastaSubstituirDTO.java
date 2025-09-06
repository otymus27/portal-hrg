package br.com.carro.entities.DTO;

public record PastaSubstituirDTO(
        Long idPastaDestino,
        Long idPastaOrigem,
        boolean copiar // se true copia, se false move
) {
}
