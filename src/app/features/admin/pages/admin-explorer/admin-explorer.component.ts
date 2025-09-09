import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
} from '../../services/admin.service';
import { ArquivoPublico } from '../../../../services/public.service';

// Defina um tipo genérico para o callback de sucesso
type SuccessCallback = () => void;

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

  // Modais e dados para CRUD
  modalCriarPastaAberto = false;
  novoNomePasta: string = '';

  modalRenomearAberto = false;
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem: string = '';

  modalUploadAberto = false;
  arquivoParaUpload: File | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    // A inicialização agora chama o método específico para a raiz.
    this.carregarConteudoRaiz();
  }

  // --- Métodos de Navegação ---
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
        // Exibir notificação de erro ao usuário
        this.loading = false;
      },
    });
  }

  abrirPasta(pasta: PastaAdmin): void {
    this.breadcrumb.push(pasta);
    this.loading = true;
    // O método abrirPasta agora usa o serviço para buscar o conteúdo da pasta por ID.
    this.adminService.listarConteudoPorId(pasta.id).subscribe({
      next: (conteudo) => {
        this.pastas = conteudo.pastas;
        this.arquivos = conteudo.arquivos;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar conteúdo da pasta:', err);
        this.loading = false;
      },
    });
  }

  navegarPara(index: number): void {
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    const pastaAtual = this.obterPastaAtual();
    if (pastaAtual) {
      this.abrirPasta(pastaAtual);
      // Remove o último item para evitar duplicação no breadcrumb.
      this.breadcrumb.pop();
    } else {
      this.carregarConteudoRaiz();
    }
  }

  // --- Métodos de Criação de Pasta ---
  abrirModalCriarPasta(): void {
    this.modalCriarPastaAberto = true;
    this.novoNomePasta = '';
  }

  fecharModalCriarPasta(): void {
    this.modalCriarPastaAberto = false;
  }

  criarPasta(): void {
    if (!this.novoNomePasta) return;
    const pastaPaiId = this.obterPastaAtual()?.id;
    this.adminService.criarPasta(this.novoNomePasta, pastaPaiId).subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalCriarPasta();
      },
      error: (err) => {
        console.error('Erro ao criar pasta:', err);
      },
    });
  }

  // --- Métodos de Renomeação ---
  abrirModalRenomear(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaRenomear = item;
    this.novoNomeItem = 'nome' in item ? item.nome : '';
    this.modalRenomearAberto = true;
  }

  fecharModalRenomear(): void {
    this.modalRenomearAberto = false;
    this.itemParaRenomear = null;
    this.novoNomeItem = '';
  }

  renomearItem(): void {
    if (!this.itemParaRenomear || !this.novoNomeItem) return;

    if (this.isFolder(this.itemParaRenomear)) {
      this.adminService
        .renomearPasta(this.itemParaRenomear.id, this.novoNomeItem)
        .subscribe({
          next: () => this.recarregarConteudo(this.fecharModalRenomear),
          error: (err) => console.error('Erro ao renomear pasta:', err),
        });
    } else {
      const arquivoItem = this.itemParaRenomear as ArquivoAdmin;
      if (arquivoItem && typeof arquivoItem.id === 'string') {
        this.adminService
          .renomearArquivo(arquivoItem.id, this.novoNomeItem)
          .subscribe({
            next: () => this.recarregarConteudo(this.fecharModalRenomear),
            error: (err) => console.error('Erro ao renomear arquivo:', err),
          });
      } else {
        console.error('O item a ser renomeado não é um arquivo válido.');
        this.fecharModalRenomear();
      }
    }
  }

  // --- Métodos de Exclusão ---
  excluirPasta(pasta: PastaAdmin): void {
    console.log(`Excluindo a pasta "${pasta.nome}"...`);
    this.adminService.excluirPasta(pasta.id).subscribe({
      next: () => this.recarregarConteudo(),
      error: (err) => console.error('Erro ao excluir pasta:', err),
    });
  }

  excluirArquivo(arquivo: ArquivoAdmin): void {
    console.log(`Excluindo o arquivo "${arquivo.nome}"...`);
    this.adminService.excluirArquivo(arquivo.id).subscribe({
      next: () => this.recarregarConteudo(),
      error: (err) => console.error('Erro ao excluir arquivo:', err),
    });
  }

  // --- Métodos de Upload de Arquivo ---
  abrirModalUpload(): void {
    this.modalUploadAberto = true;
  }

  fecharModalUpload(): void {
    this.modalUploadAberto = false;
    this.arquivoParaUpload = null;
  }

  onArquivoSelecionado(event: any): void {
    this.arquivoParaUpload = event.target.files[0];
  }

  uploadArquivo(): void {
    if (!this.arquivoParaUpload) {
      console.warn('Nenhum arquivo selecionado.');
      return;
    }

    const pastaAtual = this.obterPastaAtual();
    if (!pastaAtual) {
      console.error('Selecione uma pasta para o upload.');
      return;
    }

    this.adminService
      .uploadArquivo(this.arquivoParaUpload, pastaAtual.id)
      .subscribe({
        next: () => {
          this.recarregarConteudo();
          this.fecharModalUpload();
        },
        error: (err) => {
          console.error('Erro no upload do arquivo:', err);
        },
      });
  }

  // --- Métodos Auxiliares Refatorados ---
  private obterPastaAtual(): PastaAdmin | undefined {
    return this.breadcrumb.length > 0
      ? this.breadcrumb[this.breadcrumb.length - 1]
      : undefined;
  }

  private recarregarConteudo(callback?: SuccessCallback): void {
    const pastaPaiId = this.obterPastaAtual()?.id;
    if (pastaPaiId) {
      this.abrirPasta(this.obterPastaAtual()!);
    } else {
      this.carregarConteudoRaiz();
    }
    if (callback) {
      callback();
    }
  }

  // Método auxiliar para verificação de tipo
  isFolder(item: PastaAdmin | ArquivoAdmin | null): boolean {
    return !!item && 'subPastas' in item;
  }

  getIcon(arquivo: ArquivoPublico): string {
    const extensao = arquivo.nome.split('.').pop()?.toLowerCase();

    switch (extensao) {
      case 'pdf':
        return 'assets/icons/pdf.png';
      case 'doc':
      case 'docx':
        return 'assets/icons/word.png';
      case 'xls':
      case 'xlsx':
        return 'assets/icons/xls.png';
      case 'ppt':
      case 'pptx':
        return 'assets/icons/ppt.png';
      case 'txt':
        return 'assets/icons/txt.png';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return 'assets/icons/image.png';
      default:
        return 'assets/icons/pdf.png'; // ícone genérico
    }
  }

  fecharTodosModais(): void {
    this.modalCriarPastaAberto = false;
    this.modalRenomearAberto = false;
    this.modalUploadAberto = false;
    this.arquivoParaUpload = null;
    this.itemParaRenomear = null;
    this.novoNomeItem = '';
    this.novoNomePasta = '';
  }
}
