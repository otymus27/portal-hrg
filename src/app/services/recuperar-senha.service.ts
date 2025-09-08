import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RecuperarSenhaRequest } from '../models/recuperar-senha-request';
import { ResetSenhaRequest } from '../models/reset-senha-request';
import { environment } from '../../environment/environment.prod';

@Injectable({
  providedIn: 'root',
})
export class RecuperarSenhaService {
  private http = inject(HttpClient);
 
  // URL base da API (poderia ser movida para environment.ts)
  private readonly API_URL = environment.apiUrl+'/api/recuperar';

  // Endpoint para o ADMIN
  // ✅ Método que gera uma senha provisória com base no ID
  gerarSenhaProvisoria(id: number): Observable<any> {
    // A requisição POST espera um corpo. O corpo aqui é um DTO simples com o ID.
    // Isso evita o uso de DTOs complexos e mantém o código mais limpo.
    const body = { id };
    return this.http.post(`${this.API_URL}/gerar-senha`, body);
  }

  // Endpoint para o usuário
  atualizarSenha(dto: ResetSenhaRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/redefinir-senha`, dto);
  }
}
