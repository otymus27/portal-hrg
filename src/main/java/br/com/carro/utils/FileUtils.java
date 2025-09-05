package br.com.carro.utils;

import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class FileUtils {

    /**
     * Valida e normaliza um nome de arquivo para evitar caracteres inválidos e ataques de caminho.
     * @param fileName O nome do arquivo a ser validado.
     * @return O nome do arquivo seguro.
     */
    public static String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("O nome do arquivo não pode ser vazio.");
        }
        // Remove caracteres que são inseguros ou inválidos em caminhos de arquivo
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        // Verifica se o nome do arquivo resultante não está vazio
        if (!StringUtils.hasText(sanitized)) {
            throw new IllegalArgumentException("O nome do arquivo resulta em um nome vazio após a sanitização.");
        }
        return sanitized;
    }
}