import { Component, EventEmitter, inject, Output } from '@angular/core';
import { Router, RouterModule } from '@angular/router'; // Importe o Router
import { AuthService } from '../../../services/auth.service'; // Importe o AuthService
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterModule, CommonModule], // ✅ Adicione CommonModule aqui
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  router = inject(Router);
  authService = inject(AuthService); // ✅ Injete o AuthService
  @Output() toggleSidebarEvent = new EventEmitter<void>();

  constructor() {}

  // ✅ Variáveis para armazenar o nome de usuário e as roles (usando Observables para reatividade)
  loggedInUsername$ = this.authService.loggedInUsername$;
  loggedInRoles$ = this.authService.loggedInRoles$;

  toggleSidebar() {
    this.toggleSidebarEvent.emit();
  }

  // ✅ Método de logout
  logout() {
    this.authService.logout(); // Chama o método de logout do serviço
    this.router.navigate(['/login']); // Redireciona para a tela de login
  }
}
