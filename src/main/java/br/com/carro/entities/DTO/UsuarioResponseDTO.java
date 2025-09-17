package br.com.carro.entities.DTO;

import java.util.Set;
import java.util.stream.Collectors;

public record UsuarioResponseDTO(
        Long id,
        String username,
        String nome,
        Set<RoleDto> roles
) {
    public record RoleDto(Long id, String nome) {}

    // Método auxiliar para converter entidade Usuario → DTO
    public static UsuarioResponseDTO fromEntity(br.com.carro.entities.Usuario.Usuario usuario) {
        Set<RoleDto> rolesDto = usuario.getRoles()
                .stream()
                .map(role -> new RoleDto(role.getId(), role.getNome()))
                .collect(Collectors.toSet());

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNome(),
                rolesDto
        );
    }
}
