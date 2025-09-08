package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.exceptions.ArquivoNaoEncontradoException;
import br.com.carro.exceptions.PermissaoNegadaException;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.services.ArquivoService;
import br.com.carro.utils.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/arquivos")
public class ArquivoController {
    private static final Logger logger = LoggerFactory.getLogger(PastaController.class);

    private final ArquivoService arquivoService;
    private final ArquivoRepository arquivoRepository;
    private final PastaRepository pastaRepository;
    private AuthService authService;

    public ArquivoController(ArquivoService arquivoService, PastaRepository pastaRepository,ArquivoRepository arquivoRepository, AuthService authService) {
        this.arquivoService = arquivoService;
        this.pastaRepository = pastaRepository;
        this.arquivoRepository = arquivoRepository;
        this.authService = authService;
    }

    /**
     * RF-016: Upload de arquivo
     * @param file MultipartFile enviado
     * @param pastaId ID da pasta de destino
     * @param authentication Usuário autenticado
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadArquivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pastaId") Long pastaId,
            Authentication authentication
    ) {

        try{
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            Arquivo arquivo = arquivoService.uploadArquivo(file, pastaId, usuarioLogado, pastaRepository, arquivoRepository);
            return ResponseEntity.ok(arquivo);

        }catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            logger.warn("Erro ao fazer uploado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer upload do arquivo", e);
            return ResponseEntity.internalServerError().body("Erro ao fazer upload de arquivo.");
        }


    }

    /**
     * RF-018 – Excluir Arquivo
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> excluirArquivo(@PathVariable Long id,Authentication authentication) throws IOException {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            arquivoService.excluirArquivo(id, usuarioLogado);
            return ResponseEntity.ok("Arquivo excluído com sucesso.");
        }catch (
                EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao excluir pasta", e);
            return ResponseEntity.internalServerError().body("Erro ao excluir a pasta.");
        }
    }

    // RF-019: Mover arquivo
    @PutMapping("/{arquivoId}/mover/{pastaDestinoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> moverArquivo(
            @PathVariable Long arquivoId,
            @PathVariable Long pastaDestinoId,
            Authentication authentication
            ) {

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoMovido = arquivoService.moverArquivo(arquivoId, pastaDestinoId, usuarioLogado);
            return ResponseEntity.ok(arquivoMovido);

        } catch (RuntimeException e) {
            // Erros de negócio: arquivo não encontrado, sem permissão, etc.
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            // Erros de I/O (problemas ao mover fisicamente o arquivo)
            return ResponseEntity.internalServerError()
                    .body("Erro ao mover arquivo no sistema de arquivos: " + e.getMessage());

        } catch (Exception e) {
            // Fallback para qualquer outra exceção inesperada
            return ResponseEntity.internalServerError()
                    .body("Erro inesperado ao mover o arquivo: " + e.getMessage());
        }
    }

    // RF-020: Copiar arquivo
    @PostMapping("/{arquivoId}/copiar/{pastaDestinoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> copiarArquivo(
            @PathVariable Long arquivoId,
            @PathVariable Long pastaDestinoId,
            Authentication authentication
            ) {

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoCopiado = arquivoService.copiarArquivo(arquivoId, pastaDestinoId, usuarioLogado);
            return ResponseEntity.ok(arquivoCopiado);

        } catch (RuntimeException e) {
            // Erros de negócio: arquivo não encontrado, sem permissão, etc.
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            // Erros de I/O (problemas ao copiar fisicamente o arquivo)
            return ResponseEntity.internalServerError()
                    .body("Erro ao copiar arquivo no sistema de arquivos: " + e.getMessage());

        } catch (Exception e) {
            // Fallback para qualquer outra exceção inesperada
            return ResponseEntity.internalServerError()
                    .body("Erro inesperado ao copiar o arquivo: " + e.getMessage());
        }
    }


    // RF-017: Renomear arquivo
    @PutMapping("/renomear/{arquivoId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> renomearArquivo(
            @PathVariable Long arquivoId,
            @RequestBody Map<String, String> requestBody, // Altere aqui
            Authentication authentication
    ) {
        String novoNome = requestBody.get("novoNome");

        if (novoNome == null || novoNome.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("O novo nome não pode ser vazio.");
        }

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoRenomeado = arquivoService.renomearArquivo(arquivoId, novoNome, usuarioLogado);
            return ResponseEntity.ok(arquivoRenomeado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erro ao renomear arquivo no sistema de arquivos: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro inesperado ao renomear o arquivo: " + e.getMessage());
        }
    }

    // RF-021: Substituir arquivo
    @PostMapping("/{arquivoId}/substituir")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> substituirArquivo(
            @PathVariable Long arquivoId,
            @RequestParam("arquivo") MultipartFile arquivoMultipart,
            Authentication authentication) {

        System.out.println("Chamada recebida para substituir arquivoId=" + arquivoId);

        if (arquivoMultipart.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo enviado está vazio.");
        }

        try {
            // 1. Recuperar usuário logado
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            System.out.println("Usuário logado: " + usuarioLogado.getUsername());

            // 2. Chamar serviço para substituir o arquivo
            ArquivoDTO arquivoAtualizado = arquivoService.substituirArquivo(arquivoId, arquivoMultipart, usuarioLogado);

            System.out.println("Substituição concluída com sucesso!");
            return ResponseEntity.ok(arquivoAtualizado);

        } catch (ArquivoNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (PermissaoNegadaException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro de E/S ao substituir arquivo: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro inesperado ao substituir arquivo: " + e.getMessage());
        }
    }

    /**
     * RF-022: Excluir múltiplos ou todos os arquivos de uma pasta
     *
     * Ex.: DELETE /api/arquivos/pasta/10/excluir
     *      body = { "arquivoIds": [1,2,3] } ou vazio para todos
     */
    @DeleteMapping("/pasta/{pastaId}/excluir")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> excluirArquivosDaPasta(
            @PathVariable Long pastaId,
            @RequestBody(required = false) List<Long> arquivoIds,
            Authentication authentication) {

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);

            List<ArquivoDTO> arquivosExcluidos = arquivoService.excluirArquivosDaPasta(pastaId, arquivoIds, usuarioLogado);

            return ResponseEntity.ok(arquivosExcluidos);

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao excluir arquivos: " + e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @PostMapping("/pasta/{pastaId}/upload-multiplos")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> uploadMultiplosArquivos(
            @PathVariable Long pastaId,
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            Authentication authentication) {

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            List<ArquivoDTO> resultado = arquivoService.uploadArquivos(pastaId, arquivos, usuarioLogado);
            return ResponseEntity.ok(resultado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erro ao salvar arquivos: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro inesperado ao fazer upload: " + e.getMessage());
        }
    }

    /**
     * Lista arquivos com ordenação, filtros e paginação
     * Exemplo: GET /api/arquivos/pasta/1?page=0&size=10&sortField=nomeArquivo&sortDirection=asc&extensao=pdf
     */
    @GetMapping("/pasta/{pastaId}")
    public ResponseEntity<Page<Arquivo>> listarArquivos(
            @PathVariable Long pastaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nomeArquivo") String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String extensao) {

        Page<Arquivo> arquivos = arquivoService.listarArquivosPorPasta(
                pastaId,
                page,
                size,
                sortField,
                sortDirection,
                extensao
        );

        return ResponseEntity.ok(arquivos);
    }


    // Download individual de arquivo
    @GetMapping("/download/arquivo/{arquivoId}")
    public ResponseEntity<InputStreamResource> downloadArquivo(@PathVariable Long arquivoId) throws IOException {
        Arquivo arquivo = arquivoService.buscarPorId(arquivoId);
        Path caminho = Paths.get(arquivo.getCaminhoArmazenamento());

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(caminho));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                .contentLength(Files.size(caminho))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Download de pasta inteira em ZIP
    @GetMapping("/download/pasta/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<InputStreamResource> downloadPastaZip(@PathVariable Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new RuntimeException("Pasta não encontrada"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zipPastaRecursiva(pasta, "", zos);
        }

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(baos.toByteArray()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + pasta.getNomePasta() + ".zip")
                .contentLength(baos.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Método auxiliar para gerar ZIP recursivamente
    private void zipPastaRecursiva(Pasta pasta, String caminhoRelativo, ZipOutputStream zos) throws IOException {
        String prefixo = caminhoRelativo.isEmpty() ? "" : caminhoRelativo + "/";

        // Adiciona arquivos da pasta
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path caminhoArquivo = Paths.get(arquivo.getCaminhoArmazenamento());
            zos.putNextEntry(new ZipEntry(prefixo + arquivo.getNomeArquivo()));
            Files.copy(caminhoArquivo, zos);
            zos.closeEntry();
        }

        // Recursão para subpastas
        for (Pasta sub : pasta.getSubPastas()) {
            zipPastaRecursiva(sub, prefixo + sub.getNomePasta(), zos);
        }
    }
}
