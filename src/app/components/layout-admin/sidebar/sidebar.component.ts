import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './../sidebar/sidebar.component.html',
  styleUrl: './../sidebar/sidebar.component.scss',
})
export class SidebarComponent {
  private authService = inject(AuthService);

  isCollapsed = false;
  showPastasSubmenu = false;
  showMarcasSubmenu = false;
  showUsuariosSubmenu = false;
  showPermissoesSubmenu = false;

  toggle() {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleSubmenu(submenu: string) {
    if (submenu === 'pastas') {
      this.showPastasSubmenu = !this.showPastasSubmenu;
      this.showMarcasSubmenu = false;
      this.showUsuariosSubmenu = false;
      this.showPermissoesSubmenu = false;
    } else if (submenu === 'marcas') {
      this.showMarcasSubmenu = !this.showMarcasSubmenu;
      this.showPastasSubmenu = false;
      this.showUsuariosSubmenu = false;
      this.showPermissoesSubmenu = false;
    } else if (submenu === 'usuarios') {
      this.showUsuariosSubmenu = !this.showUsuariosSubmenu;
      this.showMarcasSubmenu = false;
      this.showPastasSubmenu = false;
      this.showPermissoesSubmenu = false;
    }
  }

  hideAllSubmenus() {
    this.showPastasSubmenu = false;
    this.showMarcasSubmenu = false;
    this.showUsuariosSubmenu = false;
    this.showPermissoesSubmenu = false;
  }

  // ✅ Método para verificar se o usuário é ADMIN
  isAdmin(): boolean {
    const roles = this.authService.getLoggedInRoles();
    return roles.includes('ADMIN');
  }

  isGerente(): boolean {
    const roles = this.authService.getLoggedInRoles();
    return roles.includes('GERENTE');
  }

  isBasic(): boolean {
    const roles = this.authService.getLoggedInRoles();
    return roles.includes('BASIC');
  }
}
