package br.com.carro.entities.DTO;

// dto para usuario logar alterar propria senha
public record AlterarSenhaRequestDTO(
        String senhaAtual,
        String novaSenha
) {}
