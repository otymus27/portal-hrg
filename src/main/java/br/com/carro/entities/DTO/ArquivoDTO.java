package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import java.time.LocalDateTime;

public record ArquivoDTO(
        Long id,
        String nome,
        String tipo,
        Long tamanho,
        LocalDateTime dataUpload,
        LocalDateTime dataAtualizacao,
        String criadoPor
) {
    public static ArquivoDTO fromEntity(Arquivo arquivo) {
        return new ArquivoDTO(
                arquivo.getId(),
                arquivo.getNomeArquivo(),
                arquivo.getTipoMime(),
                arquivo.getTamanho(),
                arquivo.getDataUpload(),
                arquivo.getDataAtualizacao(),
                arquivo.getCriadoPor().getUsername()
        );
    }
}
