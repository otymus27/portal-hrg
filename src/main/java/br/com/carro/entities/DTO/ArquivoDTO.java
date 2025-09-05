package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import java.time.LocalDateTime;

// Para listagem de arquivos na área pública ou administrativa
public record ArquivoDTO(
        Long id,
        String nome,
        String tipo,
        Long tamanho,
        LocalDateTime dataUpload,
        LocalDateTime dataAlteracao,
        String caminho,      // caminho relativo para exibição/download
        Long pastaId         // id da pasta a que pertence
) {}