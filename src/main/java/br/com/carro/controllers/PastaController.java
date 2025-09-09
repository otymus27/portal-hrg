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
import java.util.Map;
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

    //ENDPOINT 01 - Lista só as pastas principais ou raiz
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

// ENDPOINT 02- Método para busca de pastas e arquivos por id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<PastaCompletaDTO> getPastaPorId(
            @PathVariable Long id,
            Authentication authentication,
            @ModelAttribute PastaFilterDTO filtro) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            PastaCompletaDTO pasta = pastaService.getPastaCompletaPorId(id, usuarioLogado, filtro);
            return ResponseEntity.ok(pasta);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }


    /**

        Verifica permissão do usuário (ADMIN ou GERENTE com acesso).
        Exclui recursivamente todas as subpastas e arquivos.
        Remove do filesystem e do banco de dados.
        Retorna status apropriado (200 OK, 403 Forbidden, 404 Not Found ou 500 Internal Server Error).

    */


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> excluirPasta(@PathVariable Long id, Authentication authentication) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            pastaService.excluirPasta(id, usuarioLogado);
            return ResponseEntity.ok("Pasta excluída com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao excluir pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao excluir a pasta.");
        }
    }

/**   Respeita permissões (ADMIN ou usuários com acesso à pasta).
//    Atualiza filesystem e caminhos no banco.
//    Atualiza subpastas e arquivos recursivamente.
//    Garante que não haja nomes duplicados na mesma pasta.
*/
    @PatchMapping("/{id}/renomear")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> renomearPasta(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String novoNome = request.get("novoNome");
            if (novoNome == null || novoNome.isBlank()) {
                return ResponseEntity.badRequest().body("O novo nome da pasta é obrigatório.");
            }

            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);

            Pasta pastaAtualizada = pastaService.renomearPasta(id, novoNome, usuarioLogado);
            return ResponseEntity.ok(PastaDTO.fromEntity(pastaAtualizada));

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao renomear pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao renomear a pasta.");
        }
    }

/**    O método verifica permissões de ADMIN ou GERENTE.
//    Atualiza caminho da pasta e subpastas recursivamente.
//    Impede mover para local que já contenha uma pasta com mesmo nome.
//    Lança AccessDeniedException para acessos indevidos.
 **/
    @PatchMapping("/{id}/mover")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> moverPasta(@PathVariable Long id,
                                        @RequestParam(required = false) Long novaPastaPaiId,
                                        Authentication authentication) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            // Chama service para mover a pasta
            Pasta pastaMovida = pastaService.moverPasta(id, novaPastaPaiId, usuarioLogado);
            return ResponseEntity.ok(PastaDTO.fromEntity(pastaMovida));
        }  catch (IllegalArgumentException | EntityNotFoundException e) {
            logger.warn("Erro ao mover pasta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (AccessDeniedException e) {
            logger.warn("Acesso negado ao mover pasta");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao mover pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao mover a pasta.");
        }
    }

    @PostMapping("/{id}/copiar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> copiarPasta(
            @PathVariable Long id,
            @RequestParam(required = false) Long destinoPastaId,
            Authentication authentication
    ) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            var novaPasta = pastaService.copiarPasta(id, destinoPastaId, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(PastaDTO.fromEntity(novaPasta));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao copiar pasta", e);
            return ResponseEntity.internalServerError().body("Erro interno ao copiar a pasta.");
        }
    }


    @DeleteMapping("/excluir-lote")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> excluirPastasEmLote(@RequestBody PastaExcluirDTO pastaExcluirDTO,
                                                 Authentication authentication) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            pastaService.excluirPastasEmLote(pastaExcluirDTO.idsPastas(), pastaExcluirDTO.excluirConteudo(), usuarioLogado);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao excluir pastas em lote", e);
            return ResponseEntity.internalServerError().body("Erro interno ao excluir pastas.");
        }
    }


    /**
     * ✅ Substitui uma pasta existente (idDestino) por outra (idOrigem),
     * preservando a hierarquia e copiando todos os metadados.
     */
    @PutMapping("/{idDestino}/substituir/{idOrigem}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> substituirPasta(
            @PathVariable Long idDestino,
            @PathVariable Long idOrigem,
            Authentication authentication) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            Pasta pastaSubstituida = pastaService.substituirConteudoPasta(idOrigem, idDestino, usuarioLogado);
            return ResponseEntity.ok(PastaDTO.fromEntity(pastaSubstituida));

        } catch (EntityNotFoundException e) {
            logger.warn("Pasta não encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (AccessDeniedException e) {
            logger.warn("Acesso negado ao substituir pasta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (RuntimeException e) {
            logger.error("Erro ao substituir pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao substituir a pasta.");

        } catch (Exception e) {
            logger.error("Erro inesperado ao substituir pasta", e);
            return ResponseEntity.internalServerError().body("Erro inesperado.");
        }
    }


}