package br.com.carro.controllers;

import br.com.carro.entities.DTO.ArquivoPublicoDTO;
import br.com.carro.entities.DTO.PastaPublicaDTO;
import br.com.carro.services.PublicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publico")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private PublicService publicService;

    // RF-001: Listagem de pastas públicas hierárquica
    @GetMapping("/pastas")
    public ResponseEntity<List<PastaPublicaDTO>> listarPastasPublicas() {
        return ResponseEntity.ok(publicService.listarPastasPublicas());
    }

    // RF-007: Busca global por nome
    @GetMapping("/busca")
    public ResponseEntity<List<PastaPublicaDTO>> buscarPorNome(@RequestParam String termo) {
        return ResponseEntity.ok(publicService.buscarPorNome(termo));
    }

    // RF-004: Paginação + ordenação
    @GetMapping("/pastas/pagina")
    public ResponseEntity<?> listarPastasPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nome") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(publicService.listarPastasPublicas(pageable));
    }

    // RF-002: Visualização de Arquivos
    @GetMapping("/pastas/{id}/arquivos")
    public ResponseEntity<List<ArquivoPublicoDTO>> listarArquivosPublicos(@PathVariable Long id) {
        return ResponseEntity.ok(publicService.listarArquivosPublicos(id));
    }

    // RF-003 - Ordenação e Filtros de Arquivos
    @GetMapping("/pastas/{id}/arquivos/pagina")
    public ResponseEntity<?> listarArquivosPaginados(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nome") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String extensao
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(publicService.listarArquivosPublicos(id, pageable, extensao, sortBy, order));
    }


    //Acesso na area publica - RF-005 – Download de Arquivos
    @GetMapping("/download/arquivo/{id}")
    public ResponseEntity<Resource> downloadArquivo(@PathVariable Long id) {
        Resource file = publicService.getArquivoResource(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    //Acesso na area publica - RF-006 – Download de Pastas Inteiras (ZIP)
    @GetMapping("/download/pasta/{id}")
    public ResponseEntity<Resource> downloadPastaZip(@PathVariable Long id) {
        Resource zipFile = publicService.criarZipDaPasta(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pasta_" + id + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipFile);
    }


    /**
     * Retorna o conteúdo do arquivo diretamente, para visualização no navegador.
     */
    @GetMapping("/visualizar/arquivo/{id}")
    public ResponseEntity<Resource> visualizarArquivo(@PathVariable Long id) {
        Resource resource = publicService.getArquivoParaVisualizacao(id);
        String contentType = publicService.getContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }


}
