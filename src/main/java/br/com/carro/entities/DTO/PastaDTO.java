package br.com.carro.entities.DTO;

import br.com.carro.entities.Pasta;

import java.time.LocalDateTime;

public record PastaDTO(
        Long id,
        String nome,
        String caminhoCompleto,
        LocalDateTime dataCriacao,
        String nomeUsuarioCriador
) {
    public static PastaDTO fromEntity(Pasta pasta) {
        return new PastaDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                pasta.getDataCriacao(),
                pasta.getCriadoPor().getUsername()
        );
    }
}