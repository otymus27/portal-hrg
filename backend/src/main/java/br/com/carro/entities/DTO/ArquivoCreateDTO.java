package br.com.carro.entities.DTO;

// Para criação ou upload de arquivo
public record ArquivoCreateDTO(
        String nome,
        String tipo,
        byte[] conteudo,    // opcional se o upload for multipart no controller
        Long pastaId
) {}
