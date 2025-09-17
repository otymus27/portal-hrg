package br.com.carro.services;

import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RecuperarSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RecuperarSenhaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gera uma senha provisória para o usuário.
     * Apenas para uso do administrador.
     *
     * @param id do usuário
     * @return senha provisória gerada
     */
    public String gerarSenhaProvisoria(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        String senhaProvisoria = gerarSenhaAleatoria();

        usuario.setPassword(passwordEncoder.encode(senhaProvisoria));
        usuario.setSenhaProvisoria(true);

        usuarioRepository.save(usuario);

        return senhaProvisoria;  // Retorna para o admin entregar ao usuário
    }

    /**
     * Atualiza a senha do usuário.
     * Se for senha provisória, permite redefinir.
     *
     * @param username do usuário
     * @param senhaAtual senha provisória ou atual
     * @param novaSenha nova senha a ser definida
     */
    public void atualizarSenha(String username, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuario.setSenhaProvisoria(false);  // Após redefinição, marca como definitiva

        usuarioRepository.save(usuario);
    }

    private String gerarSenhaAleatoria() {
        return UUID.randomUUID().toString().substring(0, 5);  // Melhor gerar 5 caracteres
    }



}
