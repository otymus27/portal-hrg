package br.com.carro.entities.DTO;

import java.util.Set;

public record UsuarioCreateDTO(
        String username,
        String password,
        Set<RoleIdDto> roles
) {
    public record RoleIdDto(Long id) {}
}
