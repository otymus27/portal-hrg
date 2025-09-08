import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    MdbFormsModule,
    MdbRippleModule,
    RouterModule,
  ],
  templateUrl: './../login/login.html',
  styleUrl: './../login/login.scss',
})
export class Login {
  // A propriedade 'credentials' já está em uso, vamos mantê-la
  credentials = {
    username: '',
    password: '',
  };
  errorMessage: string | null = null;
  loading = false; // ✅ Propriedade para controlar o estado de carregamento

  // ✅ Propriedades específicas para erros de validação
  usernameError: string | null = null;
  passwordError: string | null = null;

  private authService = inject(AuthService);
  private router = inject(Router);

  constructor() {}

  onSubmit(): void {
    // ✅ 1. Validação no front-end para campos vazios
    this.errorMessage = null;
    this.usernameError = null;
    this.errorMessage = null; // Limpa a mensagem de erro anterior

    if (!this.credentials.username.trim()) {
      this.usernameError = 'Por favor, digite seu nome de usuário.';
      return;
    }

    if (!this.credentials.password.trim()) {
      this.passwordError = 'Por favor, digite sua senha.';
      return;
    }

    this.loading = true; // Ativa o spinner

    // ✅ 2. Tratamento de erros na resposta do back-end
    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        // A navegação já é tratada dentro do authService
        this.loading = false; // Desativa o spinner
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false; // Desativa o spinner
        if (error.status === 401) {
          this.errorMessage =
            'Credenciais inválidas. Verifique seu usuário e senha.';
        } else if (error.status === 0) {
          // Erro de status 0 geralmente indica problema de CORS,
          // ou que o servidor da API não está online ou acessível.
          this.errorMessage =
            'Não foi possível conectar ao servidor de backend. Por favor, verifique sua conexão ou tente novamente mais tarde.';
        } else {
          this.errorMessage = `Ocorreu um erro: ${error.status} - ${error.message}.`;
        }
        console.error('Erro de login:', error);
      },
    });
  }
}
