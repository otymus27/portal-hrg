import { CommonModule } from '@angular/common';
import { Component, inject, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import {
  MdbModalModule,
  MdbModalRef,
  MdbModalService,
} from 'mdb-angular-ui-kit/modal';

// ✅ Importe o serviço de Toast
import { ToastService } from '../../services/toast.service';
import { Marca } from '../../models/marca';
import { MarcaService } from '../../services/marca.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-marca',
  imports: [
    MdbModalModule,
    CommonModule,
    FormsModule, // ✅ necessário para [(ngModel)]
    MdbFormsModule, // ✅ necessário para mdbInput
  ],
  templateUrl: './marca.component.html',
  styleUrl: './marca.component.scss',
})
export class MarcaComponent {
  private authService = inject(AuthService);

  lista: Marca[] = [];
  registroSelecionado!: Marca;
  //marcaSelecionada: Marca = { id: 0, nome: '' };

  //Variáveis para configuração para paginação
  page = 0;
  size = 5;
  totalPages = 0;
  totalElements = 0;

  // Filtro
  filtro: string = '';

  // Ordenação
  colunaOrdenada: keyof Marca = 'nome';
  ordem: 'asc' | 'desc' = 'asc';

  marcaService = inject(MarcaService);
  modalService = inject(MdbModalService);
  // ✅ Injete o novo serviço de toast
  toastService = inject(ToastService);
  @ViewChild('modalMarcaDetalhe') modalMarcaDetalhe!: TemplateRef<any>;
  // ✅ Nova referência de template para o modal de confirmação
  @ViewChild('modalConfirmacaoExclusao')
  modalConfirmacaoExclusao!: TemplateRef<any>;
  modalRef!: MdbModalRef<any>;

  constructor() {
    this.listar();
  }

  listar() {
    this.marcaService
      .listar(
        this.filtro,
        this.page,
        this.size,
        this.colunaOrdenada,
        this.ordem
      )
      .subscribe({
        next: (resposta) => {
          this.lista = resposta.content;
          this.page = resposta.number;
          this.totalPages = resposta.totalPages;
          this.totalElements = resposta.totalElements;
        },
        // ✅ Use o serviço de toast
        error: () => this.toastService.showError('Erro ao listar registros!'),
      });
  }

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

  // Filtro
  aplicarFiltro() {
    this.page = 0; // sempre volta para primeira página
    this.listar();
    this.filtro = ''; // limpa campo de filtro
  }

  // Ordenação
  ordenarPor(campo: keyof Marca) {
    if (this.colunaOrdenada === campo) {
      this.ordem = this.ordem === 'asc' ? 'desc' : 'asc';
    } else {
      this.colunaOrdenada = campo;
      this.ordem = 'asc';
    }
    // agora passa os parâmetros corretos
    this.listar();
  }

  CadastrarModal() {
    this.registroSelecionado = { id: 0, nome: '' };
    this.modalRef = this.modalService.open(this.modalMarcaDetalhe);
  }

  editarModal(marca: Marca) {
    this.registroSelecionado = { ...marca };
    this.modalRef = this.modalService.open(this.modalMarcaDetalhe);
  }

  cancelarModal() {
    this.modalRef.close();
  }

  salvarMarca(marca: Marca) {
    // Validação front-end
    if (!marca.nome?.trim()) {
      return;
    }

    // Esta linha é so para debugar
    console.log('Registro a salvar:', marca);

    const isNovoRegistro = !marca.id || marca.id <= 0;

    if (isNovoRegistro) {
      // Cria registro sem id
      const novoRegistro: Partial<Marca> = { nome: marca.nome };
      this.marcaService.cadastrar(novoRegistro).subscribe({
        next: (res: Marca) => {
          console.log('Retorno do backend:', novoRegistro);
          // ✅ Use o serviço de toast
          this.toastService.showSuccess('Registro cadastrado com sucesso!');
          this.listar();
          this.modalRef.close();
        },
        error: (err) => {
          // ✅ Use o serviço de toast
          this.toastService.showError(
            `Erro: ${err.error?.mensagem || 'Erro desconhecido.'}`
          );
        },
      });
    } else {
      // Atualiza marca existente - envia Marca completo
      this.marcaService.atualizar(marca, marca.id!).subscribe({
        next: (msg) => {
          console.log('Retorno do backend:', marca);
          // ✅ Use o serviço de toast
          this.toastService.showSuccess('Registro atualizado com sucesso!');
          this.listar();
          this.modalRef.close();
        },
        error: (err) => {
          // ✅ Use o serviço de toast
          this.toastService.showError(
            `Erro: ${err.error?.mensagem || 'Erro desconhecido.'}`
          );
        },
      });
    }
  }

  // ✅ Método que abre o modal de confirmação
  excluir(marca: Marca) {
    this.registroSelecionado = marca;
    this.modalRef = this.modalService.open(this.modalConfirmacaoExclusao);
  }

  // ✅ Novo método chamado pelo modal
  excluirConfirmado() {
    // Primeiro, fecha o modal de confirmação
    this.modalRef.close();

    // Em seguida, chama o serviço de exclusão
    this.marcaService.excluir(this.registroSelecionado.id!).subscribe({
      next: () => {
        // ✅ Use o serviço de toast
        this.toastService.showSuccess('Registro excluído com sucesso!');
        this.listar();
      },
      error: () => {
        // ✅ Use o serviço de toast
        this.toastService.showError('Erro ao excluir proprietário!');
      },
    });
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
}
