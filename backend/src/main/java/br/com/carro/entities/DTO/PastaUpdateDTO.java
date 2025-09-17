package br.com.carro.entities.DTO;

import java.util.Set;

// Para atualizar pasta
public record PastaUpdateDTO(
        String nome,
        Set<Long> usuariosComPermissaoIds
) {}