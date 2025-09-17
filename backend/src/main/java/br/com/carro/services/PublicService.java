package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoPublicoDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.DTO.PastaPublicaDTO;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PublicService {

    @Autowired
    private PastaRepository pastaRepository;
    private ArquivoRepository arquivoRepository;

    public PublicService(PastaRepository pastaRepository, ArquivoRepository arquivoRepository) {
        this.pastaRepository = pastaRepository;
        this.arquivoRepository = arquivoRepository;
    }

    /**
     * Lista as pastas públicas raiz com subpastas e arquivos carregados.
     */
    public List<PastaPublicaDTO> listarPastasPublicas() {
        List<Pasta> pastasRaiz = pastaRepository.findAllByPastaPaiIsNull();

        return pastasRaiz.stream()
                .map(PastaPublicaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca global de arquivos e pastas públicas por nome
     */
    public List<PastaPublicaDTO> buscarPorNome(String termo) {
        List<Pasta> pastas = pastaRepository.findByNomePastaContaining(termo);
        return pastas.stream()
                .map(PastaPublicaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Paginação + ordenação para arquivos e pastas públicas
     */
    public Page<PastaPublicaDTO> listarPastasPublicas(Pageable pageable) {
        Page<Pasta> page = pastaRepository.findAllBy(pageable);
        return page.map(PastaPublicaDTO::fromEntity);
    }

    public List<ArquivoPublicoDTO> listarArquivosPublicos(Long pastaId) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new RuntimeException("Pasta não encontrada"));

        return pasta.getArquivos()
                .stream()
                .map(ArquivoPublicoDTO::fromEntity)
                .toList();
    }

    /**
     * Listar arquivos na area publica por pastas
     */
    public Page<ArquivoPublicoDTO> listarArquivosPublicos(Long pastaId, Pageable pageable, String extensao, String sortBy, String order) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new RuntimeException("Pasta não encontrada"));

        Stream<Arquivo> streamArquivos = pasta.getArquivos().stream();

        if (extensao != null && !extensao.isEmpty()) {
            streamArquivos = streamArquivos.filter(a -> a.getNomeArquivo().endsWith("." + extensao));
        }

        List<ArquivoPublicoDTO> lista = streamArquivos
                .sorted(getComparator(sortBy, order))
                .map(ArquivoPublicoDTO::fromEntity)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), lista.size());
        return new PageImpl<>(lista.subList(start, end), pageable, lista.size());
    }


    // Helper para ordenação
    private Comparator<Arquivo> getComparator(String sortBy, String order) {
        Comparator<Arquivo> comp;
        switch (sortBy) {
            case "tamanho": comp = Comparator.comparing(Arquivo::getTamanho); break;
            case "dataUpload": comp = Comparator.comparing(Arquivo::getDataUpload); break;
            case "dataAtualizacao": comp = Comparator.comparing(Arquivo::getDataAtualizacao); break;
            default: comp = Comparator.comparing(Arquivo::getNomeArquivo);
        }
        return order.equalsIgnoreCase("asc") ? comp : comp.reversed();
    }


    /**
     * Download - retorna o arquivo físico como Resource para download.
     */
    public Resource getArquivoResource(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));

        try {
            Path path = Paths.get(arquivo.getCaminhoArmazenamento());
            if (!Files.exists(path)) {
                throw new RuntimeException("Arquivo físico não encontrado no servidor.");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao acessar o arquivo", e);
        }
    }


    /**
     * Gera um arquivo ZIP temporário contendo todos os arquivos da pasta (e subpastas).
     */
    public Resource criarZipDaPasta(Long pastaId) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new RuntimeException("Pasta não encontrada"));

        try {
            Path zipPath = Files.createTempFile("pasta_" + pastaId + "_", ".zip");
            try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                adicionarPastaAoZip(pasta, zs, "");
            }
            return new UrlResource(zipPath.toUri());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar ZIP da pasta", e);
        }
    }

    /**
     * Método recursivo para adicionar arquivos e subpastas ao ZIP.
     */
    private void adicionarPastaAoZip(Pasta pasta, ZipOutputStream zs, String basePath) throws IOException {
        String pastaPath = basePath + pasta.getNomePasta() + "/";

        for (Arquivo arquivo : pasta.getArquivos()) {
            Path arquivoPath = Paths.get(arquivo.getCaminhoArmazenamento());
            if (Files.exists(arquivoPath)) {
                zs.putNextEntry(new ZipEntry(pastaPath + arquivo.getNomeArquivo()));
                Files.copy(arquivoPath, zs);
                zs.closeEntry();
            }
        }

        for (Pasta sub : pasta.getSubPastas()) {
            adicionarPastaAoZip(sub, zs, pastaPath);
        }
    }

    /**
     * Retorna o conteúdo de um arquivo como Resource, pronto para visualização no navegador.
     */
    public Resource getArquivoParaVisualizacao(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado com id: " + id));

        File file = new File(arquivo.getCaminhoArmazenamento());
        if (!file.exists()) {
            throw new RuntimeException("Arquivo físico não encontrado no servidor.");
        }

        return new FileSystemResource(file);
    }

    /**
     * Descobre o Content-Type do arquivo para exibição correta no navegador.
     */
    public String getContentType(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado com id: " + id));

        try {
            Path path = Path.of(arquivo.getCaminhoArmazenamento());
            return Files.probeContentType(path); // tenta identificar o tipo de arquivo
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE; // fallback genérico
        }
    }



}
