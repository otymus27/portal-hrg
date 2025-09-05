package br.com.carro.entities.Senha;

public record ResetSenhaRequestDto(
        String username,
        String senhaProvisoria,
        String novaSenha
) {
}