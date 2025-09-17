import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Arquivo {
  id: number;
  nomeArquivo: string;
  tamanho: number;
  extensao: string;
  caminhoArmazenamento: string;
  dataUpload: string;
}

@Injectable({ providedIn: 'root' })
export class ArquivoService {
  // private readonly apiUrl = '/api/arquivos';
  private apiUrl = 'http://localhost:8082/api/arquivos';

  constructor(private http: HttpClient) {}

  upload(file: File, pastaId: number): Observable<Arquivo> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('pastaId', pastaId.toString());
    return this.http.post<Arquivo>(`${this.apiUrl}/upload`, formData);
  }

  uploadMultiplos(arquivos: File[], pastaId: number): Observable<Arquivo[]> {
    const formData = new FormData();
    arquivos.forEach(file => formData.append('arquivos', file));
    return this.http.post<Arquivo[]>(`${this.apiUrl}/pasta/${pastaId}/upload-multiplos`, formData);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  excluirVarios(pastaId: number, arquivoIds?: number[]): Observable<any> {
    return this.http.delete(`${this.apiUrl}/pasta/${pastaId}/excluir`, {
      body: arquivoIds || []
    });
  }

  mover(arquivoId: number, pastaDestinoId: number): Observable<Arquivo> {
    return this.http.put<Arquivo>(`${this.apiUrl}/${arquivoId}/mover/${pastaDestinoId}`, {});
  }

  copiar(arquivoId: number, pastaDestinoId: number): Observable<Arquivo> {
    return this.http.post<Arquivo>(`${this.apiUrl}/${arquivoId}/copiar/${pastaDestinoId}`, {});
  }

  renomear(arquivoId: number, novoNome: string): Observable<Arquivo> {
    return this.http.put<Arquivo>(`${this.apiUrl}/renomear/${arquivoId}`, { novoNome });
  }

  substituir(arquivoId: number, arquivo: File): Observable<Arquivo> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return this.http.post<Arquivo>(`${this.apiUrl}/${arquivoId}/substituir`, formData);
  }

  listarPorPasta(
    pastaId: number,
    page = 0,
    size = 10,
    sortField = 'nomeArquivo',
    sortDirection = 'asc',
    extensao?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortField', sortField)
      .set('sortDirection', sortDirection);
    if (extensao) params = params.set('extensao', extensao);

    return this.http.get<any>(`${this.apiUrl}/pasta/${pastaId}`, { params });
  }

  downloadArquivo(arquivoId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/arquivo/${arquivoId}`, {
      responseType: 'blob'
    });
  }

  downloadPasta(pastaId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/pasta/${pastaId}`, {
      responseType: 'blob'
    });
  }
}
