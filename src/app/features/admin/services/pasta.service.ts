import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Pasta {
  id: number;
  nomePasta: string;
  subPastas?: Pasta[];
}

@Injectable({ providedIn: 'root' })
export class PastaService {
  // private readonly apiUrl = '/api/pastas';
  private apiUrl = 'http://localhost:8082/api/pastas';

  constructor(private http: HttpClient) {}

  listarPastasRaiz(): Observable<Pasta[]> {
    return this.http.get<Pasta[]>(`${this.apiUrl}/raiz`);
  }

  listarSubPastas(idPasta: number): Observable<Pasta[]> {
    return this.http.get<Pasta[]>(`${this.apiUrl}/${idPasta}/subpastas`);
  }
  
  criarPasta(nomePasta: string, pastaPaiId?: number): Observable<Pasta> {
    return this.http.post<Pasta>(this.apiUrl, { nomePasta, pastaPaiId });
  }

  excluirPasta(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  renomearPasta(id: number, novoNome: string): Observable<Pasta> {
    return this.http.patch<Pasta>(`${this.apiUrl}/${id}/renomear`, { novoNome });
  }

  moverPasta(id: number, novaPastaPaiId?: number): Observable<Pasta> {
    const params = novaPastaPaiId ? `?novaPastaPaiId=${novaPastaPaiId}` : '';
    return this.http.patch<Pasta>(`${this.apiUrl}/${id}/mover${params}`, {});
  }

  copiarPasta(id: number, destinoPastaId?: number): Observable<Pasta> {
    const params = destinoPastaId ? `?destinoPastaId=${destinoPastaId}` : '';
    return this.http.post<Pasta>(`${this.apiUrl}/${id}/copiar${params}`, {});
  }

  excluirPastasEmLote(idsPastas: number[], excluirConteudo = false): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/excluir-lote`, {
      body: { idsPastas, excluirConteudo }
    });
  }

  substituirPasta(idDestino: number, idOrigem: number): Observable<Pasta> {
    return this.http.put<Pasta>(`${this.apiUrl}/${idDestino}/substituir/${idOrigem}`, {});
  }
}
