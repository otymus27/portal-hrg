import { Routes } from '@angular/router';
import { Login } from './components/layout/login/login';
import { Principal } from './components/layout/principal/principal';
import { MarcaComponent } from './components/marca/marca.component';

import { AuthGuard } from './guards/auth.guard';
import { UsuarioComponent } from './components/usuario/usuario.component';
import { MarcaComponentRelatorio } from './components/relatorio/marca/relatorio.marca.component';
import { RedefinicaoSenhaComponent } from './components/redefinicao-senha/redefinicao-senha.component';
import { HomeComponentPublico } from './components/layout-publico/home/home.component';
import { ProtocolosComponent } from './components/layout-publico/protocolos/protocolos.component';
import { HomeComponent } from './components/layout/home/home.component';
import { PastaComponent } from './components/pasta/pasta.component';

export const routes: Routes = [
  // A rota principal para a página inicial
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponentPublico },
  // Rota para a lista de pastas de nível superior
  { path: 'protocolos', component: ProtocolosComponent },
  { path: 'protocolos/:caminho', component: ProtocolosComponent },

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
        component: HomeComponent,
        data: { roles: ['ADMIN', 'BASIC', 'GERENTE'] }, // ✅ Todos os perfis podem acessar
      },

      // Rotas com submenus para Marcas
      {
        path: 'marcas',
        children: [
          {
            path: 'consulta',
            component: MarcaComponentRelatorio,
            data: { roles: ['ADMIN', 'GERENTE'] }, // ✅ Só admin pode acessar
          },
          {
            path: 'gerenciar',
            component: MarcaComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
        ],
      },

      // Rotas com submenus para Marcas
      {
        path: 'pastas',
        children: [
          {
            path: 'consulta',
            component: PastaComponent,
            data: { roles: ['ADMIN', 'GERENTE'] }, // ✅ Só admin pode acessar
          },
          {
            path: 'gerenciar',
            component: PastaComponent,
            data: { roles: ['ADMIN'] }, // ✅ Só admin pode acessar
          },
        ],
      },

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
