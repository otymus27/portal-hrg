package br.com.carro.services;

import br.com.carro.autenticacao.SessionTracker;
import br.com.carro.entities.DTO.DashboardDTO;
import br.com.carro.repositories.DashboardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final SessionTracker sessionTracker;

    public DashboardService(DashboardRepository dashboardRepository, SessionTracker sessionTracker) {
        this.dashboardRepository = dashboardRepository;
        this.sessionTracker = sessionTracker;
    }

    /**
     * Gera o DashboardDTO com todas as métricas consolidadas.
     *
     * @param diasHistorico Quantos dias considerar para o gráfico de uploads por dia.
     * @param limiteTop     Quantos usuários/tipos devem ser retornados nos rankings.
     * @return DashboardDTO preenchido
     */
    public DashboardDTO gerarDashboard(int diasHistorico, int limiteTop) {

        // ========================
        // MÉTRICAS GERAIS
        // ========================
        long totalArquivos = dashboardRepository.contarTotalArquivos();
        long totalPastas = dashboardRepository.contarTotalPastas();
        long totalEspacoBytes = dashboardRepository.somarEspacoTotalBytes();

        double totalEspacoMB = totalEspacoBytes / (1024.0 * 1024.0);
        double totalEspacoGB = totalEspacoBytes / (1024.0 * 1024.0 * 1024.0);

        // ========================
        // UPLOADS POR DIA
        // ========================
        LocalDate dataInicial = LocalDate.now().minusDays(diasHistorico);
        LocalDateTime inicio = dataInicial.atStartOfDay();
        Map<LocalDate, Long> uploadsPorDia = dashboardRepository.contarUploadsPorDia(inicio)
                .stream()
                .collect(Collectors.toMap(
                        obj -> ((LocalDateTime) obj[0]).toLocalDate(),
                        obj -> (Long) obj[1],
                        Long::sum,
                        LinkedHashMap::new
                ));

        // ========================
        // TOP USUÁRIOS
        // ========================
        Map<String, Long> topUsuariosPorUpload = dashboardRepository.topUsuariosPorUpload()
                .stream()
                .limit(limiteTop)
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> ((Number) obj[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        Map<String, Long> topUsuariosPorEspaco = dashboardRepository.topUsuariosPorEspaco()
                .stream()
                .limit(limiteTop)
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> ((Number) obj[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        // ========================
        // DISTRIBUIÇÃO POR TIPO
        // ========================
        Map<String, Long> distribuicaoPorTipo = dashboardRepository.distribuicaoPorTipo()
                .stream()
                .collect(Collectors.toMap(
                        obj -> obj[0] != null ? (String) obj[0] : "desconhecido",
                        obj -> ((Number) obj[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        Map<String, Long> topTiposPorEspaco = dashboardRepository.topTiposPorEspaco()
                .stream()
                .limit(limiteTop)
                .collect(Collectors.toMap(
                        obj -> obj[0] != null ? (String) obj[0] : "desconhecido",
                        obj -> ((Number) obj[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        // ========================
        // USUÁRIOS (SessionTracker)      // ========================


        long usuariosAtivosAgora = dashboardRepository.contarUsuariosAtivosAgora();
        long usuariosLogaramHoje = dashboardRepository.contarUsuariosLogaramHoje();

        // ========================
        // RETORNA DTO FINAL
        // ========================
        return new DashboardDTO(
                totalArquivos,
                totalPastas,
                totalEspacoBytes,
                totalEspacoMB,
                totalEspacoGB,
                uploadsPorDia,
                topUsuariosPorUpload,
                topUsuariosPorEspaco,
                distribuicaoPorTipo,
                topTiposPorEspaco,
                usuariosAtivosAgora,
                usuariosLogaramHoje
        );
    }
}