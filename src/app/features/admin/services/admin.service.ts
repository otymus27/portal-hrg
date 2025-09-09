import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

// Interfaces exclusivas para a área administrativa
export interface PastaAdmin {
  id: string;
  nome: string;
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

// Interface para o retorno da lista de pastas e arquivos
export interface ConteudoPasta {
  pastas: PastaAdmin[];
  arquivos: ArquivoAdmin[];
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  // Endpoints Privados (admin) para operações de CRUD
  private readonly apiUrlAdminPastas = 'http://localhost:8082/api/pastas';
  private readonly apiUrlAdminArquivos = 'http://localhost:8082/api/arquivos';

  // Endpoints Públicos para listagem e download
  private readonly apiUrlPublica = 'http://localhost:8082/api/publico';

  constructor(private http: HttpClient) {}

  // --- Métodos de Listagem (acesso público) ---
  listarConteudoRaiz(): Observable<ConteudoPasta> {
    // Lista apenas as pastas raiz, sem subpastas ou arquivos aninhados
    return this.http.get<PastaAdmin[]>(`${this.apiUrlPublica}/pastas`).pipe(
      map(pastas => ({
        pastas,
        arquivos: []
      }))
    );
  }
  
  // Novo método para buscar pastas e arquivos por ID no backend
  listarConteudoPorId(pastaId: string): Observable<ConteudoPasta> {
    // Busca uma pasta específica e seu conteúdo aninhado por ID
    return this.http.get<PastaAdmin>(`${this.apiUrlAdminPastas}/${pastaId}`).pipe(
      map(pasta => ({
        pastas: pasta.subPastas || [],
        arquivos: pasta.arquivos || []
      }))
    );
  }

  downloadArquivo(arquivoId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrlPublica}/arquivos/${arquivoId}/download`, {
      responseType: 'blob'
    });
  }

  // --- Métodos de Pasta (acesso privado) ---
  criarPasta(nomePasta: string, pastaPaiId?: string): Observable<PastaAdmin> {
    const body = { nomePasta, pastaPaiId };
    return this.http.post<PastaAdmin>(this.apiUrlAdminPastas, body);
  }

  excluirPasta(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminPastas}/${id}`);
  }

  renomearPasta(id: string, novoNome: string): Observable<PastaAdmin> {
    return this.http.patch<PastaAdmin>(`${this.apiUrlAdminPastas}/${id}/renomear`, { novoNome });
  }

  // --- Métodos de Arquivo (acesso privado) ---
  uploadArquivo(file: File, pastaId: string): Observable<ArquivoAdmin> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('pastaId', pastaId);
    return this.http.post<ArquivoAdmin>(`${this.apiUrlAdminArquivos}/upload`, formData);
  }

  excluirArquivo(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminArquivos}/${id}`);
  }

  renomearArquivo(id: string, novoNome: string): Observable<ArquivoAdmin> {
    return this.http.put<ArquivoAdmin>(`${this.apiUrlAdminArquivos}/renomear/${id}`, { novoNome });
  }
}
