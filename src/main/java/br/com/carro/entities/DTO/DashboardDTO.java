package br.com.carro.entities.DTO;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO que representa os dados consolidados para exibição no Dashboard.
 */
public record DashboardDTO(

        // --- Métricas principais ---
        long totalArquivos,      // Quantidade total de arquivos cadastrados no sistema
        long totalPastas,        // Quantidade total de pastas
        long totalEspacoBytes,   // Espaço total ocupado pelos arquivos em bytes
        double totalEspacoMB,    // Espaço total em MB (para exibir de forma amigável)
        double totalEspacoGB,    // Espaço total em GB (para exibir de forma amigável)

        // --- Séries temporais ---
        Map<LocalDate, Long> uploadsPorDia, // Quantidade de arquivos enviados por dia (ex: últimos 7 ou 30 dias)

        // --- Rankings ---
        Map<String, Long> topUsuariosPorUpload, // Top N usuários que mais fizeram upload (usuário -> qtd arquivos)
        Map<String, Long> topUsuariosPorEspaco, // Top N usuários que mais consumiram espaço (usuário -> total bytes)

        // --- Distribuições ---
        Map<String, Long> distribuicaoPorTipo,  // Quantidade de arquivos agrupados por tipo/extensão (pdf -> 100, docx -> 45)
        Map<String, Long> topTiposPorEspaco     // Consumo de espaço por tipo de arquivo (pdf -> 200MB, etc)

) {}

