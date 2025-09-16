// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { Login } from './components/layout-admin/login/login';
import { Principal } from './components/layout-admin/principal/principal';
import { AuthGuard } from './guards/auth.guard';
import { UsuarioComponent } from './components/usuario/usuario.component';
import { RedefinicaoSenhaComponent } from './components/redefinicao-senha/redefinicao-senha.component';
import { HomeComponentPublico } from './components/layout-publico/home/home.component';
import { ExplorerComponent } from './features/public/pages/explorer/explorer.component';

// Componentes Admin
import { AdminExplorerComponent } from './features/admin/pages/admin-explorer/admin-explorer.component';
import { DashboardComponent } from './features/admin/pages/dashboard/dashboard.component';

export const routes: Routes = [
  // --- Rotas Públicas ---
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponentPublico },

  { path: 'publico', component: ExplorerComponent },
  { path: 'login', component: Login },
  { path: 'redefinir-senha', component: RedefinicaoSenhaComponent },

  // --- Rotas Administrativas (Protegidas) ---
  {
    path: 'admin',
    component: Principal,
    canActivate: [AuthGuard],
    children: [
      // Dashboard é a página inicial da área admin
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        component: DashboardComponent,
        // acessível para qualquer usuário logado
      },

      // Pastas (restrito ao ADMIN)
      {
        path: 'pastas',
        children: [
          {
            path: '',
            component: AdminExplorerComponent,
            data: { roles: ['ADMIN'] },
          },
          {
            path: 'gerenciar',
            component: AdminExplorerComponent,
            data: { roles: ['ADMIN'] },
          },
        ],
      },

      // Usuários (restrito ao ADMIN)
      {
        path: 'usuarios',
        children: [
          {
            path: '',
            component: UsuarioComponent,
            data: { roles: ['ADMIN'] },
          },
          {
            path: 'gerenciar',
            component: UsuarioComponent,
            data: { roles: ['ADMIN'] },
          },
        ],
      },
    ],
  },

  // Rotas inválidas → login
  { path: '**', redirectTo: 'login' },
];
