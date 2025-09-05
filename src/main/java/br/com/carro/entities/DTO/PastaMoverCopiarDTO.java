package br.com.carro.entities.DTO;

import java.util.List;

// Para mover/copiar/substituir pastas
public record PastaMoverCopiarDTO(
        List<Long> idsPastas,
        Long pastaDestinoId,
        boolean copiar,
        boolean substituir // se true, substitui a pasta de destino
) {}
