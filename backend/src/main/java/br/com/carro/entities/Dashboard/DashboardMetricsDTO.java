package br.com.carro.entities.Dashboard;

import java.util.List;

public record DashboardMetricsDTO(
        long totalCarros,
        long totalUsuarios,
        long totalMarcas,
        long totalProprietarios
) {
}
