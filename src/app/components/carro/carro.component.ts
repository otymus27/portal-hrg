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
import { Carro } from '../../models/carro';
import { CarroService } from '../../services/carro.service';
import { Marca } from '../../models/marca';
import { Proprietario } from '../../models/proprietario';
import { MarcaService } from '../../services/marca.service';
import { ProprietarioService } from '../../services/proprietario.service';
import { Paginacao } from '../../models/paginacao';

@Component({
  selector: 'app-carro',
  standalone: true,
  imports: [CommonModule, FormsModule, MdbFormsModule, MdbModalModule],
  providers: [],
  templateUrl: './carro.component.html',
  styleUrl: './carro.component.scss',
})
export class CarroComponent {
  lista: Carro[] = [];
  registroSelecionado!: Carro;

  marcas: Marca[] = []; // ✅ lista para select
  proprietariosDisponiveis: Proprietario[] = []; // lista geral

  //Variáveis para configuração para paginação
  page = 0;
  size = 5;
  totalPages = 0;
  totalElements = 0;

  // Filtro
  filtroModelo: string = ''; // ✅ Novo filtro para modelo
  filtroMarca: string = ''; // ✅ Novo filtro para marca
  filtroAno: number | undefined; // ✅ Novo filtro para ano

  // Ordenação
  colunaOrdenada: keyof Carro = 'modelo';
  ordem: 'asc' | 'desc' = 'asc';

  carroService = inject(CarroService);
  marcaService = inject(MarcaService);
  proprietarioService = inject(ProprietarioService);
  modalService = inject(MdbModalService);
  // ✅ Injete o novo serviço de toast
  toastService = inject(ToastService);
  // ✅ referência para o modal
  // ✅ o modal será aberto com a referência do template
  @ViewChild('modalCarroDetalhe') modalCarroDetalhe!: TemplateRef<any>;
  modalRef!: MdbModalRef<any>;

  // ✅ Nova referência de template para o modal de confirmação
  @ViewChild('modalConfirmacaoExclusao')
  modalConfirmacaoExclusao!: TemplateRef<any>;

  constructor() {
    this.listar();
    this.carregarMarcas();
    this.carregarProprietarios();
  }

  // ✅ Lógica de listagem com filtros e paginação
  listar() {
    this.carroService
      .listar(
        this.page,
        this.size,
        this.colunaOrdenada,
        this.ordem,
        this.filtroModelo,
        this.filtroMarca,
        this.filtroAno
      )
      .subscribe({
        next: (resposta: Paginacao<Carro>) => {
          this.lista = resposta.content.map((carro) => ({
            ...carro,
            proprietarios: carro.proprietarios || [],
          }));
          this.page = resposta.number;
          this.totalPages = resposta.totalPages;
          this.totalElements = resposta.totalElements;
        },
        error: () => this.toastService.showError('Erro ao listar carros!'),
      });
  }

  // ✅ Métodos de paginação
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
    this.filtroModelo = ''; // limpa campo de filtro
    this.filtroMarca = ''; // limpa campo de filtro
    this.filtroAno = 0; // limpa campo de filtro
  }

  limparFiltros() {
    this.filtroModelo = '';
    this.filtroMarca = '';
    this.filtroAno = undefined;
    this.aplicarFiltros();
  }

  // ✅ Lógica de ordenação da tabela
  ordenarPor(campo: keyof Carro) {
    if (this.colunaOrdenada === campo) {
      this.ordem = this.ordem === 'asc' ? 'desc' : 'asc';
    } else {
      this.colunaOrdenada = campo;
      this.ordem = 'asc';
    }
    this.listar();
  }

  carregarMarcas() {
    this.marcaService.listar().subscribe({
      next: (lista: Paginacao<Marca>) => {
        this.marcas = lista.content;
      },
      error: () => this.toastService.showError('Erro ao carregar marcas!'),
    });
  }

  /**
   * Carrega a lista de proprietários.
   */
  carregarProprietarios() {
    this.proprietarioService.listar().subscribe({
      next: (paginacao: Paginacao<Proprietario>) => {
        this.proprietariosDisponiveis = paginacao.content;
      },
      error: () =>
        this.toastService.showError('Erro ao carregar proprietários!'),
    });
  }

  // ✅ Método para abrir modal de cadastro
  cadastrarModal() {
    this.registroSelecionado = {
      id: 0,
      modelo: '',
      cor: '',
      ano: 0,
      marca: { id: 0, nome: '' },
      proprietarios: [],
    };
    this.modalRef = this.modalService.open(this.modalCarroDetalhe);
  }

  // ✅ Método para abrir modal de edição
  editarModal(carro: Carro) {
    const marcaSelecionada = this.marcas.find(
      (m) => m.id === carro.marca.id
    ) || { id: 0, nome: '' };
    const proprietariosSelecionados = carro.proprietarios.map((p) => {
      return this.proprietariosDisponiveis.find((pr) => pr.id === p.id)!;
    });

    this.registroSelecionado = {
      id: carro.id,
      modelo: carro.modelo || '',
      cor: carro.cor || '',
      ano: carro.ano || 0,
      marca: marcaSelecionada,
      proprietarios: proprietariosSelecionados,
    };

    this.modalRef = this.modalService.open(this.modalCarroDetalhe);
  }

  cancelarModal() {
    this.modalRef.close();
  }

  //Modal de proprietarios que ira abrir dentro do modal de carro
  @ViewChild('modalProprietarios') modalProprietarios!: TemplateRef<any>;
  modalRefProprietarios!: MdbModalRef<any>;

  // Lógica dos modais de proprietários
  abrirModalProprietarios() {
    this.modalRefProprietarios = this.modalService.open(
      this.modalProprietarios
    );
  }

  fecharModalProprietarios() {
    this.modalRefProprietarios.close();
  }

  // ====== Logica para adicionar um ou varios itens - relacionamento n para n ======
  adicionarProprietario(proprietario: Proprietario) {
    if (
      !this.registroSelecionado.proprietarios.find(
        (p) => p.id === proprietario.id
      )
    ) {
      this.registroSelecionado.proprietarios.push(proprietario);
      this.modalRefProprietarios.close();
    }
  }

  // ====== Logica para remover um ou varios itens - relacionamento n para n ======
  removerProprietario(proprietario: Proprietario) {
    this.registroSelecionado.proprietarios =
      this.registroSelecionado.proprietarios.filter(
        (p) => p.id !== proprietario.id
      );
  }
  // =======================================

  // ✅ Método para salvar (cadastrar ou atualizar)
  salvarCarro(carro: Carro) {
    if (
      !carro.modelo?.trim() ||
      !carro.cor?.trim() ||
      !carro.ano ||
      !carro.marca?.id
    ) {
      this.toastService.showError('Preencha todos os campos obrigatórios.');
      return;
    }

    const isNovoRegistro = !carro.id || carro.id <= 0;
    const carroDTO: Partial<Carro> = {
      modelo: carro.modelo,
      marca: carro.marca,
      ano: carro.ano,
      cor: carro.cor,
      proprietarios: carro.proprietarios,
    };

    if (isNovoRegistro) {
      this.carroService.cadastrar(carroDTO).subscribe({
        next: () => {
          console.log('Retorno do backend:', carroDTO);
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
      this.carroService.atualizar(carro, carro.id!).subscribe({
        next: (msg) => {
          console.log('Retorno do backend:', carro);
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

  // ✅ Métodos para o modal de exclusão
  excluir(carro: Carro) {
    this.registroSelecionado = carro;
    this.modalRef = this.modalService.open(this.modalConfirmacaoExclusao);
  }

  excluirConfirmado() {
    this.modalRef.close();
    this.carroService.excluir(this.registroSelecionado.id!).subscribe({
      next: () => {
        this.toastService.showSuccess('Carro excluído com sucesso!');
        this.listar();
      },
      error: () => {
        this.toastService.showError('Erro ao excluir carro!');
      },
    });
  }
}
