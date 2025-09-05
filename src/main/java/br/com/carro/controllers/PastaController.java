package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.PastaCreateDTO;
import br.com.carro.entities.DTO.PastaDTO;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.PastaService;
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

    @Autowired
    private JpaUserDetailsService userDetailsService;

    // DTO para receber os dados da nova pasta
    public record NovaPastaDto(String nomePasta, Long pastaPaiId) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> criarPasta(@RequestBody @Valid PastaRequestDTO pastaDTO, Authentication authentication) {
        try {
            // Obter o Principal (Jwt) e o nome do usuário (subject)
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());

            // Chamar o service com o DTO e o usuário logado
            Pasta novaPasta = pastaService.criarPasta(pastaDTO, usuarioLogado);

            return ResponseEntity.status(HttpStatus.CREATED).body(PastaDTO.fromEntity(novaPasta));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            logger.warn("Erro ao criar pasta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AccessDeniedException e) {
            logger.warn("Acesso negado ao criar pasta");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao criar a pasta.");
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PastaDTO>> listarPastasRaiz(@AuthenticationPrincipal Usuario usuarioLogado) {
        List<Pasta> pastasRaiz = pastaService.listarPastasRaiz(usuarioLogado);
        List<PastaDTO> pastasDTO = pastasRaiz.stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pastasDTO);
    }


}