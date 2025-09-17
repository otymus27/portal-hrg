package br.com.carro.utils;

import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;


/**
✅ Centraliza a lógica de pegar o usuário logado
✅ Funciona em qualquer Service sem duplicar código
✅ Usa o Jwt do SecurityContextHolder
✅ Fácil de manter se no futuro mudar o claim do token (só altera no AuthService)

 o AuthService - para centralizar toda a lógica de autenticação e facilitar sua vida no restante da aplicação.
 Assim você terá métodos prontos para recuperar usuário logado, roles, username e até verificar se ele é admin.
**/


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Retorna o usuário logado, consultando no banco de dados.
     */
    /**
     * Obtém o usuário logado a partir do Authentication.
     */
    public Usuario getUsuarioLogado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof Jwt jwt) {
            username = jwt.getSubject();
        } else {
            username = principal.toString(); // fallback
        }

        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));
    }


    /**
     * Retorna o username do usuário logado (sem precisar buscar no banco).
     */
    public String getUsername(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return jwt.getSubject();
    }

    /**
     * Retorna as roles do usuário logado diretamente do token.
     */
    public List<String> getRoles(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Collections.emptyList();
        }
        return jwt.getClaimAsStringList("authorities");
    }

    /**
     * Retorna true se o usuário logado for ADMIN.
     */
    public boolean isAdmin(Authentication authentication) {
        return getRoles(authentication).contains("ROLE_ADMIN");
    }
}