import { Routes } from '@angular/router';
import { Login } from './components/layout-admin/login/login';
import { Principal } from './components/layout-admin/principal/principal';

import { AuthGuard } from './guards/auth.guard';
import { UsuarioComponent } from './components/usuario/usuario.component';

import { RedefinicaoSenhaComponent } from './components/redefinicao-senha/redefinicao-senha.component';
import { HomeComponentPublico } from './components/layout-publico/home/home.component';

import { HomeComponentAdmin } from './components/layout-admin/home/home.component';
import { ExplorerComponent } from './features/public/pages/explorer/explorer.component';

export const routes: Routes = [
  // A rota principal para a página inicial
  { path: '', redirectTo: 'publico', pathMatch: 'full' },
  { path: 'publico', component: ExplorerComponent },

  // Rotas futuras (admin, login etc.)
  // { path: 'admin', loadComponent: () => import('./features/admin/pages/dashboard/dashboard.component').then(c => c.DashboardComponent) },

  { path: '**', redirectTo: 'publico' },


  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponentPublico },
  // Rota para a lista de pastas de nível superior

  // Rota para navegação dentro das pastas (com ID)
  //{ path: 'protocolos/:id', component: ProtocolosComponent },

  // Rota para a área de gerenciamento autenticada
  // { path: 'gerenciar-arquivos', component: FileManagementComponent }

  // // Redireciona a rota base para a página de login
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Rota para o componente de login
  { path: 'login', component: Login },

  // // ✅ Nova rota de nível superior para o usuário redefinir a senha
  // { path: 'redefinir-senha', component: RedefinicaoSenhaComponent },

  // Rota para o painel de administração (com sidebar, header, etc.)
  {
    path: 'admin',
    component: Principal,
    canActivate: [AuthGuard], // <-- Adicione esta linha
    children: [
      // Rota padrão para o componente 'Início'
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      {
        path: 'home',
        component: HomeComponentAdmin,
        data: { roles: ['ADMIN', 'BASIC', 'GERENTE'] }, // ✅ Todos os perfis podem acessar
      },

      // Rotas com submenus para Marcas

      // Rotas com submenus para Marcas

      // Rotas com submenus para Marcas
      {
        path: 'usuarios',
        children: [
          {
            path: '',
            component: UsuarioComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
          {
            path: 'gerenciar',
            component: UsuarioComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
        ],
      },
    ],
  },

  // Rota wildcard para redirecionar URLs inválidas para a página de login
  { path: '**', redirectTo: 'login' },
];
