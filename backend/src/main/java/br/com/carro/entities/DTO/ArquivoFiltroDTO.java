package br.com.carro.entities.DTO;

// Para filtros de busca e paginação
public record ArquivoFiltroDTO(
        String nome,
        String extensao,
        Long pastaId,
        String ordenarPor, // "nome", "data", "tamanho"
        String ordem, // "asc" ou "desc"
        int pagina,
        int tamanhoPagina
) {}
