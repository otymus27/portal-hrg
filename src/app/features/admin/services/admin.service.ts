import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Paginacao<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface PastaAdmin {
  id: string;
  nomePasta: string;
  subPastas?: PastaAdmin[];
  arquivos?: ArquivoAdmin[];
}

export interface ArquivoAdmin {
  id: string;
  nome: string;
  tipo: string;
  tamanho: number;
  dataModificacao: string;
  url: string;
}

export interface ConteudoPasta {
  pastas: PastaAdmin[];
  arquivos: ArquivoAdmin[];
}

export interface PastaCompletaDTO {
  id: number;
  nomePasta: string;
  caminhoCompleto: string;
  dataCriacao: string;
  dataAtualizacao: string;
  arquivos: ArquivoAdmin[];
  subPastas: PastaCompletaDTO[];
}

export interface PastaExcluirDTO {
  idsPastas: string[];
  excluirConteudo: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrlAdminPastas = 'http://localhost:8082/api/pastas';
  private readonly apiUrlAdminArquivos = 'http://localhost:8082/api/arquivos';
  private readonly apiUrlPublica = 'http://localhost:8082/api/publico';

  constructor(private http: HttpClient) {}

  // --- Listagem ---
  listarConteudoRaiz(): Observable<ConteudoPasta> {
    return this.http
      .get<PastaAdmin[]>(`${this.apiUrlAdminPastas}/subpastas`)
      .pipe(
        map((pastas) => ({
          pastas,
          arquivos: [],
        }))
      );
  }

  listarConteudoPorId(pastaId: string): Observable<ConteudoPasta> {
    return this.http
      .get<PastaAdmin>(`${this.apiUrlAdminPastas}/${pastaId}`)
      .pipe(
        map((pasta) => ({
          pastas: pasta.subPastas || [],
          arquivos: pasta.arquivos || [],
        }))
      );
  }

  downloadArquivo(arquivoId: string): Observable<Blob> {
    return this.http.get(
      `${this.apiUrlPublica}/download/arquivo/${arquivoId}`,
      {
        responseType: 'blob',
      }
    );
  }

  // --- Pastas ---
  criarPasta(body: {
    nome: string;
    pastaPaiId?: string;
    usuariosComPermissaoIds: number[];
  }): Observable<PastaAdmin> {
    return this.http.post<PastaAdmin>(this.apiUrlAdminPastas, body);
  }

  excluirPasta(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminPastas}/${id}`);
  }

  renomearPasta(id: string, novoNome: string): Observable<PastaAdmin> {
    return this.http.patch<PastaAdmin>(
      `${this.apiUrlAdminPastas}/${id}/renomear`,
      { novoNome }
    );
  }

  excluirPastasEmLote(dto: PastaExcluirDTO): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminPastas}/excluir-lote`, {
      body: dto,
    });
  }

  // --- Arquivos ---
  uploadArquivo(file: File, pastaId: string): Observable<ArquivoAdmin> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('pastaId', pastaId);
    return this.http.post<ArquivoAdmin>(
      `${this.apiUrlAdminArquivos}/upload`,
      formData
    );
  }

  excluirArquivo(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminArquivos}/arquivos/${id}`);
  }

  renomearArquivo(id: string, novoNome: string): Observable<ArquivoAdmin> {
    return this.http.put<ArquivoAdmin>(
      `${this.apiUrlAdminArquivos}/renomear/${id}`,
      { novoNome }
    );
  }
}
