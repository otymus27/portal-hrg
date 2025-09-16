import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Usuario } from '../../../models/usuario';

// ✅ INTERFACE PAGINACAO COMPLETA
export interface Paginacao<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  pageable: {
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    pageNumber: number;
    pageSize: number;
    unpaged: boolean;
    paged: boolean;
  };
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
}

export interface PastaAdmin {
  id: number;
  nomePasta: string;
  caminhoCompleto: string;
  dataCriacao: Date;
  criadoPor: any;
  subPastas?: PastaAdmin[];
  arquivos?: ArquivoAdmin[];
  usuariosComPermissao?: Usuario[];
}

export interface ArquivoAdmin {
  id: number;
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
  idsPastas: number[];
  excluirConteudo: boolean;
}

export interface PastaPermissaoAcaoDTO {
  pastaId: number;
  adicionarUsuariosIds: number[];
  removerUsuariosIds: number[];
}

// ✅ Interface para o DTO de resumo do usuário
export interface UsuarioResumoDTO {
  id: number;
  username: string;
}

// ✅ Interface que recebe json de tratamento de erros do backend
export interface ErrorMessage {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrlAdminPastas = 'http://localhost:8082/api/pastas';
  private readonly apiUrlAdminArquivos = 'http://localhost:8082/api/arquivos';
  private readonly apiUrlPublica = 'http://localhost:8082/api/publico';
  private readonly apiUrlUsuarios = 'http://localhost:8082/api/usuario';

  constructor(private http: HttpClient) {}

  // --- Listagem ---
  listarConteudoRaiz(): Observable<ConteudoPasta> {
    return this.http
      .get<PastaAdmin[]>(`${this.apiUrlAdminPastas}/subpastas`)
      .pipe(
        map((pastas) => ({ pastas, arquivos: [] })),
        catchError(this.tratarErro)
      );
  }

  listarConteudoPorId(pastaId: number): Observable<ConteudoPasta> {
    return this.http
      .get<PastaAdmin>(`${this.apiUrlAdminPastas}/${pastaId}`)
      .pipe(
        map((pasta) => ({
          pastas: pasta.subPastas || [],
          arquivos: pasta.arquivos || [],
        })),
        catchError(this.tratarErro)
      );
  }

  downloadArquivo(arquivoId: number): Observable<Blob> {
    return this.http
      .get(`${this.apiUrlPublica}/download/arquivo/${arquivoId}`, {
        responseType: 'blob',
      })
      .pipe(catchError(this.tratarErro));
  }

  // --- Pastas ---
  criarPasta(body: {
    nome: string;
    pastaPaiId?: number;
    usuariosComPermissaoIds: number[];
  }): Observable<PastaAdmin> {
    return this.http
      .post<PastaAdmin>(this.apiUrlAdminPastas, body)
      .pipe(catchError(this.tratarErro));
  }

  excluirPasta(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrlAdminPastas}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  renomearPasta(id: number, novoNome: string): Observable<PastaAdmin> {
    return this.http
      .patch<PastaAdmin>(`${this.apiUrlAdminPastas}/${id}/renomear`, {
        novoNome,
      })
      .pipe(catchError(this.tratarErro));
  }

  excluirPastasEmLote(dto: PastaExcluirDTO): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrlAdminPastas}/excluir-lote`, { body: dto })
      .pipe(catchError(this.tratarErro));
  }

  // --- Upload ---
  uploadArquivo(file: File, pastaId: number): Observable<ArquivoAdmin> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('pastaId', pastaId.toString());
    return this.http
      .post<ArquivoAdmin>(`${this.apiUrlAdminArquivos}/upload`, formData)
      .pipe(catchError(this.tratarErro));
  }

  uploadMultiplosArquivos(files: File[], pastaId: number) {
    const formData = new FormData();
    files.forEach((file) => formData.append('arquivos', file));

    return this.http
      .post(
        `${this.apiUrlAdminArquivos}/pasta/${pastaId}/upload-multiplos`,
        formData
      )
      .pipe(catchError(this.tratarErro));
  }

  excluirArquivo(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrlAdminArquivos}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  renomearArquivo(id: number, novoNome: string): Observable<ArquivoAdmin> {
    return this.http
      .put<ArquivoAdmin>(`${this.apiUrlAdminArquivos}/renomear/${id}`, {
        novoNome,
      })
      .pipe(catchError(this.tratarErro));
  }

  copiarArquivo(
    arquivoId: number,
    pastaDestinoId: number
  ): Observable<ArquivoAdmin> {
    const url = `${this.apiUrlAdminArquivos}/${arquivoId}/copiar/${pastaDestinoId}`;
    return this.http
      .post<ArquivoAdmin>(url, null)
      .pipe(catchError(this.tratarErro));
  }

  copiarPasta(
    id: string,
    destinoPastaId?: string | number
  ): Observable<PastaAdmin> {
    let params = new HttpParams();
    if (destinoPastaId !== undefined && destinoPastaId !== null) {
      params = params.set('destinoPastaId', String(destinoPastaId));
    }
    return this.http
      .post<PastaAdmin>(`${this.apiUrlAdminPastas}/${id}/copiar`, null, {
        params,
      })
      .pipe(catchError(this.tratarErro));
  }

  moverPasta(idPasta: number, novaPastaPaiId?: number): Observable<PastaAdmin> {
    const params: any = novaPastaPaiId !== undefined ? { novaPastaPaiId } : {};
    return this.http
      .patch<PastaAdmin>(`${this.apiUrlAdminPastas}/${idPasta}/mover`, null, {
        params,
      })
      .pipe(catchError(this.tratarErro));
  }

  moverArquivo(
    arquivoId: number,
    pastaDestinoId: number
  ): Observable<ArquivoAdmin> {
    return this.http
      .put<ArquivoAdmin>(
        `${this.apiUrlAdminArquivos}/${arquivoId}/mover/${pastaDestinoId}`,
        null
      )
      .pipe(catchError(this.tratarErro));
  }

  atualizarPermissoesAcao(dto: PastaPermissaoAcaoDTO): Observable<string> {
    return this.http
      .post(`${this.apiUrlAdminPastas}/permissao/acao`, dto, {
        responseType: 'text',
      })
      .pipe(catchError(this.tratarErro));
  }

  listarUsuarios(
    username?: string,
    page: number = 0,
    size: number = 10
  ): Observable<Paginacao<Usuario>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (username) {
      params = params.set('username', username);
    }

    return this.http
      .get<Paginacao<Usuario>>(this.apiUrlUsuarios, { params })
      .pipe(catchError(this.tratarErro));
  }

  listarUsuariosPorPasta(pastaId: number): Observable<UsuarioResumoDTO[]> {
    return this.http
      .get<UsuarioResumoDTO[]>(`${this.apiUrlAdminPastas}/${pastaId}/usuarios`)
      .pipe(catchError(this.tratarErro));
  }

  // ---------------- Erro centralizado ----------------
  private tratarErro(error: HttpErrorResponse) {
    console.error('Erro capturado no AdminService:', error);

    const erroBackend: ErrorMessage = {
      status: error.status,
      error: error.error?.error || 'Erro desconhecido',
      message: error.error?.message || 'Erro ao processar requisição',
      path: error.error?.path || '',
      timestamp: error.error?.timestamp || new Date().toISOString(),
    };

    return throwError(() => erroBackend);
  }
}
