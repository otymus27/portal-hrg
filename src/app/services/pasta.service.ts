import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ItemDTO {
  nome: string;
  isDiretorio: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class PastaService {
  private apiUrl = 'http://localhost:8082/api/publico/pastas';

  constructor(private http: HttpClient) {}

  /**
   * Obtém a lista de arquivos e pastas de um diretório específico.
   * @param caminho O caminho do diretório (opcional).
   * @returns Um Observable com a lista de ItemDTOs.
   */
  getConteudoPasta(caminho?: string): Observable<ItemDTO[]> {
    let params = new HttpParams();
    if (caminho) {
      params = params.set('caminho', caminho);
    }
    return this.http.get<ItemDTO[]>(this.apiUrl, { params });
  }

  downloadArquivo(caminho: string): void {
    const url = `http://localhost:8082/api/publico/arquivos/download?caminho=${encodeURIComponent(
      caminho
    )}`;
    window.open(url, '_blank');
  }
}
