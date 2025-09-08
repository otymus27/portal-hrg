// src/app/services/user.service.ts
import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Paginacao } from '../models/paginacao'; // Reutilize seu modelo de Paginação
import { Usuario } from '../models/usuario'; // Importa o novo modelo de Usuario
import { Role } from '../models/role';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root', // Torna o serviço disponível em toda a aplicação
})
export class UsuarioService {
  http = inject(HttpClient);

  // ⚠️ Ajuste o endpoint da sua API de usuários no backend
    // URL base da API (poderia ser movida para environment.ts)
    private readonly API_URL = environment.apiUrl+'/api/usuario';

  constructor() {}

  /**
   * Lista usuários com paginação, ordenação e filtros opcionais.
   * @param page Página atual (default = 0)
   * @param size Tamanho da página (default = 5)
   * @param sortField Campo usado para ordenação (default = 'id')
   * @param sortDirection Direção da ordenação ('asc' ou 'desc')
   * @param username Filtro por nome de usuário (opcional)
   */
  listar(
    page: number = 0,
    size: number = 5,
    sortField: keyof Usuario = 'id', // Usa 'id' ou 'username' como campo padrão
    sortDirection: 'asc' | 'desc' = 'asc',
    username?: string
  ): Observable<Paginacao<Usuario>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortField', sortField.toString())
      .set('sortDir', sortDirection);

    if (username) params = params.set('username', username);

    // O backend NÃO deve retornar a senha em operações GET por segurança.
     return this.http.get<Paginacao<Usuario>>(this.API_URL, { params });
  }

  /**
   * Busca um usuário por ID.
   * @param id ID do usuário
   */
  buscarPorId(id: number): Observable<Usuario> {
    // O backend NÃO deve retornar a senha em operações GET por segurança.
    return this.http
      .get<Usuario>(`${this.API_URL}/${id}`)
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Cadastra um novo usuário.
   * @param usuario Objeto com dados do novo usuário (inclui senha)
   */
  cadastrar(usuario: Partial<Usuario>): Observable<string> {
    return this.http
      .post<string>(this.API_URL, usuario, {
        responseType: 'text' as 'json',
      })
      .pipe(catchError(this.tratarErro));
  }

  /**
   * Atualiza um usuário existente.
   * Usa PATCH para atualizações parciais.
   * @param id ID do usuário
   * @param usuario Objeto com dados do usuário a serem atualizados (senha opcional)
   * @returns Um Observable que emite o objeto Usuario atualizado pelo backend.
   */
  atualizar(id: number, usuario: Partial<Usuario>): Observable<Usuario> { // ✅ Recebe Partial<Usuario> e retorna Observable<Usuario>
     return this.http.patch<Usuario>(`${this.API_URL}/${id}`, usuario, {
      responseType: 'text' as 'json', // backend retorna texto, não JSON
    });// ✅ Espera JSON de retorno, remove responseType
      
  }


  /**
   * Exclui um usuário pelo ID.
   * @param id ID do usuário
   */
  excluir(id: number): Observable<string> {
    // Backend deve retornar uma mensagem de sucesso (String)
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
