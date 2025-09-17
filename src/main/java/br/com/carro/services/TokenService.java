package br.com.carro.services;

import br.com.carro.entities.LoginAudit;
import br.com.carro.repositories.LoginAuditRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final LoginAuditRepository loginAuditRepository;

    public TokenService(JwtEncoder jwtEncoder, LoginAuditRepository loginAuditRepository) {
        this.jwtEncoder = jwtEncoder;
        this.loginAuditRepository = loginAuditRepository;
    }

    public String gerarToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L; // 10 horas, como no seu log

        // ✅ Isso coleta as autoridades EXATAMENTE como elas são definidas no UserDetailsService
        // Se seu UserDetailsService define "ROLE_ADMIN", aqui será coletado "ROLE_ADMIN".
        var scopes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("carro-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("roles", scopes) // A claim "roles" terá, por exemplo, ["ROLE_ADMIN"]
                .build();


        // >>> Header COM algoritmo HS256 <<<
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        // ✅ Registrar login no banco
        loginAuditRepository.save(new LoginAudit(authentication.getName()));

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}