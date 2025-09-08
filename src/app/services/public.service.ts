import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PastaPublica {
  id: number;
  nome: string;
  caminhoCompleto: string;
  subPastas: PastaPublica[];
  arquivos: ArquivoPublico[];
}

export interface ArquivoPublico {
  id: number;
  nome: string;
  tamanho: number;
  tipoMime: string;
}

@Injectable({ providedIn: 'root' })
export class PublicService {
  private apiUrl = 'http://localhost:8082/api/publico';

  constructor(private http: HttpClient) {}

  listarPastas(): Observable<PastaPublica[]> {
    return this.http.get<PastaPublica[]>(`${this.apiUrl}/pastas`);
  }

  listarArquivos(pastaId: number): Observable<ArquivoPublico[]> {
    return this.http.get<ArquivoPublico[]>(
      `${this.apiUrl}/pastas/${pastaId}/arquivos`
    );
  }

  downloadArquivo(arquivoId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/arquivo/${arquivoId}`, {
      responseType: 'blob',
    });
  }

  downloadPasta(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/pasta/${id}`, {
      responseType: 'blob',
    });
  }
}
