package br.com.carro.autenticacao;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // ✅ Habilita o uso de @PreAuthorize e @PostAuthorize
public class SecurityConfigurations {

    // Aqui injetamos o valor de 'jwt.secret' do application.properties/yml
    @Value("${jwt.secret}")
    private String jwtSecret;

    private byte[] secretBytes; // armazenamos a chave já resolvida
    private SecretKey hmacKey;

    @PostConstruct
    public void initSecret() {
        this.secretBytes = resolveSecretBytes(jwtSecret);
        if (this.secretBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret deve ter pelo menos 32 bytes (256 bits) após o processamento. " +
                            "Use um segredo mais longo."
            );
        }
        this.hmacKey = new SecretKeySpec(this.secretBytes, "HmacSHA256");

        // ✅ Diagnóstico opcional: loga a chave para verificar a consistência
        System.out.println("--- Diagnóstico JWT ---");
        System.out.println("jwt.secret (string) length: " + (jwtSecret == null ? 0 : jwtSecret.length()));
        System.out.println("secretBytes length: " + this.secretBytes.length + " bytes");
        // Convertendo para String Base64 para visualização (NÃO FAÇA EM PROD SE FOR MUITO SENSIBIL)
        System.out.println("SecretKey (Base64 encoded for debug): " + Base64.getEncoder().encodeToString(this.secretBytes));
        System.out.println("--- Fim Diagnóstico JWT ---");
    }

    private byte[] resolveSecretBytes(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("jwt.secret não definido");
        }

        // 1) tenta Base64-URL
        try {
            return Base64.getUrlDecoder().decode(raw);
        } catch (IllegalArgumentException ignored) {}

        // 2) tenta Base64 padrão
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ignored) {}

        // 3) usa como texto puro (UTF-8)
        return raw.getBytes(StandardCharsets.UTF_8);
    }

     @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Usado para só desenvolvimento
//        configuration.addAllowedOrigin("http://localhost:4200");  // ou http://localhost
//        configuration.addAllowedOrigin("http://localhost:8082");  // ou http://localhost
//        configuration.addAllowedOrigin("http://localhost");
//        configuration.addAllowedOrigin("null"); // Para acesso via arquivo local (file://)


         // ✅ CORREÇÃO: Liste explicitamente as origens permitidas depois tem que apagar
         // Inclui a sua aplicação local e "null" para o acesso via arquivo HTML
         configuration.setAllowedOrigins(Arrays.asList(
                 "http://localhost:4200", // Origem do seu frontend Angular
                 "http://localhost:8082", // Opcional, mas útil para testes
                 "http://localhost",
                 "http://localhost:86",
                 "http://10.85.190.202:86",
                 "null" // Para acesso via arquivo local (file://)
         ));


         // Usado para produção
         configuration.addAllowedOrigin("http://localhost:86");  // ou http://localhost:80
         configuration.addAllowedOrigin("http://10.85.190.202:86"); // IP da sua máquina servidor
         configuration.addAllowedMethod("*"); // GET, POST, etc.
         configuration.addAllowedHeader("*"); // Authorization, Content-Type...
         configuration.setAllowCredentials(true); // se usar cookies ou auth
         configuration.setMaxAge(3600L);

         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", configuration);

         return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desabilita o CSRF para API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.POST, "/login").permitAll() // ✅ Permite acesso público ao endpoint de login
                                // ✅ Permite POST para /api/login sem autenticação
                                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                                // ✅ Permite OPTIONS para qualquer endpoint
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                // ✅ Permite acesso a qualquer rota pública (incluindo as novas)
                                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()


                                .requestMatchers(HttpMethod.GET, "/api/privado/pastas/download").authenticated() // ✅ Novo: Protege especificamente o download




                                .anyRequest().authenticated()
                )

                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions
                                .sameOrigin() // Permite que a página seja exibida em um iframe somente se for do mesmo domínio
                        )
                )

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))) // Configura o servidor de recursos OAuth2 para usar JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // Garante que sessões não serão criadas

        return http.build();
    }



    // ✅ BEAN ATUALIZADO: Configura como as autoridades são extraídas do JWT para Spring Security 6.x+
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // 👉 Ajusta o prefixo do claim de roles (senão fica "SCOPE_" por padrão)
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);


        return jwtAuthenticationConverter;
    }


    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        // ✅ ADICIONADO: Log para verificar a chave sendo usada pelo encoder
        System.out.println("--- Diagnóstico JWT (JwtEncoder) ---");
        System.out.println("JwtEncoder usando SecretKey (Base64 encoded): " + Base64.getEncoder().encodeToString(this.hmacKey.getEncoded()));
        System.out.println("--- Fim Diagnóstico JWT (JwtEncoder) ---");
        // ✅ CORREÇÃO: Usa a 'hmacKey' já resolvida para garantir consistência
        return new NimbusJwtEncoder(new ImmutableSecret<>(this.hmacKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // ✅ ADICIONADO: Log para verificar a chave sendo usada pelo decoder
        System.out.println("--- Diagnóstico JWT (JwtDecoder) ---");
        System.out.println("JwtDecoder usando SecretKey (Base64 encoded): " + Base64.getEncoder().encodeToString(this.hmacKey.getEncoded()));
        System.out.println("--- Fim Diagnóstico JWT (JwtDecoder) ---");
        // Usa a mesma chave no decoder
        return NimbusJwtDecoder.withSecretKey(hmacKey).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
