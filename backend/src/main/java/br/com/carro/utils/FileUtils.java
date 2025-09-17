package br.com.carro.utils;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public final class FileUtils {
    private FileUtils() { /* utilitário */ }

    /**
     * Remove um diretório e todo seu conteúdo recursivamente.
     * @param directory diretório a ser removido
     * @throws IOException em caso de erro de I/O
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (directory == null) return;
        deleteDirectory(directory.toPath());
    }

    /**
     * Remove um diretório e todo seu conteúdo recursivamente.
     * @param dirPath caminho do diretório
     * @throws IOException em caso de erro de I/O
     */
    public static void deleteDirectory(Path dirPath) throws IOException {
        if (dirPath == null || !Files.exists(dirPath)) return;

        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copia um diretório inteiro (conteúdo + subdiretórios) para outro local.
     * Se o destino não existir, será criado. Arquivos existentes serão sobrescritos.
     * @param sourceDir diretório de origem
     * @param targetDir diretório de destino
     * @throws IOException em caso de erro de I/O
     */
    public static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (sourceDir == null || targetDir == null) {
            throw new IllegalArgumentException("sourceDir e targetDir não podem ser nulos.");
        }
        copyDirectory(sourceDir.toPath().toFile(), targetDir.toPath().toFile());
    }

    /**
     * Copia um diretório inteiro (conteúdo + subdiretórios) para outro local.
     * Se o destino não existir, será criado. Arquivos existentes serão sobrescritos.
     * @param source caminho de origem
     * @param target caminho de destino
     * @throws IOException em caso de erro de I/O
     */
    public static Pasta copyDirectory(Pasta pastaOriginal, Path destino, Usuario usuarioLogado, PastaRepository pastaRepository, ArquivoRepository arquivoRepository) {
        // 1. Criar a pasta no sistema de arquivos
        try {
            Files.createDirectories(destino);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de destino: " + destino, e);
        }

        // 2. Criar a nova pasta no banco
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaOriginal.getNomePasta());
        novaPasta.setCaminhoCompleto(destino.toString());
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setDataAtualizacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuarioLogado);
        novaPasta.setUsuariosComPermissao(new HashSet<>(pastaOriginal.getUsuariosComPermissao()));
        novaPasta.setPastaPai(null); // vai ser setado no serviço antes de salvar se necessário
        novaPasta = pastaRepository.save(novaPasta);

        // 3. Copiar arquivos da pasta original
        for (Arquivo arquivoOriginal : pastaOriginal.getArquivos()) {
            Path caminhoOrigem = Paths.get(arquivoOriginal.getCaminhoArmazenamento());
            Path caminhoDestino = destino.resolve(arquivoOriginal.getNomeArquivo());

            try {
                Files.copy(caminhoOrigem, caminhoDestino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao copiar arquivo: " + arquivoOriginal.getNomeArquivo(), e);
            }

            // Criar novo registro no banco
            Arquivo novoArquivo = new Arquivo();
            novoArquivo.setNomeArquivo(arquivoOriginal.getNomeArquivo());
            novoArquivo.setCaminhoArmazenamento(caminhoDestino.toString());
            novoArquivo.setTamanho(arquivoOriginal.getTamanho());
            novoArquivo.setTipoMime(arquivoOriginal.getTipoMime());
            novoArquivo.setDataUpload(LocalDateTime.now());
            novoArquivo.setDataAtualizacao(LocalDateTime.now());
            novoArquivo.setCriadoPor(usuarioLogado);
            novoArquivo.setPasta(novaPasta);

            arquivoRepository.save(novoArquivo);
        }

        // 4. Copiar recursivamente as subpastas
        for (Pasta subOriginal : pastaOriginal.getSubPastas()) {
            Path destinoSub = destino.resolve(subOriginal.getNomePasta());
            Pasta novaSub = copyDirectory(subOriginal, destinoSub, usuarioLogado, pastaRepository, arquivoRepository);
            novaSub.setPastaPai(novaPasta);
            pastaRepository.save(novaSub);
            novaPasta.getSubPastas().add(novaSub);
        }

        return pastaRepository.save(novaPasta);
    }

    /**
     * Sanitiza um nome de arquivo/pasta substituindo caracteres proibidos por underscore.
     * @param name nome original
     * @return nome sanitizado
     */
    public static String sanitizeFileName(String name) {
        if (name == null) return null;
        // remove caracteres inválidos comuns em Windows/Unix: \ / : * ? " < > | e trims
        return name.trim().replaceAll("[\\\\/:*?\"<>|]+", "_");
    }


}