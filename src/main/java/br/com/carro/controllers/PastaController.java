package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.*;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.PastaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/pasta")
public class PastaController {

    private static final Logger logger = LoggerFactory.getLogger(PastaController.class);
    public record Mensagem(String mensagem) {}

    @Autowired
    private final PastaService pastaService;
    private final JpaUserDetailsService userDetailsService;

    @Autowired
    public PastaController(PastaService pastaService, JpaUserDetailsService userDetailsService) {
        this.pastaService = pastaService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/top-level")
    public List<PastaPublicaDTO> getPastasTopLevel() {
        return pastaService.findPastasTopLevelDTO();
    }

    @GetMapping("/subpastas/{id}")
    public List<PastaPublicaDTO> getSubpastas(@PathVariable Long id) {
        return pastaService.findSubpastasByPastaPaiIdDTO(id);
    }

    @GetMapping("/principais")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Page<PastaDTO>> listarPastasPrincipaisDoUsuario(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (jwt == null || !jwt.hasClaim("sub")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());

        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        boolean isAdmin = usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"));

        return ResponseEntity.ok(pastaService.listarPastasPrincipais(isAdmin, usuarioLogado, pageable));
    }

    @GetMapping("/subpastas/{pastaPaiId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Page<PastaDTO>> listarSubpastas(
            @PathVariable Long pastaPaiId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(pastaService.listarSubpastas(pastaPaiId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASIC', 'GERENTE')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Pasta pasta = pastaService.buscarPorId(id);
            return new ResponseEntity<>(PastaDTO.fromEntity(pasta), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Cria uma nova pasta. Ela pode ser uma pasta raiz ou uma subpasta.
     * Acesso restrito a 'ADMIN'.
     */
    /**
     * ✅ MÉTODO PRINCIPAL: Cria uma nova pasta. Ela pode ser uma pasta raiz ou uma subpasta.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> criarPasta(@RequestBody PastaRequestDTO pastaDTO, @AuthenticationPrincipal Jwt jwt) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            Pasta novaPasta = this.pastaService.criarPasta(pastaDTO, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(PastaDTO.fromEntity(novaPasta));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Mensagem(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Mensagem(e.getMessage()));
        } catch (Exception e) {
            // Em caso de qualquer outro erro, retorne um erro genérico 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Mensagem("Erro ao criar a pasta."));
        }
    }

    /**
     * ✅ NOVO ENDPOINT: Faz o upload de uma estrutura de pastas e arquivos.
     */
    @PostMapping(value = "/upload-diretorio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> uploadDiretorio(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            pastaService.uploadDiretorioComArquivos(files, pastaId, usuarioLogado);
            return ResponseEntity.ok("Diretório e arquivos enviados com sucesso!");
        } catch (Exception e) {
            // Este log é fundamental para capturar o erro
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao processar o upload: " + e.getMessage());

        }
    }

    /**
     * Atualiza uma pasta existente.
     * Acesso restrito a 'ADMIN'.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PastaDTO> atualizar(@PathVariable Long id, @RequestBody Pasta pasta) {
        try {
            Pasta pastaAtualizada = this.pastaService.atualizar(id, pasta);
            return new ResponseEntity<>(PastaDTO.fromEntity(pastaAtualizada), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Exclui uma pasta por ID, incluindo suas subpastas.
     * Acesso restrito a 'ADMIN'.
     */
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        try {
            this.pastaService.excluir(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }



    /**
     * ✅ NOVO ENDPOINT: Exclui uma pasta por ID, incluindo todos os seus arquivos.
     * Exemplo: DELETE http://localhost:8082/api/pasta/excluir/1
     */
    @DeleteMapping("/excluir/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirPasta(@PathVariable("id") Long id) {
        try {
            pastaService.excluirPasta(id);
            return ResponseEntity.ok("Pasta e seu conteúdo excluídos com sucesso!");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir a pasta: " + e.getMessage());
        }
    }



    /**
     * Endpoint que busca pastas por nome com paginação e ordenação.
     * Exemplo de requisição: GET http://localhost:8082/api/pasta/buscar?nome=relatorio&page=0&size=10&sort=nomePasta,asc
     * @param nome O termo de busca.
     * @param pageable Objeto Pageable injetado automaticamente pelo Spring.
     * @return Uma página de PastaDTOs.
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Page<PastaDTO>> buscarPastasPorNome(
            @RequestParam("nome") String nome,
            @PageableDefault(page = 0, size = 10, sort = "dataCriacao", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PastaDTO> pastasEncontradas = pastaService.buscarPastasPorNome(nome, pageable);
        return ResponseEntity.ok(pastasEncontradas);
    }


    /**
     * ✅ NOVO ENDPOINT: Faz o download de uma pasta inteira em formato ZIP.
     */
    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Resource> downloadPasta(@PathVariable Long id) {
        Path tempZipFile = null;
        try {
            tempZipFile = pastaService.downloadPasta(id);
            Resource resource = new UrlResource(tempZipFile.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tempZipFile.getFileName().toString() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ NOVO ENDPOINT: Substitui o conteúdo de uma pasta existente.
     */
    @PutMapping(value = "/substituir/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> substituirPasta(
            @PathVariable("id") Long pastaId,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            System.out.println("Fui chamado substituir");
            testeDelecao();
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            pastaService.substituirPasta(pastaId, files, usuarioLogado);
            return ResponseEntity.ok("Conteúdo da pasta substituído com sucesso!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar a substituição: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ---- ENDPOINT PARA MOVER PASTA PARA OUTRO LOCAL

    @PutMapping("/mover/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')") // Ajuste a role conforme necessário
    public ResponseEntity<?> moverPasta(
            @PathVariable Long pastaId,
            @RequestBody PastaMoverCopiarDTO pastaMoverDTO,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            PastaDTO pastaAtualizada = pastaService.moverPasta(pastaId, pastaMoverDTO.pastaDestinoId(), usuarioLogado);
            return ResponseEntity.ok(pastaAtualizada);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao mover a pasta: " + e.getMessage());
        }
    }

    // ---- FIM ENDPOINT MOVER PASTA PARA OUTRO LOCAL

    // ---- ENDPOINT PARA COPIAR PASTA PARA OUTRO LOCAL

    @PostMapping("/copiar/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')") // Ajuste a role se necessário
    public ResponseEntity<?> copiarPasta(
            @PathVariable Long pastaId,
            @RequestBody PastaCopiarDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            PastaDTO pastaCopiada = pastaService.copiarPasta(pastaId, dto.pastaDestinoId(), usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(pastaCopiada);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao copiar a pasta: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno no servidor: " + e.getMessage());
        }
    }

    // --- FIM ENDPOINT COPIAR ARQUIVO PARA PASTA



    //Métodos para testes apagar depois
    /**
     * ✅ NOVO ENDPOINT: Roda o teste de deleção de arquivo.
     * Use para diagnosticar problemas de caminho.
     * GET http://localhost:8082/api/pasta/teste-delecao
     */
    @GetMapping("/teste-delecao")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> testeDelecao() {
        pastaService.testarCaminhoDelecao();
        return ResponseEntity.ok("Teste de deleção executado. Verifique o console.");
    }


}