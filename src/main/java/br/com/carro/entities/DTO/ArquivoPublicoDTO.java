package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import java.time.LocalDateTime;

public record ArquivoPublicoDTO(
        Long id,
        String nome,
        String tipo,
        Long tamanho,
        LocalDateTime dataUpload,
        LocalDateTime dataAtualizacao
) {
    public static ArquivoPublicoDTO fromEntity(Arquivo arquivo) {
        return new ArquivoPublicoDTO(
                arquivo.getId(),
                arquivo.getNomeArquivo(),
                arquivo.getTipoMime(),
                arquivo.getTamanho(),
                arquivo.getDataUpload(),
                arquivo.getDataAtualizacao()
        );
    }
}

