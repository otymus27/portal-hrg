package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

// Para listagem de pastas
public record PastaDTO(
        Long id,
        String nome,
        Long pastaPaiId,
        LocalDateTime dataCriacao,
        LocalDateTime dataAlteracao,
        Long donoId // usuário dono da pasta pai
) {}
