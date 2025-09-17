package br.com.carro.entities.DTO;

import java.util.Set;

public record UsuarioCreateDTO(
        String username,
        String password,
        String nome,
        Set<RoleIdDto> roles
) {
    public record RoleIdDto(Long id) {}
}
