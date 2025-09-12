package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.exceptions.ArquivoNaoEncontradoException;
import br.com.carro.exceptions.ErrorMessage;
import br.com.carro.exceptions.PermissaoNegadaException;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.services.ArquivoService;
import br.com.carro.utils.AuthService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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

    // ✅ ENDPOINT 01 - Upload de arquivo para uma pasta
    @PostMapping("/upload")
    public ResponseEntity<?> uploadArquivo(@RequestParam("file") MultipartFile file,
                                           @RequestParam("pastaId") Long pastaId,
                                           Authentication authentication,
                                           HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            Arquivo arquivo = arquivoService.uploadArquivo(file, pastaId, usuarioLogado, pastaRepository, arquivoRepository);
            return ResponseEntity.ok(arquivo);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.BAD_REQUEST.value(),
                    "Dados inválidos",
                    e.getMessage(),
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (AccessDeniedException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.FORBIDDEN.value(),
                    "Acesso negado",
                    "Você não tem permissão para atualizar fazer upload",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (Exception e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro ao fazer upload.",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ✅ ENDPOINT 02 - Excluir arquivo
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> excluirArquivo(@PathVariable Long id,
                                            Authentication authentication,
                                            HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            arquivoService.excluirArquivo(id, usuarioLogado);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(),
                            "Arquivo não encontrado",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorMessage(HttpStatus.FORBIDDEN.value(),
                            "Acesso negado",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao excluir arquivo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro interno",
                            "Erro ao excluir arquivo",
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 03 - Mover arquivo
    @PutMapping("/{arquivoId}/mover/{pastaDestinoId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> moverArquivo(@PathVariable Long arquivoId,
                                          @PathVariable Long pastaDestinoId,
                                          Authentication authentication,
                                          HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoMovido = arquivoService.moverArquivo(arquivoId, pastaDestinoId, usuarioLogado);
            return ResponseEntity.ok(arquivoMovido);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Erro ao mover arquivo",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao mover arquivo no sistema de arquivos: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao mover arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 04 - Copiar arquivo
    @PostMapping("/{arquivoId}/copiar/{pastaDestinoId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> copiarArquivo(@PathVariable Long arquivoId,
                                           @PathVariable Long pastaDestinoId,
                                           Authentication authentication,
                                           HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoCopiado = arquivoService.copiarArquivo(arquivoId, pastaDestinoId, usuarioLogado);
            return ResponseEntity.ok(arquivoCopiado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Erro ao copiar arquivo",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao copiar arquivo no sistema de arquivos: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao copiar arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 05 - Renomear arquivo
    @PutMapping("/renomear/{arquivoId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> renomearArquivo(@PathVariable Long arquivoId,
                                             @RequestBody Map<String, String> requestBody,
                                             Authentication authentication,
                                             HttpServletRequest httpRequest) {
        String novoNome = requestBody.get("novoNome");
        if (novoNome == null || novoNome.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Campo obrigatório",
                            "O novo nome não pode ser vazio",
                            httpRequest.getRequestURI()));
        }

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoRenomeado = arquivoService.renomearArquivo(arquivoId, novoNome, usuarioLogado);
            return ResponseEntity.ok(arquivoRenomeado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Erro ao renomear arquivo",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao renomear arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao renomear arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 06 - Substituir arquivo
    @PostMapping("/{arquivoId}/substituir")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> substituirArquivo(@PathVariable Long arquivoId,
                                               @RequestParam("arquivo") MultipartFile arquivoMultipart,
                                               Authentication authentication,
                                               HttpServletRequest httpRequest) {
        if (arquivoMultipart.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Arquivo vazio",
                            "Arquivo enviado está vazio",
                            httpRequest.getRequestURI()));
        }

        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            ArquivoDTO arquivoAtualizado = arquivoService.substituirArquivo(arquivoId, arquivoMultipart, usuarioLogado);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (ArquivoNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(),
                            "Arquivo não encontrado",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (PermissaoNegadaException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorMessage(HttpStatus.FORBIDDEN.value(),
                            "Acesso negado",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao substituir arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao substituir arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 07 - Excluir múltiplos arquivos de uma pasta
    @DeleteMapping("/pasta/{pastaId}/excluir")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> excluirArquivosDaPasta(@PathVariable Long pastaId,
                                                    @RequestBody(required = false) List<Long> arquivoIds,
                                                    Authentication authentication,
                                                    HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            List<ArquivoDTO> arquivosExcluidos = arquivoService.excluirArquivosDaPasta(pastaId, arquivoIds, usuarioLogado);
            return ResponseEntity.ok(arquivosExcluidos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Erro ao excluir arquivos",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao excluir arquivos: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao excluir arquivos: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 08 - Upload de múltiplos arquivos
    @PostMapping("/pasta/{pastaId}/upload-multiplos")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> uploadMultiplosArquivos(@PathVariable Long pastaId,
                                                     @RequestParam("arquivos") List<MultipartFile> arquivos,
                                                     Authentication authentication,
                                                     HttpServletRequest httpRequest) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            List<ArquivoDTO> resultado = arquivoService.uploadArquivos(pastaId, arquivos, usuarioLogado);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(),
                            "Erro ao fazer upload",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao salvar arquivos: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro inesperado ao fazer upload: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 09 - Listar arquivos com paginação, filtros e ordenação
    @GetMapping("/pasta/{pastaId}")
    public ResponseEntity<?> listarArquivos(@PathVariable Long pastaId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "nomeArquivo") String sortField,
                                            @RequestParam(defaultValue = "asc") String sortDirection,
                                            @RequestParam(required = false) String extensao,
                                            HttpServletRequest httpRequest) {
        try {
            Page<Arquivo> arquivos = arquivoService.listarArquivosPorPasta(
                    pastaId, page, size, sortField, sortDirection, extensao
            );
            return ResponseEntity.ok(arquivos);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(),
                            "Pasta não encontrada",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            logger.error("Erro ao listar arquivos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro interno ao listar arquivos",
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 10 - Download de arquivo por id
    @GetMapping("/download/arquivo/{arquivoId}")
    public ResponseEntity<?> downloadArquivo(@PathVariable Long arquivoId,
                                             HttpServletRequest httpRequest) {
        try {
            Arquivo arquivo = arquivoService.buscarPorId(arquivoId);
            Path caminho = Paths.get(arquivo.getCaminhoArmazenamento());
            if (!Files.exists(caminho)) {
                throw new RuntimeException("Arquivo físico não encontrado");
            }

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(caminho));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                    .contentLength(Files.size(caminho))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(),
                            "Arquivo não encontrado",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao ler arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer download de arquivo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro ao fazer download do arquivo: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 11 - Download de pasta inteira (zip)
    @GetMapping("/download/pasta/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<?> downloadPastaZip(@PathVariable Long pastaId,
                                              HttpServletRequest httpRequest) {
        try {
            Pasta pasta = pastaRepository.findById(pastaId)
                    .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada"));

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

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(),
                            "Pasta não encontrada",
                            e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro de I/O",
                            "Erro ao criar arquivo zip: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao fazer download da pasta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro inesperado",
                            "Erro ao fazer download da pasta: " + e.getMessage(),
                            httpRequest.getRequestURI()));
        }
    }

    // ✅ ENDPOINT 11 - Buscar arquivo por id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','BASIC')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id,
                                         HttpServletRequest httpRequest) {
        try {

            Arquivo arquivo = arquivoService.buscarPorId(id);

            return ResponseEntity.ok(arquivo);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(
                            HttpStatus.NOT_FOUND.value(),
                            "Arquivo não encontrado",
                            e.getMessage(),
                            httpRequest.getRequestURI()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Erro interno",
                            "Erro ao buscar arquivo",
                            httpRequest.getRequestURI()
                    ));
        }
    }
    // ✅ Método Auxiliar para zipar pasta recursivamente
    private void zipPastaRecursiva(Pasta pasta, String caminhoRelativo, ZipOutputStream zos) throws IOException {
        String prefixo = caminhoRelativo.isEmpty() ? "" : caminhoRelativo + "/";

        // Adiciona arquivos da pasta
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path caminhoArquivo = Paths.get(arquivo.getCaminhoArmazenamento());
            if (Files.exists(caminhoArquivo)) {
                zos.putNextEntry(new ZipEntry(prefixo + arquivo.getNomeArquivo()));
                Files.copy(caminhoArquivo, zos);
                zos.closeEntry();
            }
        }

        // Recursão para subpastas
        for (Pasta sub : pasta.getSubPastas()) {
            zipPastaRecursiva(sub, prefixo + sub.getNomePasta(), zos);
        }
    }


}
