import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Paginacao } from '../models/paginacao';
import { Marca } from '../models/marca';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root', // Torna o serviço disponível em toda a aplicação
})
export class MarcaService {
  // Injeção do HttpClient para chamadas HTTP
  http = inject(HttpClient);

  // URL base da API (poderia ser movida para environment.ts)
  private readonly API_URL = environment.apiUrl+'/api/marca';

  constructor() {}

  /**
   * Lista proprietários com paginação, ordenação e filtros opcionais.
   * @param page Página atual (default = 0)
   * @param size Tamanho da página (default = 5)
   * @param sortField Campo usado para ordenação (default = 'id')
   * @param sortDirection Direção da ordenação ('asc' ou 'desc')
   * @param nome Filtro por nome (opcional)   
   */
  listar(
    nome?: string,
    page: number = 0,
    size: number = 5,
    sortField: keyof Marca = 'id',
    sortDirection: 'asc' | 'desc' = 'asc',    
  ): Observable<Paginacao<Marca>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortField', sortField) // campo de ordenação
      .set('sortDir', sortDirection); // direção da ordenação

    // ✅ A verificação e reatribuição são cruciais
    if (nome) {
      params = params.set('nome', nome);
    }
    
    return this.http.get<Paginacao<Marca>>(`${this.API_URL}`, { params });
        
  }

  /**
   * Busca um proprietário por ID.
   * @param id ID do proprietário
   */
  buscarPorId(id: number): Observable<Marca> {
    return this.http
      .get<Marca>(`${this.API_URL}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Cadastra um novo proprietário.
   * @param marca Objeto com dados do proprietário (parcial, sem ID)
   */
  cadastrar(marca: Partial<Marca>): Observable<Marca> {
    //return this.http.post<Marca>(`${this.API_URL}/api/marca`, marca, {
    return this.http.post<Marca>(`${this.API_URL}`, marca, {
      responseType: 'json' as 'json',
    });
  }

  /**
   * Atualiza um registro existente.
   * @param marca Objeto do proprietário atualizado
   * @param id ID do proprietário
   */
  atualizar(marca: Marca, id: number): Observable<string> {
    return this.http.patch<string>(`${this.API_URL}/${id}`, marca, {
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
