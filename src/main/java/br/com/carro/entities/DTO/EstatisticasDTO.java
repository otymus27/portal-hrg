package br.com.carro.entities.DTO;

import java.time.LocalDate;
import java.util.Map;

public record EstatisticasDTO(

        long totalArquivos,
        long totalPastas,
        long totalEspacoBytes,
        double totalEspacoMB,
        double totalEspacoGB,

        Map<LocalDate, Long> uploadsPorDia,

        Map<String, Long> topUsuariosPorUpload,
        Map<String, Long> topUsuariosPorEspaco,

        Map<String, Long> distribuicaoPorTipo,
        Map<String, Long> topTiposPorEspaco

) {}
