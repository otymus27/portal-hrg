import { CommonModule } from '@angular/common';
import { Component, inject, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';

import {
  MdbModalModule,
  MdbModalRef,
  MdbModalService,
} from 'mdb-angular-ui-kit/modal';
import { ToastService } from '../../services/toast.service'; // Serviço de Toast
import { Paginacao } from '../../models/paginacao'; // Modelo de Paginação
import { RoleService } from '../../services/role.service';
// ✅ Novos imports para o gerenciamento de usuários
import { Usuario } from '../../models/usuario';
import { ErrorMessage, UsuarioService } from '../../services/usuario.service';
import { Role } from '../../models/role';
import { RecuperarSenhaService } from '../../services/recuperar-senha.service';
import { ModalConfirmacaoComponent } from '../modal/confirmacao/confirmacao.component';
import { InformacaoComponent } from '../modal/informacao/informacao.component';

@Component({
  selector: 'app-user', // ✅ Seletor atualizado para 'app-user'
  standalone: true,
  imports: [CommonModule, FormsModule, MdbFormsModule, MdbModalModule],
  providers: [],
  templateUrl: './usuario.component.html', // ✅ Template HTML para o UserComponent
  styleUrl: './usuario.component.scss', // ✅ Arquivo de estilo para o UserComponent
})
export class UsuarioComponent {
  lista: Usuario[] = []; // ✅ Lista de usuários
  registroSelecionado!: Usuario; // ✅ Usuário selecionado para edição/exclusão

  // ✅ Lista de todas as roles disponíveis no sistema para seleção
  //rolesDisponiveis: string[] = ['ROLE_ADMIN', 'ROLE_USER', 'ROLE_GERENTE']; // ⚠️ Ajuste conforme as roles do seu backend
  rolesDisponiveis: Role[] = [];

  // Variáveis para configuração de paginação
  page = 0;
  size = 5;
  totalPages = 0;
  totalElements = 0;

  // Filtro
  filtroUsername: string = ''; // ✅ Novo filtro por username

  // Ordenação
  colunaOrdenada: keyof Usuario = 'username'; // ✅ Campo padrão para ordenação
  ordem: 'asc' | 'desc' = 'asc';

  // ✅ Injeção do UserService (substitui CarroService, MarcaService, ProprietarioService)
  usuarioService = inject(UsuarioService);
  roleService = inject(RoleService);
  modalService = inject(MdbModalService);
  toastService = inject(ToastService);

  // ✅ Injetando o serviço de recuperação de senha
  private recuperarSenhaService = inject(RecuperarSenhaService);

  @ViewChild('modalUserDetalhe') modalUserDetalhe!: TemplateRef<any>; // ✅ Referência para o modal de detalhes do usuário
  modalRef!: MdbModalRef<any>;

  @ViewChild('modalConfirmacaoExclusao')
  modalConfirmacaoExclusao!: TemplateRef<any>; // ✅ Referência para o modal de confirmação de exclusão

  constructor() {
    this.listar(); // ✅ Chama listar() na inicialização
    this.carregarRoles(); // Chama a lista de roles
  }

  // ✅ Lógica de listagem com filtros e paginação para usuários
  listar() {
    this.usuarioService
      .listar(
        this.page,
        this.size,
        this.colunaOrdenada,
        this.ordem,
        this.filtroUsername
      )
      .subscribe({
        next: (resposta: Paginacao<Usuario>) => {
          this.lista = resposta.content;
          this.page = resposta.number;
          this.totalPages = resposta.totalPages;
          this.totalElements = resposta.totalElements;
        },
        error: () => this.toastService.showError('Erro ao listar usuários!'),
      });
  }

  // ✅ Métodos de paginação (mantidos do CarroComponent)
  irParaPagina(p: number) {
    this.page = p;
    this.listar();
  }

  proximaPagina() {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.listar();
    }
  }

  paginaAnterior() {
    if (this.page > 0) {
      this.page--;
      this.listar();
    }
  }

  // ✅ Método para aplicar filtros
  aplicarFiltros() {
    this.page = 0; // Sempre volta para a primeira página ao aplicar o filtro
    this.listar();
    this.filtroUsername = ''; // Limpa campo de busca
  }

  // ✅ Método para limpar filtros
  limparFiltros() {
    this.filtroUsername = '';
    this.aplicarFiltros();
  }

  // ✅ Lógica de ordenação da tabela
  ordenarPor(campo: keyof Usuario) {
    if (this.colunaOrdenada === campo) {
      this.ordem = this.ordem === 'asc' ? 'desc' : 'asc';
    } else {
      this.colunaOrdenada = campo;
      this.ordem = 'asc';
    }
    this.listar();
  }

  // ✅ Novo método para carregar a lista de roles disponíveis do backend
  carregarRoles() {
    this.roleService.listar().subscribe({
      next: (paginacao: Paginacao<Role>) => {
        this.rolesDisponiveis = paginacao.content;
      },
      error: () => this.toastService.showError('Erro ao carregar roles!'),
    });
  }

  // ✅ Método para abrir modal de cadastro
  cadastrarModal() {
    this.registroSelecionado = {
      id: 0,
      username: '',
      password: '',
      roles: [], // Inicializa com um array vazio de objetos Role
    };
    this.editingPassword = true; // ✅ Senha sempre editável para novo registro
    this.modalRef = this.modalService.open(this.modalUserDetalhe);
  }

  // ✅ Nova propriedade para controlar a edição da senha (reintroduzido)
  editingPassword = false;

  // ✅ Método para abrir modal de edição de usuário
  editarModal(usuario: Usuario) {
    // Ao editar, o backend NÃO deve retornar a senha.
    // Por isso, não definimos 'password' aqui. A senha será atualizada separadamente se o usuário desejar.
    // Mapeia as roles do usuário para objetos Role completos
    const rolesSelecionados: Role[] = usuario.roles.map((r: Role) => {
      // Encontra a role correspondente na lista de roles disponíveis
      return (
        this.rolesDisponiveis.find((pr) => pr.id === r.id) || {
          id: r.id,
          nome: r.nome || '',
        }
      );
    });

    this.registroSelecionado = {
      id: usuario.id,
      username: usuario.username,
      roles: rolesSelecionados, // Atribui as roles completas
      password: '', // ✅ Importante: Limpar a senha ao editar para não enviar vazia acidentalmente
    };
    this.editingPassword = false; // ✅ Senha desabilitada por padrão na edição
    this.modalRef = this.modalService.open(this.modalUserDetalhe);
  }

  cancelarModal() {
    this.modalRef.close();
  }

  // ✅ NOVO MÉTODO: Habilita a edição da senha (reintroduzido)
  habilitarEdicaoSenha() {
    this.editingPassword = true;
    this.registroSelecionado.password = ''; // Limpa o campo para o usuário digitar a nova senha
  }

  //Modal de roles que ira abrir dentro do modal de usuarios
  @ViewChild('modalRoles') modalRoles!: TemplateRef<any>;
  modalRefRoles!: MdbModalRef<any>;

  // Lógica dos modais de roles
  abrirModalRoles() {
    this.modalRefRoles = this.modalService.open(this.modalRoles);
  }

  fecharModalRoles() {
    this.modalRefRoles.close();
  }

  // ====== Logica para adicionar um ou varios itens - relacionamento n para n ======
  adicionarRole(role: Role) {
    if (!this.registroSelecionado.roles.find((r) => r.id === role.id)) {
      this.registroSelecionado.roles.push(role);
      this.modalRefRoles.close();
    }
  }

  // ✅ Lógica para remover uma role do usuário selecionado
  removerRole(role: Role) {
    this.registroSelecionado.roles = this.registroSelecionado.roles.filter(
      (r) => r !== role
    );
  }

  // ✅ Método para salvar (cadastrar ou atualizar)
  salvarUsuario(usuario: Usuario) {
    if (!usuario.username?.trim()) {
      this.toastService.showError('Preencha todos os campos obrigatórios.');
      return;
    }

    const isNovoRegistro = !usuario.id || usuario.id <= 0;
    const usuarioDTO: Partial<Usuario> = {
      username: usuario.username,
      password: usuario.password,
      roles: usuario.roles,
    };

    if (isNovoRegistro) {
      // Para cadastro, 'password' é obrigatório no UserCreateDTO
      if (!usuario.password || usuario.password.trim() === '') {
        this.toastService.showError(
          'A senha é obrigatória para novos usuários.'
        );
        return;
      }
      this.usuarioService.cadastrar(usuarioDTO).subscribe({
        next: (msg) => {
          // ✅ Debug para retorno do backend
          console.log('Retorno do backend:', usuarioDTO);
          // ✅ Log de debug para o objeto enviado no cadastro
          console.log(
            '%c[Debug - Cadastro]%c Objeto enviado para o backend:',
            'color: lightblue; font-weight: bold;',
            'color: unset;',
            isNovoRegistro
          );

          this.toastService.showSuccess(msg || 'Usuário cadastrado com sucesso!');
          this.listar();
          this.modalRef.close();
        },
        error: (err: ErrorMessage) => {
          this.toastService.showError(
            `Erro (${err.status} - ${err.error}): ${err.message}`
          );
        },
      });
    } else {
      // ✅ Para atualização: Começamos com um objeto parcial que NÃO tem a senha por padrão
      const isAtualizarRegistro: Partial<Usuario> = {
        username: usuario.username,
        roles: this.rolesDisponiveis as Role[], // Envia roles como objetos Role
      };

      // ✅ Apenas inclui a senha no DTO se a edição de senha foi habilitada E o campo não estiver vazio
      if (
        this.editingPassword &&
        usuario.password &&
        usuario.password.trim() !== ''
      ) {
        isAtualizarRegistro.password = usuario.password;
      }

      // ✅ Log de debug para o objeto enviado na atualização
      console.log(
        '%c[Debug - Atualização]%c Objeto enviado para o backend:',
        'color: lightgreen; font-weight: bold;',
        'color: unset;',
        isAtualizarRegistro
      );

      // ✅ Chamada ao serviço com o objeto parcial
      this.usuarioService.atualizar(usuario.id!, usuario).subscribe({
        next: () => {
          this.toastService.showSuccess('Usuário atualizado com sucesso!');
          this.listar();
          this.modalRef.close();
        },
        error: (err: ErrorMessage) => {
          this.toastService.showError(
            `Erro (${err.status} - ${err.error}): ${err.message}`
          );
        },
      });
    }
  }

  // ✅ Métodos para o modal de exclusão
  excluir(user: Usuario) {
    this.registroSelecionado = user;
    this.modalRef = this.modalService.open(this.modalConfirmacaoExclusao);
  }

  excluirConfirmado() {
    this.modalRef.close();
    this.usuarioService.excluir(this.registroSelecionado.id!).subscribe({
      next: () => {
        this.toastService.showSuccess('Usuário excluído com sucesso!');
        this.listar();
      },
      error: (err: ErrorMessage) => {
        this.toastService.showError(
          `Erro (${err.status} - ${err.error}): ${err.message}`
        );
      },
    });
  }

  mensagem: string | null = null;

  resetarSenha(id: number): void {
    // ✅ Passo 1: Abrir o modal de confirmação do MDB
    this.modalService
      .open(ModalConfirmacaoComponent)
      .onClose.subscribe((result: boolean) => {
        // ✅ Passo 2: Lidar com a resposta do modal
        if (result) {
          // O usuário confirmou. Faça a chamada para o serviço.
          this.recuperarSenhaService.gerarSenhaProvisoria(id).subscribe({
            next: (response) => {
              // ✅ 2. Aqui armazenamos o resultado vindo do backend
              const senhaGerada = response.mensagem;
              // ✅ 3. Abre o modal de informação para exibir a senha
              this.modalService.open(InformacaoComponent, {
                data: {
                  password: senhaGerada, // Passa a senha como 'data' para o modal
                },
              });
            },
            error: (err) => {
              this.mensagem = err.error.message;
            },
          });
        }
      });
  }
}
