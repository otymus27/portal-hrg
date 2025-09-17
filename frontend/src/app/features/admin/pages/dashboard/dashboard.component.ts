import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule, ChartComponent } from 'ng-apexcharts';
import {
  ApexNonAxisChartSeries,
  ApexResponsive,
  ApexChart,
  ApexTitleSubtitle,
  ApexAxisChartSeries,
  ApexXAxis,
  ApexStroke,
  ApexDataLabels,
  ApexGrid,
} from 'ng-apexcharts';
import { DashboardService, DashboardMetrics } from '../../services/dashboard.service';

export type ChartOptions = {
  series: ApexAxisChartSeries | ApexNonAxisChartSeries;
  chart: ApexChart;
  labels?: string[];
  title: ApexTitleSubtitle;
  responsive?: ApexResponsive[];
  xaxis?: ApexXAxis;
  stroke?: ApexStroke;
  dataLabels?: ApexDataLabels;
  grid?: ApexGrid;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  dashboardData: DashboardMetrics | null = null;
  isLoading = true;

  // 📊 Opções de gráficos
  uploadsPorDiaChartOptions: Partial<ChartOptions> = {
    series: [],
    chart: { type: 'line', height: 350 },
    title: { text: 'Uploads nos Últimos 30 Dias' },
    xaxis: { categories: [] },
    stroke: { curve: 'smooth' },
    dataLabels: { enabled: false },
    grid: { borderColor: '#f1f1f1' }
  };

  topUsuariosPorUploadChartOptions: Partial<ChartOptions> = {
    series: [],
    chart: { type: 'bar', height: 350 },
    title: { text: 'Top Usuários por Upload' },
    xaxis: { categories: [] }
  };

  distribuicaoPorTipoChartOptions: Partial<ChartOptions> = {
    series: [],
    chart: { type: 'donut', height: 350 },
    title: { text: 'Distribuição por Tipo de Arquivo' },
    labels: [],
    responsive: []
  };

  topTiposPorEspacoChartOptions: Partial<ChartOptions> = {
    series: [],
    chart: { type: 'pie', height: 350 },
    title: { text: 'Consumo de Espaço por Tipo' },
    labels: [],
    responsive: []
  };

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.dashboardService.getDashboardData().subscribe({
      next: (data) => {
        this.dashboardData = data;

        // 📈 Uploads por dia
        this.uploadsPorDiaChartOptions = {
          ...this.uploadsPorDiaChartOptions,
          series: [{ name: 'Uploads', data: data.uploadsPorDia.map(d => d.value) }],
          xaxis: { categories: data.uploadsPorDia.map(d => d.label) }
        };

        // 👥 Top usuários
        this.topUsuariosPorUploadChartOptions = {
          ...this.topUsuariosPorUploadChartOptions,
          series: [{ name: 'Uploads', data: data.topUsuariosPorUpload.map(d => d.value) }],
          xaxis: { categories: data.topUsuariosPorUpload.map(d => d.label) }
        };

        // 📂 Distribuição por tipo
        this.distribuicaoPorTipoChartOptions = {
          ...this.distribuicaoPorTipoChartOptions,
          series: data.distribuicaoPorTipo.map(d => d.value),
          labels: data.distribuicaoPorTipo.map(d => d.label)
        };

        // 💾 Consumo por tipo
        this.topTiposPorEspacoChartOptions = {
          ...this.topTiposPorEspacoChartOptions,
          series: data.topTiposPorEspaco.map(d => d.value),
          labels: data.topTiposPorEspaco.map(d => d.label)
        };

        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar dados do dashboard:', error);
        this.isLoading = false;
      }
    });
  }

  formatBytes(bytes: number, decimals = 2): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
}
