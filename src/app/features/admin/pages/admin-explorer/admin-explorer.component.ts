import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
  PastaExcluirDTO,
  UsuarioResumoDTO,
  PastaPermissaoAcaoDTO,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Paginacao } from '../../../../models/paginacao';
import { ModalUsuarioComponent } from '../modal-usuario/modal-usuario.component';

@Component({
  selector: 'app-admin-explorer',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule, ModalUsuarioComponent],
  templateUrl: './admin-explorer.component.html',
  styleUrls: ['./admin-explorer.component.scss'],
})
export class AdminExplorerComponent implements OnInit {
  // ---------------- Propriedades ----------------
  // Conteúdo
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
  modalPermissaoAberto = false;

  // CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  itemParaExcluir: PastaAdmin | ArquivoAdmin | null = null;

  // Usuários
  usuarios: Usuario[] = [];
  usuariosSelecionados: Usuario[] = [];

  // Permissões
  usuariosComPermissao: UsuarioResumoDTO[] = [];
  usuariosDisponiveis: Usuario[] = [];
  private usuariosIniciaisComPermissao: UsuarioResumoDTO[] = [];
  usuariosComPermissaoIds: number[] = [];

  // Pasta para permissões
  pastaParaPermissao: PastaAdmin | null = null;

  // Seleção
  itensSelecionados: (PastaAdmin | ArquivoAdmin)[] = [];

  // Upload múltiplo
  arquivosParaUpload: File[] = [];

  modalSelecionarUsuarioAberto = false;

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

  public obterPastaAtual(): PastaAdmin | undefined {
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
  }

  fecharModalUpload(): void {
    this.modalUploadAberto = false;
    this.arquivosParaUpload = [];
  }

  onArquivosSelecionados(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input?.files) {
      const novosArquivos = Array.from(input.files).filter(
        (novo) =>
          !this.arquivosParaUpload.some(
            (existente) =>
              existente.name === novo.name && existente.size === novo.size
          )
      );
      this.arquivosParaUpload.push(...novosArquivos);
    }
  }

  removerArquivoSelecionado(file: File): void {
    this.arquivosParaUpload = this.arquivosParaUpload.filter((f) => f !== file);
  }

  uploadArquivos(): void {
    const pastaAtual = this.obterPastaAtual();
    if (!pastaAtual)
      return this.handleError('Selecione uma pasta antes do upload');
    if (!this.arquivosParaUpload.length) return;

    this.loading = true;
    this.adminService
      .uploadMultiplosArquivos(this.arquivosParaUpload, pastaAtual.id)
      .subscribe({
        next: () => {
          this.recarregarConteudo();
          this.fecharModalUpload();
          this.loading = false;
          this.arquivosParaUpload = [];
        },
        error: (err) =>
          this.handleError('Erro ao fazer upload dos arquivos', err),
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

  get todosItens(): (PastaAdmin | ArquivoAdmin)[] {
    return [...this.pastas, ...this.arquivos];
  }

  marcarTodos(): void {
    this.itensSelecionados = this.todosItens;
  }

  desmarcarTodos(): void {
    this.itensSelecionados = [];
  }

  inverterSelecao(): void {
    this.itensSelecionados = this.todosItens.filter(
      (item) => !this.isSelecionado(item)
    );
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
    if (!this.itensSelecionados.length) return;

    const pastasSelecionadas = this.itensSelecionados.filter(this.isPasta);
    const arquivosSelecionados = this.itensSelecionados.filter(
      (i) => !this.isPasta(i)
    );

    this.loading = true;
    const requests: Observable<any>[] = [];

    if (pastasSelecionadas.length) {
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

    arquivosSelecionados.forEach((arquivo) =>
      requests.push(
        this.adminService.excluirArquivo((arquivo as ArquivoAdmin).id).pipe(
          catchError((err) => {
            this.handleError(
              `Erro ao excluir arquivo ${(arquivo as ArquivoAdmin).nome}`,
              err
            );
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
    this.adminService.listarUsuarios().subscribe({
      next: (resposta: Paginacao<Usuario>) => {
        this.usuarios = resposta.content || [];
      },
      error: (err: any) => {
        this.handleError('Erro ao carregar usuários', err);
      },
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    const idx = this.usuariosSelecionados.findIndex((u) => u.id === usuario.id);
    idx === -1
      ? this.usuariosSelecionados.push(usuario)
      : this.usuariosSelecionados.splice(idx, 1);
  }

  public isUsuarioSelecionado(usuario: Usuario): boolean {
    return this.usuariosSelecionados.some((u) => u.id === usuario.id);
  }

  // ---------------- Permissões ----------------
  abrirModalSelecionarUsuario(): void {
    this.modalSelecionarUsuarioAberto = true;
  }

  onUsuarioSelecionado(usuario: Usuario | null): void {
    if (usuario) {
      this.adicionarUsuarioPermissao(usuario);
    }
    this.modalSelecionarUsuarioAberto = false;
  }

  abrirModalPermissao(pasta: PastaAdmin): void {
    this.pastaParaPermissao = pasta;
    this.loading = true;
  
    this.adminService.listarUsuariosPorPasta(pasta.id).subscribe({
      // ✅ Corrigido: adicionado o tipo `UsuarioResumoDTO[]` ao parâmetro
      next: (usuariosPermitidos: UsuarioResumoDTO[]) => {
        this.usuariosComPermissao = usuariosPermitidos;
        this.usuariosIniciaisComPermissao = [...usuariosPermitidos];
        this.atualizarListaDeDisponiveis();
        this.modalPermissaoAberto = true;
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Erro ao carregar usuários da pasta', err);
        this.loading = false;
      },
    });
  }

  adicionarUsuarioPermissao(usuario: Usuario): void {
    this.usuariosComPermissao.push({
      id: usuario.id,
      username: usuario.username,
    });
    this.atualizarListaDeDisponiveis();
  }

  removerUsuarioPermissao(usuario: UsuarioResumoDTO): void {
    this.usuariosComPermissao = this.usuariosComPermissao.filter(
      (u) => u.id !== usuario.id
    );
    this.atualizarListaDeDisponiveis();
  }

  private atualizarListaDeDisponiveis(): void {
    this.usuariosDisponiveis = this.usuarios.filter(
      (u) => !this.usuariosComPermissao.some((p) => p.id === u.id)
    );
    this.usuariosComPermissaoIds = this.usuariosComPermissao.map((u) => u.id);
  }

  atualizarPermissoes(): void {
    if (!this.pastaParaPermissao || !this.pastaParaPermissao.id) {
      this.handleError('ID da pasta para permissões não encontrado.');
      return;
    }

    const idsAtuais = new Set(this.usuariosComPermissao.map((u) => u.id));
    const idsIniciais = new Set(this.usuariosIniciaisComPermissao.map((u) => u.id));

    const adicionarUsuariosIds = this.usuariosComPermissao
      .filter((u) => !idsIniciais.has(u.id))
      .map((u) => u.id);

    const removerUsuariosIds = this.usuariosIniciaisComPermissao
      .filter((u) => !idsAtuais.has(u.id))
      .map((u) => u.id);

    const dto: PastaPermissaoAcaoDTO = {
      pastaId: this.pastaParaPermissao.id,
      adicionarUsuariosIds: adicionarUsuariosIds,
      removerUsuariosIds: removerUsuariosIds,
    };

    this.loading = true;
    this.adminService.atualizarPermissoesAcao(dto).subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalPermissao();
        this.loading = false;
      },
      error: (err) => this.handleError('Erro ao atualizar permissões', err),
    });
  }

  fecharModalPermissao(): void {
    this.modalPermissaoAberto = false;
    this.pastaParaPermissao = null;
    this.usuariosComPermissao = [];
    this.usuariosDisponiveis = [];
    this.usuariosIniciaisComPermissao = [];
  }

  // ---------------- Helpers ----------------
  isPasta(item: PastaAdmin | ArquivoAdmin): item is PastaAdmin {
    return (item as PastaAdmin).subPastas !== undefined;
  }

  getNomeItem(item: PastaAdmin | ArquivoAdmin | null): string {
    return item ? (this.isPasta(item) ? item.nomePasta : item.nome) : '';
  }

  private handleError(msg: string, err?: any): void {
    console.error(msg, err);
    this.loading = false;
  }
}