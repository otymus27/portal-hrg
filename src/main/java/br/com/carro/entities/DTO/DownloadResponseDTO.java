package br.com.carro.entities.DTO;

// Para resposta de download (nome do arquivo ZIP gerado)
public record DownloadResponseDTO(
        String nomeArquivo,
        byte[] conteudo
) {}
