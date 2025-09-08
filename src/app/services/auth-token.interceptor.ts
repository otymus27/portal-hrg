import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service'; // Ajuste o caminho se necessário

@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(
    request: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    // ✅ Adicionado: Verifica se a URL é para uma rota pública
    const isPublicApi = request.url.includes('/api/publico/');
    const isLoginApi = request.url.includes('/api/login');
    const isApiRequest = request.url.includes('/api/');

    console.log(
      `%c[Interceptor Debug]%c URL: ${request.url}`,
      'color: cyan; font-weight: bold;',
      'color: unset;'
    );
    console.log(
      `%c[Interceptor Debug]%c Token from localStorage: ${
        token ? 'Found' : 'NOT Found'
      }`,
      'color: cyan; font-weight: bold;',
      'color: unset;'
    );
    console.log(
      `%c[Interceptor Debug]%c Is Public API: ${isPublicApi}`,
      'color: cyan; font-weight: bold;',
      'color: unset;'
    );
    console.log(
      `%c[Interceptor Debug]%c Is Login API: ${isLoginApi}`,
      'color: cyan; font-weight: bold;',
      'color: unset;'
    );
    console.log(
      `%c[Interceptor Debug]%c Is API Request: ${isApiRequest}`,
      'color: cyan; font-weight: bold;',
      'color: unset;'
    );

    // Só adiciona o token se:
    // 1. Houver um token.
    // 2. For uma requisição para a API.
    // 3. NÃO for uma rota de login ou uma rota pública.
    if (token && isApiRequest && !isPublicApi && !isLoginApi) {
      const authReq = request.clone({
        headers: request.headers.set('Authorization', `Bearer ${token}`),
      });
      console.log(
        `%c[Interceptor Debug]%c Authorization Header ADDED: ${authReq.headers.get(
          'Authorization'
        )}`,
        'color: green; font-weight: bold;',
        'color: unset;'
      );
      return next.handle(authReq);
    }

    console.log(
      `%c[Interceptor Debug]%c NO Authorization Header added for ${request.url}. Passing original request.`,
      'color: yellow; font-weight: bold;',
      'color: unset;'
    );
    return next.handle(request);
  }
}
