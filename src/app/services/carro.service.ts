import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Carro } from '../models/carro';
import { Paginacao } from '../models/paginacao';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root', // Torna o serviço disponível em toda a aplicação
})
export class CarroService {
  http = inject(HttpClient);

  
  // URL base da API (poderia ser movida para environment.ts)
  private readonly API_URL = environment.apiUrl+'/api/carro';

  constructor() {}

  /**
   * Lista proprietários com paginação, ordenação e filtros opcionais.
   * @param page Página atual (default = 0)
   * @param size Tamanho da página (default = 5)
   * @param sortField Campo usado para ordenação (default = 'id')
   * @param sortDirection Direção da ordenação ('asc' ou 'desc')
   * @param modelo Filtro por modelo(opcional)
   * @param marca Filtro por Marca (opcional)
   * @param ano Filtro por Ano (opcional)
   */

  listar(
    page: number = 0,
    size: number = 5,
    sortField: keyof Carro = 'id',
    sortDirection: 'asc' | 'desc' = 'asc',
    modelo?: string,
    marca?: string,
    ano?: number
  ): Observable<Paginacao<Carro>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortField', sortField) // campo de ordenação
      .set('sortDir', sortDirection); // direção da ordenação

    // Aplica filtros se existirem
    if (modelo) params = params.set('modelo', modelo);
    if (marca) params = params.set('marca', marca);
    if (ano) params = params.set('ano', ano);

    return this.http.get<Paginacao<Carro>>(this.API_URL, { params });
  }

  /**
   * Busca um proprietário por ID.
   * @param id ID do proprietário
   */
  buscarPorId(id: number): Observable<Carro> {
    return this.http
      .get<Carro>(`${this.API_URL}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Cadastra um novo carro.
   * @param carro Objeto com dados do carro
   */
  // ✅ ALTERADO: Agora espera uma string como resposta
  cadastrar(carro: Partial<Carro>): Observable<string> {
    return this.http
      .post<string>(this.API_URL, carro, {
        responseType: 'text' as 'json',
      })
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Atualiza um proprietário existente.
   * @param carro Objeto do proprietário atualizado
   * @param id ID do proprietário
   */
  atualizar(carro: Carro, id: number): Observable<string> {
    return this.http.patch<string>(`${this.API_URL}/${id}`, carro, {
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
