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

import { NgxMaskDirective, NgxMaskPipe, provideNgxMask } from 'ngx-mask';

import { Proprietario } from '../../models/proprietario';
import { ProprietarioService } from '../../services/proprietario.service';
import { CpfMaskPipe } from '../../pipes/cpf-mask.pipe';
import { TelefoneMaskPipe } from '../../pipes/telefone-mask.pipe';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-proprietario',
  imports: [
    CommonModule,
    FormsModule, // necessário para [(ngModel)]
    MdbFormsModule,
    MdbModalModule,
    CpfMaskPipe, // importar pipe
    TelefoneMaskPipe,
    NgxMaskDirective, // precisa importar
  ],
  providers: [provideNgxMask()], // habilita
  templateUrl: './proprietario.component.html',
  styleUrl: './proprietario.component.scss',
})
export class ProprietarioComponent {
  lista: Proprietario[] = [];
  registroSelecionado!: Proprietario;

  //Variáveis para configuração para paginação
  page = 0;
  size = 5;
  totalPages = 0;
  totalElements = 0;

  // Filtro
  filtro: string = '';

  // Ordenação
  colunaOrdenada: keyof Proprietario = 'nome';
  ordem: 'asc' | 'desc' = 'asc';

  proprietarioService = inject(ProprietarioService);
  modalService = inject(MdbModalService);
  // ✅ Injete o novo serviço de toast
  toastService = inject(ToastService);

  @ViewChild('modalProprietarioDetalhe')
  modalProprietarioDetalhe!: TemplateRef<any>;

  // ✅ Nova referência de template para o modal de confirmação
  @ViewChild('modalConfirmacaoExclusao')
  modalConfirmacaoExclusao!: TemplateRef<any>;

  modalRef!: MdbModalRef<any>;

  // ✅ Adicione esta propriedade
  cpfError: string | null = null;
  // ✅ Adicione uma propriedade para mensagem geral
  errorMessage: string | null = null;

  constructor() {
    this.listar();
  }

  listar() {
    // Detecta se o filtro é CPF ou nome
    let filtroNome: string | undefined;
    let filtroCpf: string | undefined;

    const rawFiltro = this.filtro?.trim();
    if (rawFiltro) {
      const cpfRegex = /^\d{3}\.?\d{3}\.?\d{3}-?\d{2}$/; // padrão CPF
      if (cpfRegex.test(rawFiltro)) {
        filtroCpf = rawFiltro.replace(/\D/g, ''); // remove máscara
      } else {
        filtroNome = rawFiltro;
      }
    }

    this.proprietarioService
      .listar(
        this.page,
        this.size,
        this.colunaOrdenada,
        this.ordem,
        filtroNome,
        filtroCpf
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

  aplicarFiltro() {
    this.page = 0; // sempre volta para primeira página
    this.listar();
    this.filtro = ''; // limpa campo de filtro
  }

  ordenarPor(campo: keyof Proprietario) {
    if (this.colunaOrdenada === campo) {
      this.ordem = this.ordem === 'asc' ? 'desc' : 'asc';
    } else {
      this.colunaOrdenada = campo;
      this.ordem = 'asc';
    }
    this.listar();
  }

  cadastrarModal() {
    this.registroSelecionado = { id: 0, nome: '', cpf: '', telefone: '' };
    this.modalRef = this.modalService.open(this.modalProprietarioDetalhe);
  }

  editarModal(proprietario: Proprietario) {
    this.registroSelecionado = { ...proprietario };
    this.modalRef = this.modalService.open(this.modalProprietarioDetalhe);
  }

  cancelarModal() {
    this.modalRef.close();
  }

  salvarProprietario(proprietario: Proprietario) {
    // ✅ Limpa as mensagens de erro antes de cada tentativa
    this.cpfError = null;
    this.errorMessage = null;
    // Validação front-end
    if (
      !proprietario.nome?.trim() ||
      !proprietario.cpf?.trim() ||
      !proprietario.telefone?.trim()
    ) {
      return;
    }

    // Esta linha é so para debugar
    console.log('Registro a salvar:', proprietario);

    // Remove máscara antes de enviar
    const p = { ...proprietario };
    p.cpf = p.cpf.replace(/\D/g, '');
    p.telefone = p.telefone ? p.telefone.replace(/\D/g, '') : '';

    const isNovoRegistro = !proprietario.id || proprietario.id <= 0;

    if (isNovoRegistro) {
      const novoRegistro: Partial<Proprietario> = {
        nome: proprietario.nome,
        cpf: p.cpf,
        telefone: p.telefone,
      };

      this.proprietarioService.cadastrar(novoRegistro).subscribe({
        next: () => {
          console.log('Retorno do backend:', novoRegistro);
          // ✅ Use o serviço de toast
          this.toastService.showSuccess('Registro cadastrado com sucesso!');
          this.listar();
          this.modalRef.close();
        },
        error: (err: HttpErrorResponse) => {
          // ✅ Lógica de tratamento de cpf duplicado
          if (err.status === 400 && err.error === 'CPF já cadastrado!') {
            this.cpfError =
              'Este CPF já está cadastrado. Por favor, use outro CPF.';
            this.toastService.showError(this.cpfError);
          } else {
            this.errorMessage = `Erro ao salvar: ${
              err.error || 'Erro desconhecido.'
            }`;
            this.toastService.showError(this.errorMessage);
          }
          console.error('Erro de cadastro:', err);
        },
      });
    } else {
      this.proprietarioService
        .atualizar(proprietario, proprietario.id!)
        .subscribe({
          next: (msg) => {
            console.log('Retorno do backend:', proprietario);
            // ✅ Use o serviço de toast
            this.toastService.showSuccess('Registro atualizado com sucesso!');
            this.listar();
            this.modalRef.close();
          },
          error: (err: HttpErrorResponse) => {
            // ✅ Tratamento de erro específico para CPF duplicado na atualização (se aplicável)
            if (err.status === 400 && err.error === 'CPF já cadastrado!') {
              this.cpfError = 'Este CPF já está cadastrado. Por favor, use outro CPF.';
              this.toastService.showError(this.cpfError);
            } else {
              this.errorMessage = `Erro ao atualizar: ${err.error || 'Erro desconhecido.'}`;
              this.toastService.showError(this.errorMessage);
            }
            console.error('Erro de atualização:', err);
          },
        });
    }
  }

  // ✅ Método que abre o modal de confirmação
  excluir(proprietario: Proprietario) {
    this.registroSelecionado = proprietario;
    this.modalRef = this.modalService.open(this.modalConfirmacaoExclusao);
  }

  // ✅ Novo método chamado pelo modal
  excluirConfirmado() {
    // Primeiro, fecha o modal de confirmação
    this.modalRef.close();

    // Em seguida, chama o serviço de exclusão
    this.proprietarioService.excluir(this.registroSelecionado.id!).subscribe({
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
}
