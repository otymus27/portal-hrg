// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes'; // Supondo que você tem um arquivo app.routes.ts
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http'; // ✅ Importe 'withInterceptorsFromDi'
import { HTTP_INTERCEPTORS } from '@angular/common/http'; // ✅ Importe HTTP_INTERCEPTORS
import { AuthTokenInterceptor } from './app/services/auth-token.interceptor'; // ✅ Importe seu interceptor
import { AuthService } from './app/services/auth.service'; // ✅ Importe seu AuthService se ele não tiver `providedIn: 'root'`

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes), // Provedor de roteamento

    // ✅ Configuração do HttpClient para usar interceptors do DI
    provideHttpClient(withInterceptorsFromDi()),

    // ✅ Registro do seu AuthTokenInterceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthTokenInterceptor,
      multi: true, // Permite que múltiplos interceptors sejam registrados
    },

    // ✅ Provedor do AuthService (se ele não estiver com `providedIn: 'root'`)
    AuthService,

    // ✅ Opcional: Provedor do AuthGuard
    // AuthGuard // Se seu AuthGuard não estiver com `providedIn: 'root'`
  ],
}).catch((err) => console.error(err));
