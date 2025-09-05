package br.com.carro.autenticacao;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class CustomTokenClaims implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        // Obtenha as permissões do usuário autenticado
        // Coleta as authorities como uma lista de Strings (ex: "ROLE_ADMIN", "ROLE_USER")
        // Não as junta em uma única string, para que sejam uma lista JSON
        Collection<String> authorities = context.getPrincipal().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // ✅ CORREÇÃO: Adicione as permissões como uma claim "roles" (no plural) no token
        // Isso alinha com o `setAuthoritiesClaimName("roles")` no `SecurityConfigurations`
        context.getClaims().claim("roles", authorities);
    }
}