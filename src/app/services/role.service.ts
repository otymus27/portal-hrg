// src/app/services/role.service.ts
import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Role } from '../models/role';
import { Paginacao } from '../models/paginacao';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root', // Torna o serviço disponível em toda a aplicação
})
export class RoleService {
  // Injeção do HttpClient para chamadas HTTP
  http = inject(HttpClient);

  // ✅ URL base da API para roles.
  //    Assumindo que o backend tem um endpoint como /api/roles que retorna List<String>  
  // URL base da API (poderia ser movida para environment.ts)
  private readonly API_URL = environment.apiUrl+'/api/role';

  constructor() {}

  /**
     * Lista proprietários com paginação, ordenação e filtros opcionais.
     * @param page Página atual (default = 0)
     * @param size Tamanho da página (default = 5)
     * @param sortField Campo usado para ordenação (default = 'id')
     * @param sortDirection Direção da ordenação ('asc' ou 'desc')
     
     
     */
  listar(
    page: number = 0,
    size: number = 5,
    sortField: keyof Role = 'id',
    sortDirection: 'asc' | 'desc' = 'asc'
  ): Observable<Paginacao<Role>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortField', sortField) // campo de ordenação
      .set('sortDir', sortDirection); // direção da ordenação

    return this.http.get<Paginacao<Role>>(this.API_URL, { params });
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
