package br.com.carro.controllers;
import br.com.carro.entities.Login.LoginRequest;
import br.com.carro.entities.Login.LoginResponse;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.UsuarioRepository;
import br.com.carro.services.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// ✅ Adicione @CrossOrigin à classe ou ao método
// Isso sobrescreve a configuração global para este controlador, ou age como um fallback.
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository; // ✅ Injete o repositório

    public AuthenticationController(AuthenticationManager authenticationManager, TokenService tokenService,UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {

        // ✅ Coloque um breakpoint aqui
        System.out.println("Tentativa de login para usuário: " + loginRequest.username());
        // ... Sua lógica de autenticação
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());

        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);

        String token = tokenService.gerarToken(authentication);

        // 1. Obtém o nome de usuário autenticado
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Busca o usuário do banco de dados para obter o campo senhaProvisoria
        // O orElseThrow lança uma exceção caso o usuário não seja encontrado (o que não deve acontecer após a autenticação)
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado após autenticação"));

        // 3. Constrói a resposta com a informação da senha provisória
        boolean isSenhaProvisoria = usuario.isSenhaProvisoria();

        // 4. Retorna o token, o tempo de expiração e a senha provisoria conforme pede a assinatura do LoginResponse
        return new LoginResponse(token, 36000L, isSenhaProvisoria);
    }
}
