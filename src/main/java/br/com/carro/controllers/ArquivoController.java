package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.*;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.ArquivoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/arquivos")
public class ArquivoController {

    private final ArquivoService arquivoService;

    @Autowired
    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    // --- Endpoints de Acesso Público ---

    /**
     * Lista todos os arquivos dentro de uma pasta específica.
     * Acesso público, sem autenticação.
     */
    @GetMapping("/publico/pasta/{pastaId}")
    public ResponseEntity<List<ArquivoDTO>> listarArquivosPorPastaPublico(@PathVariable Long pastaId) {
        try {
            List<ArquivoDTO> arquivos = arquivoService.listarArquivosPorPasta(pastaId);
            return ResponseEntity.ok(arquivos);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Busca e retorna um arquivo por ID.
     * Acesso restrito a usuários com roles 'ADMIN', 'GERENTE' ou 'CLIENTE'.
     *
     * @param id O ID do arquivo a ser buscado.
     * @return ResponseEntity com ArquivoDTO se encontrado, ou 404 se não.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CLIENTE')")
    public ResponseEntity<ArquivoDTO> buscarArquivoPorId(@PathVariable Long id) {
        Optional<Arquivo> arquivoOptional = arquivoService.buscarArquivoPorId(id);

        if (arquivoOptional.isPresent()) {
            Arquivo arquivo = arquivoOptional.get();
            // Código corrigido
            ArquivoDTO arquivoDTO = new ArquivoDTO(
                    arquivo.getId(),
                    arquivo.getNomeArquivo(),
                    arquivo.getCaminhoArmazenamento(),
                    arquivo.getTamanhoBytes(),
                    arquivo.getDataUpload()
            );
            return ResponseEntity.ok(arquivoDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Endpoint para download de um arquivo pelo seu ID.
     * Acesso público, sem autenticação.
     */
    @GetMapping("/publico/download/{id}")
    public ResponseEntity<Resource> downloadArquivoPublico(@PathVariable Long id) {
        try {
            Resource resource = arquivoService.downloadArquivoPublico(id);

            String contentType = "application/octet-stream";
            if (resource.exists() && resource.isReadable()) {
                contentType = Files.probeContentType(resource.getFile().toPath());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Endpoints de Acesso Privado (Autenticado) ---

    /**
     * Endpoint unificado para upload de um ou mais arquivos.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<List<ArquivoDTO>> uploadArquivos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            List<ArquivoDTO> arquivosSalvos = arquivoService.uploadMultiplosArquivos(List.of(files), pastaId, usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(arquivosSalvos);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // O método de renomear foi removido. Use o PATCH para todas as atualizações de metadados.

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> atualizarMetadados(@PathVariable Long id, @RequestBody ArquivoUpdateDTO dto) {
        try {
            ArquivoDTO arquivoAtualizado = arquivoService.atualizarMetadados(id, dto);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Substitui o conteúdo de um arquivo por uma nova versão.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @PutMapping("/{id}/substituir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> substituirArquivo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            ArquivoDTO arquivoAtualizado = arquivoService.substituirArquivo(id, file);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            // MUDE ESTE CÓDIGO
            System.err.println("Erro durante a substituição do arquivo: " + e.getMessage());
            e.printStackTrace(); // Isso imprimirá a pilha de chamadas completa
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mover um arquivo para uma nova pasta.
     * Acesso restrito a usuários com roles 'ADMIN', 'GERENTE' ou 'BASIC'.
     */
    @PutMapping("/mover/{arquivoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<String> moverArquivo(
            @PathVariable Long arquivoId,
            @RequestBody ArquivoMoverDTO moverArquivoDTO,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            // Correct the method call to 'pastaId()'
            arquivoService.moverArquivo(arquivoId, moverArquivoDTO.pastaDestinoId(), usuario);
            return ResponseEntity.ok("Arquivo movido com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao mover arquivo: " + e.getMessage());
        }
    }

    /**
     * Copiar um arquivo para uma nova pasta.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @PostMapping("/copiar/{arquivoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> copiarArquivo(
            @PathVariable Long arquivoId,
            @RequestBody PastaDestinoDTO dto, // <--- Recebendo o DTO
            @AuthenticationPrincipal Usuario usuario) {
        try {
            ArquivoDTO arquivoCopiado = arquivoService.copiarArquivo(arquivoId, dto.pastaDestinoId(), usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(arquivoCopiado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui um arquivo pelo seu ID.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluirArquivo(@PathVariable Long id) {
        try {
            arquivoService.excluirArquivo(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui múltiplos arquivos por uma lista de IDs.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @DeleteMapping("/excluir-multiplos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluirMultiplosArquivos(@RequestBody List<Long> arquivoIds) {
        try {
            arquivoService.excluirMultiplosArquivos(arquivoIds);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui todos os arquivos de uma pasta por ID.
     * Acesso restrito a usuários com roles 'ADMIN' ou 'GERENTE'.
     */
    @DeleteMapping("/pasta/{id}/excluir-arquivos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluirTodosArquivosNaPasta(@PathVariable("id") Long pastaId) {
        try {
            arquivoService.excluirTodosArquivosNaPasta(pastaId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca arquivos por nome.
     * Acesso restrito a usuários com roles 'ADMIN', 'GERENTE' ou 'BASIC'.
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Page<ArquivoDTO>> buscarArquivosPorNome(
            @RequestParam("nome") String nome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nomeArquivo") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sortDir), sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ArquivoDTO> arquivos = arquivoService.buscarPorNome(nome, pageable);
        return ResponseEntity.ok(arquivos);
    }
}