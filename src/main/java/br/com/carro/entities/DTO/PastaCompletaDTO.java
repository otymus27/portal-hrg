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
        String criadoPor,                  // ✅ Novo campo: nome do usuário que criou
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

        // ✅ Pega o nome do usuário criador, se existir
        String criadorNome = (pasta.getCriadoPor() != null)
                ? pasta.getCriadoPor().getUsername()
                : "Desconhecido";

        return new PastaCompletaDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                pasta.getDataCriacao(),
                pasta.getDataAtualizacao(),
                criadorNome,
                arquivosDTO,
                List.of() // Subpastas preenchidas depois no service
        );
    }

    public PastaCompletaDTO withSubPastas(List<PastaCompletaDTO> subPastas) {
        return new PastaCompletaDTO(
                this.id,
                this.nomePasta,
                this.caminhoCompleto,
                this.dataCriacao,
                this.dataAtualizacao,
                this.criadoPor,
                this.arquivos,
                subPastas
        );
    }
}
