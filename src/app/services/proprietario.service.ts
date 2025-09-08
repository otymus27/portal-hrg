import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Proprietario } from '../models/proprietario';
import { Paginacao } from '../models/paginacao';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root', // Torna o serviço disponível em toda a aplicação
})

export class ProprietarioService {
  // Injeção do HttpClient para chamadas HTTP
  http = inject(HttpClient);

  // URL base da API (poderia ser movida para environment.ts)
  private readonly API_URL = environment.apiUrl+'/api/proprietario';

  constructor() {}

  /**
   * Lista proprietários com paginação, ordenação e filtros opcionais.
   * @param page Página atual (default = 0)
   * @param size Tamanho da página (default = 5)
   * @param sortField Campo usado para ordenação (default = 'id')
   * @param sortDirection Direção da ordenação ('asc' ou 'desc')
   * @param nome Filtro por nome (opcional)
   * @param cpf Filtro por CPF (opcional)
   */
  listar(
    page: number = 0,
    size: number = 5,
    sortField: keyof Proprietario = 'id',
    sortDirection: 'asc' | 'desc' = 'asc',
    nome?: string,
    cpf?: string
  ): Observable<Paginacao<Proprietario>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortField', sortField) // campo de ordenação
      .set('sortDir', sortDirection); // direção da ordenação

    // Aplica filtros se existirem
    if (nome) params = params.set('nome', nome);
    if (cpf) params = params.set('cpf', cpf);

    return this.http.get<Paginacao<Proprietario>>(this.API_URL, { params });
  }

  /**
   * Busca um proprietário por ID.
   * @param id ID do proprietário
   */
  buscarPorId(id: number): Observable<Proprietario> {
    return this.http
      .get<Proprietario>(`${this.API_URL}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Cadastra um novo proprietário.
   * @param proprietario Objeto com dados do proprietário (parcial, sem ID)
   */
  cadastrar(proprietario: Partial<Proprietario>): Observable<Proprietario> {
    return this.http.post<Proprietario>(this.API_URL, proprietario, {
      responseType: 'json' as 'json',
    });
  }

  /**
   * Atualiza um proprietário existente.
   * @param proprietario Objeto do proprietário atualizado
   * @param id ID do proprietário
   */
  atualizar(proprietario: Proprietario, id: number): Observable<string> {
    return this.http.patch<string>(`${this.API_URL}/${id}`, proprietario, {
      responseType: 'text' as 'json', // backend retorna texto, não JSON
    });
  }

  /**
   * Exclui um proprietário pelo ID.
   * @param id ID do proprietário
   */
  excluir(id: number): Observable<string> {
    return this.http.delete<string>(`${this.API_URL}/${id}`, {
      responseType: 'text' as 'json',
    });
  }

  /**
   * Tratamento centralizado de erros de requisições HTTP.
   * Loga no console e retorna um erro amigável.
   */
  private tratarErro(error: HttpErrorResponse) {
    console.error('Ocorreu um erro:', error);

    return throwError(
      () => new Error(error.error?.mensagem || 'Erro ao processar requisição')
    );
  }
}
