package br.com.carro.entities.DTO;

import br.com.carro.entities.Usuario.Usuario;


//Traz a lista de usuarios por pasta
public record UsuarioResumoDTO(
        Long id,
        String username

) {
    public static UsuarioResumoDTO fromEntity(Usuario usuario) {
        return new UsuarioResumoDTO(
                usuario.getId(),
                usuario.getUsername()
        );
    }
}
