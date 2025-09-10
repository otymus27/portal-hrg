package br.com.carro.entities.DTO;

import br.com.carro.entities.Pasta;

import java.time.LocalDateTime;
import java.util.List;

public record PastaCompletaDTO(
        Long id,
        String nomePasta,
        String caminhoCompleto,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        List<ArquivoDTO> arquivos,
        List<PastaCompletaDTO> subPastas
) {
    public static PastaCompletaDTO fromEntity(Pasta pasta) {
        List<ArquivoDTO> arquivosDTO = pasta.getArquivos() != null
                ? pasta.getArquivos().stream().map(ArquivoDTO::fromEntity).toList()
                : List.of();

        // Converte subPastas recursivamente
        List<PastaCompletaDTO> subPastasDTO = pasta.getSubPastas() != null
                ? pasta.getSubPastas().stream()
                .map(PastaCompletaDTO::fromEntity)
                .toList()
                : List.of();

        // Inicialmente subPastas vazio, será preenchido pelo service recursivamente
        return new PastaCompletaDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                pasta.getDataCriacao(),
                pasta.getDataAtualizacao(),
                arquivosDTO,
                List.of()
        );
    }

    public PastaCompletaDTO withSubPastas(List<PastaCompletaDTO> subPastas) {
        return new PastaCompletaDTO(
                this.id,
                this.nomePasta,
                this.caminhoCompleto,
                this.dataCriacao,
                this.dataAtualizacao,
                this.arquivos,
                subPastas
        );
    }
}
