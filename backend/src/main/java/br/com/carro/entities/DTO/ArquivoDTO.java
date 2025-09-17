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
        String criadoPor                  // ✅ Nome do criador ou valor padrão
) {
    public static ArquivoDTO fromEntity(Arquivo arquivo) {
        // ✅ Evita NullPointerException caso criadoPor seja nulo
        String criadorNome = (arquivo.getCriadoPor() != null && arquivo.getCriadoPor().getUsername() != null)
                ? arquivo.getCriadoPor().getUsername()
                : "Sistema";

        return new ArquivoDTO(
                arquivo.getId(),
                arquivo.getNomeArquivo(),
                arquivo.getTipoMime(),
                arquivo.getTamanho(),
                arquivo.getDataUpload(),
                arquivo.getDataAtualizacao(),
                criadorNome
        );
    }
}
