package br.com.carro.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class ArquivoUtils {// Salvar arquivo enviado via MultipartFile
    public Path salvarArquivo(MultipartFile file, String caminhoPasta) throws IOException {
        Path dir = Paths.get(caminhoPasta);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path destino = dir.resolve(sanitizeFileName(file.getOriginalFilename()));
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    // Renomear arquivo
    public Path renomearArquivo(String caminhoAtual, String novoNome) throws IOException {
        Path arquivoAtual = Paths.get(caminhoAtual);
        if (!Files.exists(arquivoAtual)) {
            throw new IOException("Arquivo não encontrado: " + caminhoAtual);
        }
        Path novoCaminho = arquivoAtual.getParent().resolve(sanitizeFileName(novoNome));
        return Files.move(arquivoAtual, novoCaminho, StandardCopyOption.REPLACE_EXISTING);
    }

    // Excluir arquivo
    public void deleteFile(String caminho) throws IOException {
        Path arquivo = Paths.get(caminho);
        if (Files.exists(arquivo)) {
            Files.delete(arquivo);
        }
    }

    // Mover arquivo
    public Path moverArquivo(String caminhoAtual, String caminhoDestinoPasta) throws IOException {
        Path arquivoAtual = Paths.get(caminhoAtual);
        Path destinoDir = Paths.get(caminhoDestinoPasta);

        if (!Files.exists(destinoDir)) {
            Files.createDirectories(destinoDir);
        }

        Path destino = destinoDir.resolve(arquivoAtual.getFileName());
        return Files.move(arquivoAtual, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    // Copiar arquivo
    public Path copiarArquivo(String caminhoAtual, String caminhoDestinoPasta) throws IOException {
        Path arquivoAtual = Paths.get(caminhoAtual);
        Path destinoDir = Paths.get(caminhoDestinoPasta);

        if (!Files.exists(destinoDir)) {
            Files.createDirectories(destinoDir);
        }

        Path destino = destinoDir.resolve(arquivoAtual.getFileName());
        return Files.copy(arquivoAtual, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    // **Substituir arquivo existente pelo novo arquivo**
    public Path substituirArquivo(String caminhoAtual, Path novoArquivo) throws IOException {
        Path arquivoExistente = Paths.get(caminhoAtual);

        if (!Files.exists(arquivoExistente)) {
            throw new IOException("Arquivo a ser substituído não encontrado: " + caminhoAtual);
        }

        // Opção 1: mover o novo arquivo para o caminho antigo, sobrescrevendo
        return Files.move(novoArquivo, arquivoExistente, StandardCopyOption.REPLACE_EXISTING);
    }

    // Sanitiza nomes de arquivos para evitar caracteres inválidos
    public static String sanitizeFileName(String nome) {
        return nome.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}