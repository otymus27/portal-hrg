package br.com.carro.entities.DTO;

import java.util.List;

// Para requisição de download de múltiplos arquivos/pastas em ZIP
public record DownloadRequestDTO(
        List<Long> idsArquivos,
        List<Long> idsPastas
) {}
