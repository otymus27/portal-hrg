import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
  PastaExcluirDTO,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';
import { Paginacao } from '../../../../models/paginacao';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-admin-explorer',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule],
  templateUrl: './admin-explorer.component.html',
  styleUrls: ['./admin-explorer.component.scss'],
})
export class AdminExplorerComponent implements OnInit {
  pastas: PastaAdmin[] = [];
  arquivos: ArquivoAdmin[] = [];
  breadcrumb: PastaAdmin[] = [];
  loading = false;

  // Modais
  modalCriarPastaAberto = false;
  modalRenomearAberto = false;
  modalUploadAberto = false;
  modalExcluirAberto = false;
  modalExcluirSelecionadosAberto = false;

  // CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  arquivoParaUpload: File | null = null;
  itemParaExcluir: PastaAdmin | ArquivoAdmin | null = null;

  // Usuários
  usuarios: Usuario[] = [];
  usuariosSelecionados: Usuario[] = [];

  // Seleção
  itensSelecionados: (PastaAdmin | ArquivoAdmin)[] = [];

  constructor(
    private adminService: AdminService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.recarregarConteudo();
    this.carregarUsuarios();
  }

  // ---------------- Navegação ----------------
  recarregarConteudo(): void {
    const pastaAtual = this.obterPastaAtual();
    pastaAtual
      ? this.abrirPasta(pastaAtual, false)
      : this.carregarConteudoRaiz();
  }

  carregarConteudoRaiz(): void {
    this.loading = true;
    this.adminService.listarConteudoRaiz().subscribe({
      next: (conteudo: ConteudoPasta) => {
        this.atualizarConteudo(conteudo);
        this.breadcrumb = [];
      },
      error: (err: any) =>
        this.handleError('Erro ao carregar conteúdo raiz', err),
    });
  }

  abrirPasta(pasta: PastaAdmin, adicionarBreadcrumb = true): void {
    if (!pasta) return;
    if (adicionarBreadcrumb) this.breadcrumb.push(pasta);

    this.loading = true;
    this.adminService.listarConteudoPorId(pasta.id).subscribe({
      next: (conteudo: ConteudoPasta) => this.atualizarConteudo(conteudo),
      error: (err: any) => this.handleError('Erro ao abrir pasta', err),
    });
  }

  navegarPara(index: number): void {
    if (index < 0) return this.carregarConteudoRaiz();

    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    this.recarregarConteudo();
  }

  voltarUmNivel(): void {
    this.navegarPara(this.breadcrumb.length - 2);
  }

  private atualizarConteudo(conteudo: ConteudoPasta): void {
    this.pastas = conteudo.pastas;
    this.arquivos = conteudo.arquivos;
    this.loading = false;
    this.itensSelecionados = [];
  }

  private obterPastaAtual(): PastaAdmin | undefined {
    return this.breadcrumb[this.breadcrumb.length - 1];
  }

  // ---------------- CRUD ----------------
  abrirModalCriarPasta(): void {
    this.modalCriarPastaAberto = true;
    this.novoNomePasta = '';
    this.usuariosSelecionados = [];
  }

  fecharModalCriarPasta(): void {
    this.modalCriarPastaAberto = false;
  }

  criarPasta(): void {
    if (!this.novoNomePasta || this.usuariosSelecionados.length === 0) return;

    const body = {
      nome: this.novoNomePasta,
      pastaPaiId: this.obterPastaAtual()?.id,
      usuariosComPermissaoIds: this.usuariosSelecionados.map((u) => u.id),
    };

    this.adminService.criarPasta(body).subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalCriarPasta();
      },
      error: (err: any) => this.handleError('Erro ao criar pasta', err),
    });
  }

  abrirModalRenomear(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaRenomear = item;
    this.novoNomeItem = this.getNomeItem(item);
    this.modalRenomearAberto = true;
  }

  fecharModalRenomear(): void {
    this.modalRenomearAberto = false;
    this.itemParaRenomear = null;
    this.novoNomeItem = '';
  }

  renomearItem(): void {
    if (!this.itemParaRenomear || !this.novoNomeItem) return;

    const request: Observable<any> = this.isPasta(this.itemParaRenomear)
      ? this.adminService.renomearPasta(
          this.itemParaRenomear.id,
          this.novoNomeItem
        )
      : this.adminService.renomearArquivo(
          this.itemParaRenomear.id,
          this.novoNomeItem
        );

    request.subscribe({
      next: () => this.recarregarConteudo(),
      error: (err: any) => this.handleError('Erro ao renomear item', err),
    });

    this.fecharModalRenomear();
  }

  // ---------------- Upload ----------------
  abrirModalUpload(): void {
    this.modalUploadAberto = true;
    this.arquivoParaUpload = null;
  }

  fecharModalUpload(): void {
    this.modalUploadAberto = false;
    this.arquivoParaUpload = null;
  }

  onArquivoSelecionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.arquivoParaUpload = input?.files?.[0] || null;
  }

  uploadArquivo(): void {
    if (!this.arquivoParaUpload) return;
    const pastaAtual = this.obterPastaAtual();
    if (!pastaAtual)
      return this.handleError('Selecione uma pasta antes do upload');

    this.adminService
      .uploadArquivo(this.arquivoParaUpload, pastaAtual.id)
      .subscribe({
        next: () => {
          this.recarregarConteudo();
          this.fecharModalUpload();
        },
        error: (err: any) => this.handleError('Erro no upload', err),
      });
  }

  downloadArquivo(arquivo: ArquivoAdmin): void {
    this.adminService.downloadArquivo(arquivo.id).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = arquivo.nome;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  // ---------------- Seleção ----------------
  isSelecionado(item: PastaAdmin | ArquivoAdmin): boolean {
    return this.itensSelecionados.includes(item);
  }

  toggleSelecao(item: PastaAdmin | ArquivoAdmin): void {
    this.isSelecionado(item)
      ? (this.itensSelecionados = this.itensSelecionados.filter(
          (i) => i !== item
        ))
      : this.itensSelecionados.push(item);
  }

  marcarTodos(): void {
    this.itensSelecionados = this.todosItens;
  }

  desmarcarTodos(): void {
    this.itensSelecionados = [];
  }

  inverterSelecao(): void {
    const todos = this.todosItens;
    this.itensSelecionados = todos.filter((item) => !this.isSelecionado(item));
  }

  // ---------------- Exclusão ----------------
  abrirModalExcluir(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaExcluir = item;
    this.modalExcluirAberto = true;
  }

  fecharModalExcluir(): void {
    this.modalExcluirAberto = false;
    this.itemParaExcluir = null;
  }

  confirmarExclusao(): void {
    if (!this.itemParaExcluir) return;

    const request: Observable<void> = this.isPasta(this.itemParaExcluir)
      ? this.adminService.excluirPasta(this.itemParaExcluir.id)
      : this.adminService.excluirArquivo(this.itemParaExcluir.id);

    this.loading = true;
    request.subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalExcluir();
        this.loading = false;
      },
      error: (err: any) => this.handleError('Erro ao excluir item', err),
    });
  }

  abrirModalExcluirSelecionados(): void {
    if (this.itensSelecionados.length > 0)
      this.modalExcluirSelecionadosAberto = true;
  }

  fecharModalExcluirSelecionados(): void {
    this.modalExcluirSelecionadosAberto = false;
  }

  confirmarExclusaoSelecionados(): void {
    if (this.itensSelecionados.length === 0) return;

    const pastasSelecionadas = this.itensSelecionados.filter(this.isPasta);
    const arquivosSelecionados = this.itensSelecionados.filter(
      (i) => !this.isPasta(i)
    );

    this.loading = true;

    const requests: Observable<any>[] = [];

    // Exclusão de pastas em lote
    if (pastasSelecionadas.length > 0) {
      const dto: PastaExcluirDTO = {
        idsPastas: pastasSelecionadas.map((p) => p.id),
        excluirConteudo: true,
      };
      requests.push(
        this.adminService.excluirPastasEmLote(dto).pipe(
          catchError((err) => {
            this.handleError('Erro ao excluir pastas em lote', err);
            return of(null);
          })
        )
      );
    }

    // Exclusão de arquivos individualmente
    arquivosSelecionados.forEach((arquivo) =>
      requests.push(
        this.adminService.excluirArquivo((arquivo as ArquivoAdmin).id).pipe(
          catchError((err) => {
            this.handleError(`Erro ao excluir arquivo ${(arquivo as ArquivoAdmin).nome}`, err);
            return of(null);
          })
        )
      )
    );

    forkJoin(requests).subscribe({
      next: () => {
        this.itensSelecionados = [];
        this.recarregarConteudo();
        this.fecharModalExcluirSelecionados();
        this.loading = false;
      },
      error: (err: any) =>
        this.handleError('Erro ao excluir itens selecionados', err),
    });
  }

  // ---------------- Usuários ----------------
  carregarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (resposta: Paginacao<Usuario>) =>
        (this.usuarios = resposta.content || []),
      error: (err: any) => this.handleError('Erro ao carregar usuários', err),
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    const idx = this.usuariosSelecionados.findIndex((u) => u.id === usuario.id);
    idx === -1
      ? this.usuariosSelecionados.push(usuario)
      : this.usuariosSelecionados.splice(idx, 1);
  }

  // ---------------- Helpers ----------------
  isFolder(item: PastaAdmin | ArquivoAdmin | null): boolean {
    return !!item && 'subPastas' in item;
  }

  isPasta(item: PastaAdmin | ArquivoAdmin): item is PastaAdmin {
    return (item as PastaAdmin).subPastas !== undefined;
  }

  getNomeItem(item: PastaAdmin | ArquivoAdmin | null): string {
    return item ? (this.isPasta(item) ? item.nomePasta : item.nome) : '';
  }

  get todosItens(): (PastaAdmin | ArquivoAdmin)[] {
    return [...this.pastas, ...this.arquivos];
  }

  private handleError(msg: string, err?: any): void {
    console.error(msg, err);
    this.loading = false;
  }
}
