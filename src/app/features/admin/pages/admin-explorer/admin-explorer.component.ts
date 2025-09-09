import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
  Paginacao,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';

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

  // Dados para CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  arquivoParaUpload: File | null = null;

  // Usuários para permissões
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

  // --- Navegação ---
  carregarConteudoRaiz(): void {
    this.loading = true;
    this.adminService.listarConteudoRaiz().subscribe({
      next: (conteudo) => {
        this.pastas = conteudo.pastas;
        this.arquivos = conteudo.arquivos;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar conteúdo:', err);
        this.loading = false;
      },
    });
  }

  abrirPasta(pasta: PastaAdmin): void {
    this.breadcrumb.push(pasta);
    this.loading = true;
    this.adminService.listarConteudoPorId(pasta.id).subscribe({
      next: (conteudo) => {
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
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    const pastaAtual = this.obterPastaAtual();
    if (pastaAtual) {
      this.abrirPasta(pastaAtual);
      this.breadcrumb.pop(); // evita duplicação
    } else {
      this.carregarConteudoRaiz();
    }
  }

  private obterPastaAtual(): PastaAdmin | undefined {
    return this.breadcrumb.length > 0
      ? this.breadcrumb[this.breadcrumb.length - 1]
      : undefined;
  }

  private recarregarConteudo(): void {
    const pastaAtual = this.obterPastaAtual();
    if (pastaAtual) {
      this.abrirPasta(pastaAtual);
    } else {
      this.carregarConteudoRaiz();
    }
  }

  // --- Criar Pasta ---
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
  // --- Renomear ---
  abrirModalRenomear(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaRenomear = item;
    this.novoNomeItem = item.nome;
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
        .subscribe({
          next: () => this.recarregarConteudo(),
          error: (err) => console.error('Erro ao renomear pasta:', err),
        });
    } else {
      this.adminService
        .renomearArquivo(this.itemParaRenomear.id, this.novoNomeItem)
        .subscribe({
          next: () => this.recarregarConteudo(),
          error: (err) => console.error('Erro ao renomear arquivo:', err),
        });
    }
    this.fecharModalRenomear();
  }

  // --- Excluir ---
  excluirPasta(pasta: PastaAdmin): void {
    this.adminService.excluirPasta(pasta.id).subscribe({
      next: () => this.recarregarConteudo(),
      error: (err) => console.error('Erro ao excluir pasta:', err),
    });
  }

  excluirArquivo(arquivo: ArquivoAdmin): void {
    this.adminService.excluirArquivo(arquivo.id).subscribe({
      next: () => this.recarregarConteudo(),
      error: (err) => console.error('Erro ao excluir arquivo:', err),
    });
  }

  // --- Upload ---
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
    if (!pastaAtual) {
      console.error('Selecione uma pasta antes do upload.');
      return;
    }

    this.adminService
      .uploadArquivo(this.arquivoParaUpload, pastaAtual.id)
      .subscribe({
        next: () => {
          this.recarregarConteudo();
          this.fecharModalUpload();
        },
        error: (err) => console.error('Erro no upload:', err),
      });
  }

  // --- Download ---
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

  // --- Utilitários ---
  isFolder(item: PastaAdmin | ArquivoAdmin | null): boolean {
    return !!item && 'subPastas' in item;
  }

  carregarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (resposta: Paginacao<Usuario>) => {
        this.usuarios = resposta.content; // ✅ pega apenas o array
      },
      error: () => console.error('Erro ao carregar usuários:'),
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    if (!this.usuariosSelecionados.find((u) => u.id === usuario.id)) {
      this.usuariosSelecionados.push(usuario);
    }
  }

  removerUsuario(usuario: Usuario): void {
    this.usuariosSelecionados = this.usuariosSelecionados.filter(
      (u) => u.id !== usuario.id
    );
  }
}
