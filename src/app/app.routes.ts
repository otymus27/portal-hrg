// src/app/app.routes.ts ou onde suas rotas estão definidas
import { Routes } from '@angular/router';
import { Login } from './components/layout-admin/login/login';
import { Principal } from './components/layout-admin/principal/principal';
import { AuthGuard } from './guards/auth.guard';
import { UsuarioComponent } from './components/usuario/usuario.component';
import { RedefinicaoSenhaComponent } from './components/redefinicao-senha/redefinicao-senha.component';
import { HomeComponentPublico } from './components/layout-publico/home/home.component';
import { HomeComponentAdmin } from './components/layout-admin/home/home.component';
import { ExplorerComponent } from './features/public/pages/explorer/explorer.component';

// Importa o novo componente
import { AdminExplorerComponent } from './features/admin/pages/admin-explorer/admin-explorer.component';

export const routes: Routes = [
  // --- Rotas Públicas ---
  // Rota raiz que redireciona para a página inicial pública
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponentPublico },

  // Rota de exploração pública de arquivos
  { path: 'publico', component: ExplorerComponent },

  // Rota de login
  { path: 'login', component: Login },

  // Rota de redefinição de senha
  { path: 'redefinir-senha', component: RedefinicaoSenhaComponent },

  // --- Rotas Administrativas (Protegidas) ---
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

      // Rotas com submenus para Pastas

      {
        path: 'pastas',
        children: [
          {
            path: '',
            component: AdminExplorerComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
          {
            path: 'gerenciar',
            component: AdminExplorerComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
        ],
      },

      // Rotas com submenus para usuarios
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
