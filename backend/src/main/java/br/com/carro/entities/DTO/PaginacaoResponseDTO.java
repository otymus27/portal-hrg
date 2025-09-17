package br.com.carro.entities.DTO;

import java.util.List;

// Resposta paginada
public record PaginacaoResponseDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int totalPaginas,
        long totalElementos,
        int totalPages) {}
