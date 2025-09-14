package br.com.carro.entities.DTO;

import java.util.Set;

/**
 * DTO para representar os dados de um usuário de forma segura.
 * Usado para resposta em endpoints, evitando expor a senha.
 * Usado para retornar os dados do usuário logado no endpoint de perfil:
 */
public record UsuarioLogadoDTO(
        Long id,
        String username,
        Set<Long> pastasPrincipaisAcessadasIds,
        Set<RoleDto> roles
) {
    public record RoleDto(Long id, String nome) {}
}