package br.com.carro.entities.DTO;

import br.com.carro.entities.Pasta;
import br.com.carro.entities.Arquivo;
import java.time.LocalDateTime;

public record BuscaGlobalDTO(
        Long id,
        String tipo, // "PASTA" ou "ARQUIVO"
        String nome,
        String caminhoCompleto,
        String tipoArquivo,
        Long tamanho,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    public static BuscaGlobalDTO fromPasta(Pasta pasta) {
        return new BuscaGlobalDTO(
                pasta.getId(),
                "PASTA",
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                null,
                null,
                pasta.getDataCriacao(),
                pasta.getDataAtualizacao()
        );
    }

    public static BuscaGlobalDTO fromArquivo(Arquivo arquivo) {
        return new BuscaGlobalDTO(
                arquivo.getId(),
                "ARQUIVO",
                arquivo.getNomeArquivo(),
                null,
                arquivo.getTipoMime(),
                arquivo.getTamanho(),
                arquivo.getDataUpload(),
                arquivo.getDataAtualizacao()
        );
    }
}
