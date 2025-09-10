import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';
import { Paginacao } from '../../../../models/paginacao';

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

  // CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  arquivoParaUpload: File | null = null;
  itemParaExcluir: PastaAdmin | ArquivoAdmin | null = null;

  // Usuários
  usuarios: Usuario[] = [];
  usuariosSelecionados: Usuario[] = [];

  constructor(
    private adminService: AdminService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.carregarConteudoRaiz();
    this.carregarUsuarios();
  }

  // ---------------- Navegação ----------------
  carregarConteudoRaiz(): void {
    this.loading = true;
    this.adminService.listarConteudoRaiz().subscribe({
      next: (conteudo: ConteudoPasta) => {
        this.pastas = conteudo.pastas;
        this.arquivos = conteudo.arquivos;
        this.breadcrumb = [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar conteúdo:', err);
        this.loading = false;
      },
    });
  }

  abrirPasta(pasta: PastaAdmin): void {
    if (!pasta) return;
    this.breadcrumb.push(pasta);
    this.loading = true;
    this.adminService.listarConteudoPorId(pasta.id).subscribe({
      next: (conteudo: ConteudoPasta) => {
        this.pastas = conteudo.pastas;
        this.arquivos = conteudo.arquivos;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao abrir pasta:', err);
        this.loading = false;
      },
    });
  }

  navegarPara(index: number): void {
    if (index < 0) {
      this.carregarConteudoRaiz();
      return;
    }
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    const pastaAtual = this.obterPastaAtual();
    if (pastaAtual) {
      this.abrirPasta(pastaAtual);
      this.breadcrumb.pop();
    }
  }

  voltarUmNivel(): void {
    this.navegarPara(this.breadcrumb.length - 2);
  }

  private obterPastaAtual(): PastaAdmin | undefined {
    return this.breadcrumb.length > 0
      ? this.breadcrumb[this.breadcrumb.length - 1]
      : undefined;
  }

  private recarregarConteudo(): void {
    const pastaAtual = this.obterPastaAtual();
    if (pastaAtual) this.abrirPasta(pastaAtual);
    else this.carregarConteudoRaiz();
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
      pastaPaiId: this.obterPastaAtual()?.id ?? undefined,
      usuariosComPermissaoIds: this.usuariosSelecionados.map((u) => u.id),
    };
    this.adminService.criarPasta(body).subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalCriarPasta();
      },
      error: (err) => console.error('Erro ao criar pasta:', err),
    });
  }

  abrirModalRenomear(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaRenomear = item;
    this.novoNomeItem = this.isPasta(item) ? item.nomePasta : item.nome;
    this.modalRenomearAberto = true;
  }

  fecharModalRenomear(): void {
    this.modalRenomearAberto = false;
    this.itemParaRenomear = null;
    this.novoNomeItem = '';
  }

  renomearItem(): void {
    if (!this.itemParaRenomear || !this.novoNomeItem) return;
    if ('subPastas' in this.itemParaRenomear) {
      this.adminService
        .renomearPasta(this.itemParaRenomear.id, this.novoNomeItem)
        .subscribe(() => this.recarregarConteudo());
    } else {
      this.adminService
        .renomearArquivo(this.itemParaRenomear.id, this.novoNomeItem)
        .subscribe(() => this.recarregarConteudo());
    }
    this.fecharModalRenomear();
  }

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
    if ('subPastas' in this.itemParaExcluir) {
      this.adminService
        .excluirPasta(this.itemParaExcluir.id)
        .subscribe(() => this.recarregarConteudo());
    } else {
      this.adminService
        .excluirArquivo(this.itemParaExcluir.id)
        .subscribe(() => this.recarregarConteudo());
    }
    this.fecharModalExcluir();
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

  onArquivoSelecionado(event: any): void {
    this.arquivoParaUpload = event.target.files[0];
  }

  uploadArquivo(): void {
    if (!this.arquivoParaUpload) return;
    const pastaAtual = this.obterPastaAtual();
    if (!pastaAtual)
      return console.error('Selecione uma pasta antes do upload.');

    this.adminService
      .uploadArquivo(this.arquivoParaUpload, pastaAtual.id)
      .subscribe(() => {
        this.recarregarConteudo();
        this.fecharModalUpload();
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

  isFolder(item: PastaAdmin | ArquivoAdmin | null): boolean {
    return !!item && 'subPastas' in item;
  }

  // Type guard
  isPasta(item: PastaAdmin | ArquivoAdmin): item is PastaAdmin {
    return (item as PastaAdmin).subPastas !== undefined;
  }

  // Função para obter o nome do item
  getNomeItem(item: PastaAdmin | ArquivoAdmin | null): string {
    if (!item) return '';
    return this.isPasta(item) ? item.nomePasta : item.nome;
  }

  // ---------------- Usuários ----------------
  carregarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (resposta: Paginacao<Usuario>) => {
        this.usuarios = resposta.content || [];
      },
      error: () => console.error('Erro ao carregar usuários'),
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    const idx = this.usuariosSelecionados.findIndex((u) => u.id === usuario.id);
    if (idx === -1) this.usuariosSelecionados.push(usuario);
    else this.usuariosSelecionados.splice(idx, 1);
  }

}
