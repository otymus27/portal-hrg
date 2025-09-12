package br.com.carro.controllers;

import br.com.carro.entities.DTO.DashboardDTO;
import br.com.carro.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estatisticas")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Endpoint para retornar o dashboard com métricas do sistema.
     *
     * @param diasHistorico Número de dias para o gráfico de uploads por dia (opcional, default 30)
     * @param limiteTop     Quantos usuários/tipos considerar nos rankings (opcional, default 5)
     * @return DashboardDTO com todas as métricas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<DashboardDTO> getDashboard(
            @RequestParam(value = "diasHistorico", defaultValue = "30") int diasHistorico,
            @RequestParam(value = "limiteTop", defaultValue = "5") int limiteTop
    ) {
        try {
            DashboardDTO dashboard = dashboardService.gerarDashboard(diasHistorico, limiteTop);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            e.printStackTrace(); // mantém log completo do erro no backend
            return ResponseEntity.internalServerError().build();
        }
    }
}
