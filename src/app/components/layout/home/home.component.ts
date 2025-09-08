import { Component, inject } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  DashboardMetrics,
  DashboardService,
} from '../../../services/dashboard.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private authService = inject(AuthService);
  private dashboardService = inject(DashboardService);

  // ✅ Propriedades para armazenar os dados do dashboard
  dashboardMetrics: DashboardMetrics | null = null;

  ngOnInit(): void {
    this.fetchDashboardData();
  }

  fetchDashboardData(): void {
    // ✅ Chame o serviço para buscar os dados
    this.dashboardService.getMetrics().subscribe({
      next: (data) => {
        this.dashboardMetrics = data;
      },
      error: (err) => {
        console.error('Falha ao buscar as métricas do dashboard:', err);
        // Opcional: exibir uma mensagem de erro ao usuário
      },
    });
  }

  // ✅ Método para verificar a role do usuário
  isAdmin(): boolean {
    const roles = this.authService.getLoggedInRoles();
    return roles.includes('ADMIN');
  }
}
