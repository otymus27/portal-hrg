package br.com.carro.entities.DTO;

import java.util.List;

/**
 * Record DTO para representar um item (pasta ou arquivo) com todas as
 * informações necessárias para o front-end, incluindo ID, tamanho,
 * contagem de itens e data de criação.
 */
public record ItemCompletoDTO(
        Long id,
        String nome,
        boolean isDiretorio,
        List<ItemDTO> filhos,
        Double tamanho,
        Integer contagem,
        String dataCriacao
) {}
