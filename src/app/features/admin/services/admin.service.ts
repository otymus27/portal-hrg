import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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
  caminhoCompleto: string; // Adicionado para resolver o erro
  dataCriacao: Date; // Adicionado para resolver o erro
  criadoPor: any; // Adicionado para resolver o erro (tipo `any` ou tipo `Usuario` se aplicável)
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

// ✅ Interface para o DTO de resumo do usuário, conforme o seu endpoint
export interface UsuarioResumoDTO {
  id: number;
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrlAdminPastas = 'http://localhost:8082/api/pastas';
  private readonly apiUrlAdminArquivos = 'http://localhost:8082/api/arquivos';
  private readonly apiUrlPublica = 'http://localhost:8082/api/publico';
  private readonly apiUrlUsuarios = 'http://10.85.190.202:8082/api/usuario'; // ✅ Adicione a URL base para usuários

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

  listarConteudoPorId(pastaId: number): Observable<ConteudoPasta> {
    return this.http
      .get<PastaAdmin>(`${this.apiUrlAdminPastas}/${pastaId}`)
      .pipe(
        map((pasta) => ({
          pastas: pasta.subPastas || [],
          arquivos: pasta.arquivos || [],
        }))
      );
  }

  downloadArquivo(arquivoId: number): Observable<Blob> {
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
    pastaPaiId?: number;
    usuariosComPermissaoIds: number[];
  }): Observable<PastaAdmin> {
    return this.http.post<PastaAdmin>(this.apiUrlAdminPastas, body);
  }

  excluirPasta(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminPastas}/${id}`);
  }

  renomearPasta(id: number, novoNome: string): Observable<PastaAdmin> {
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

  // --- Upload de um arquivo ---
  uploadArquivo(file: File, pastaId: number): Observable<ArquivoAdmin> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('pastaId', pastaId.toString());
    return this.http.post<ArquivoAdmin>(
      `${this.apiUrlAdminArquivos}/upload`,
      formData
    );
  }

  // --- Upload de vários arquivos ---
  uploadMultiplosArquivos(files: File[], pastaId: number) {
    const formData = new FormData();
    files.forEach((file) => formData.append('arquivos', file));

    return this.http.post(
      `${this.apiUrlAdminArquivos}/pasta/${pastaId}/upload-multiplos`,
      formData
    );
  }

  excluirArquivo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrlAdminArquivos}/arquivos/${id}`);
  }

  renomearArquivo(id: number, novoNome: string): Observable<ArquivoAdmin> {
    return this.http.put<ArquivoAdmin>(
      `${this.apiUrlAdminArquivos}/renomear/${id}`,
      { novoNome }
    );
  }

  // ---------------- RF-020: Copiar arquivo ----------------
  copiarArquivo(
    arquivoId: number,
    pastaDestinoId: number
  ): Observable<ArquivoAdmin> {
    const url = `${this.apiUrlAdminArquivos}/${arquivoId}/copiar/${pastaDestinoId}`;
    return this.http.post<ArquivoAdmin>(url, null);
  }

  /**
   * Copia uma pasta (opcionalmente para uma pasta de destino).
   * O backend espera o destino como request param 'destinoPastaId' (opcional).
   */
  copiarPasta(
    id: string,
    destinoPastaId?: string | number
  ): Observable<PastaAdmin> {
    let params = new HttpParams();
    if (destinoPastaId !== undefined && destinoPastaId !== null) {
      params = params.set('destinoPastaId', String(destinoPastaId));
    }
    // POST sem body (backend apenas usa path + query param)
    return this.http.post<PastaAdmin>(
      `${this.apiUrlAdminPastas}/${id}/copiar`,
      null,
      { params }
    );
  }

  // --- Mover Pasta ---
  moverPasta(idPasta: number, novaPastaPaiId?: number): Observable<PastaAdmin> {
    const params: any = novaPastaPaiId !== undefined ? { novaPastaPaiId } : {};
    return this.http.patch<PastaAdmin>(
      `${this.apiUrlAdminPastas}/${idPasta}/mover`,
      null, // Sem corpo, apenas query param
      { params }
    );
  }

  // RF-019: Mover arquivo
  moverArquivo(
    arquivoId: number,
    pastaDestinoId: number
  ): Observable<ArquivoAdmin> {
    return this.http.put<ArquivoAdmin>(
      `${this.apiUrlAdminArquivos}/${arquivoId}/mover/${pastaDestinoId}`,
      null // PUT sem corpo
    );
  }

  // ✅ Atualizar permissões de pasta
  atualizarPermissoesAcao(dto: PastaPermissaoAcaoDTO): Observable<string> {
    // Adiciona a opção responseType: 'text'
    // para instruir o Angular a não tentar parsear a resposta como JSON.
    return this.http.post(`${this.apiUrlAdminPastas}/permissao/acao`, dto, {
      responseType: 'text',
    });
  }
  // // ---------------- Novo método: listar todos usuários ----------------
  // listarUsuarios(): Observable<Usuario[]> {
  //   return this.http.get<Usuario[]>(this.apiUrlUsuarios);
  // }

  // ---------------- NOVO MÉTODO: listar todos usuários ----------------
  // ✅ Método corrigido para aceitar o parâmetro opcional 'username'
  /**
   * ✅ Método atualizado para aceitar paginação.
   * @param username - Termo de busca opcional.
   * @param page - Índice da página a ser buscada (padrão: 0).
   * @param size - Tamanho da página (padrão: 10).
   */
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

    return this.http.get<Paginacao<Usuario>>(this.apiUrlUsuarios, { params });
  }

  // ✅ LISTAR USUÁRIOS POR PASTA
  listarUsuariosPorPasta(pastaId: number): Observable<UsuarioResumoDTO[]> {
    return this.http.get<UsuarioResumoDTO[]>(
      `${this.apiUrlAdminPastas}/${pastaId}/usuarios`
    );
  }
}
