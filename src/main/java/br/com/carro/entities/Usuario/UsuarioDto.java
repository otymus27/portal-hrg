package br.com.carro.entities.Usuario;
import br.com.carro.entities.Role.RoleDto;
import java.util.Set;

/**
 * DTO (Data Transfer Object) para representar os dados de um usuário.
 * Usado para expor dados do usuário na API, sem incluir informações sensíveis como a senha.
 */
public record UsuarioDto(
        Long id,
        String username,
        Set<Long> pastasPrincipaisAcessadasIds, // ✅ Novo campo
        Set<RoleDto> roles
) {}