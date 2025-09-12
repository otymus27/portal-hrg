import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface MetricData {
  label: string;
  value: number;
}

export interface DashboardMetrics {
  totalArquivos: number;
  totalPastas: number;
  totalEspacoBytes: number;
  totalEspacoMB: number;
  totalEspacoGB: number;
  uploadsPorDia: MetricData[];
  topUsuariosPorUpload: MetricData[];
  topUsuariosPorEspaco: MetricData[];
  distribuicaoPorTipo: MetricData[];
  topTiposPorEspaco: MetricData[];
}

interface RawDashboardData {
  totalArquivos: number;
  totalPastas: number;
  totalEspacoBytes: number;
  totalEspacoMB: number;
  totalEspacoGB: number;
  uploadsPorDia: Record<string, number>;
  topUsuariosPorUpload: Record<string, number>;
  topUsuariosPorEspaco: Record<string, number>;
  distribuicaoPorTipo: Record<string, number>;
  topTiposPorEspaco: Record<string, number>;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private apiUrl = 'http://localhost:8082/api/estatisticas';

  constructor(private http: HttpClient) {}

  private transformRawData(raw: RawDashboardData): DashboardMetrics {
    const transform = (obj: Record<string, number>): MetricData[] => {
      return Object.entries(obj).map(([label, value]) => ({ label, value }));
    };

    return {
      ...raw,
      uploadsPorDia: transform(raw.uploadsPorDia),
      topUsuariosPorUpload: transform(raw.topUsuariosPorUpload),
      topUsuariosPorEspaco: transform(raw.topUsuariosPorEspaco),
      distribuicaoPorTipo: transform(raw.distribuicaoPorTipo),
      topTiposPorEspaco: transform(raw.topTiposPorEspaco),
    };
  }

  getDashboardData(
    diasHistorico: number = 30,
    limiteTop: number = 5
  ): Observable<DashboardMetrics> {
    const params = new HttpParams()
      .set('diasHistorico', diasHistorico.toString())
      .set('limiteTop', limiteTop.toString());

    return this.http
      .get<RawDashboardData>(this.apiUrl, { params })
      .pipe(map((rawData) => this.transformRawData(rawData)));
  }
}
