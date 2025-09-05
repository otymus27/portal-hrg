package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.*;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import br.com.carro.services.PastaService;
import br.com.carro.utils.AuthService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.nio.file.AccessDeniedException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pastas")
public class PastaController {
    private static final Logger logger = LoggerFactory.getLogger(PastaController.class);


    @Autowired
    private PastaService pastaService;
    private AuthService authService;
    private JpaUserDetailsService userDetailsService;

    // ✅ Use constructor injection
    public PastaController( AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> criarPasta(
            @RequestBody @Valid PastaRequestDTO pastaDTO,
            Authentication authentication
    ) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            Pasta novaPasta = pastaService.criarPasta(pastaDTO, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(PastaDTO.fromEntity(novaPasta));

        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            logger.warn("Erro ao criar pasta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            logger.error("Erro inesperado ao criar pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao criar a pasta.");
        }
    }

    @GetMapping("/raiz")
    public ResponseEntity<List<PastaDTO>> listarPastasRaiz(Authentication authentication) throws AccessDeniedException {
        Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
        List<Pasta> pastas = pastaService.listarPastasRaiz(usuarioLogado);
        return ResponseEntity.ok(pastas.stream().map(PastaDTO::fromEntity).toList());
    }

    @GetMapping("/arvore")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE','BASIC')")
    public ResponseEntity<?> getArvorePastas(
            Authentication authentication,
            @RequestBody(required = false) PastaFilterDTO filtro
    ) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            if (filtro == null) filtro = new PastaFilterDTO();
            List<PastaCompletaDTO> arvore = pastaService.getTodasPastasCompletas(usuarioLogado, filtro);
            return ResponseEntity.ok(arvore);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao buscar árvore de pastas.");
        }
    }

}