import { Component, OnInit } from '@angular/core';

import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PastaService } from '../../../services/pasta.service';

// ✅ DTO para corresponder à estrutura de dados do backend
export interface ItemDTO {
  nome: string;
  isDiretorio: boolean;
  tamanho?: number;
  contagem?: number;
}

@Component({
  selector: 'app-protocolos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './protocolos.component.html',
  styleUrls: ['./protocolos.component.scss'],
})
export class ProtocolosComponent implements OnInit {
  pastas: ItemDTO[] = [];
  isLoading = true;
  errorMessage = '';
  caminhoAtual: string = '';
  caminhoCompleto: string[] = [];

  // ✅ Adicionado: Variáveis para controle do modal de confirmação
  showConfirmationModal = false;
  selectedFile: ItemDTO | null = null;

  constructor(
    private pastaService: PastaService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Escuta as mudanças nos parâmetros da rota para carregar o conteúdo correto
    this.route.paramMap.subscribe((params) => {
      this.caminhoAtual = params.get('caminho') || '';
      this.caminhoCompleto = this.caminhoAtual.split('/').filter((p) => p);
      this.carregarConteudo(this.caminhoAtual);
    });
  }

  carregarConteudo(caminho: string): void {
    this.isLoading = true;
    this.pastaService.getConteudoPasta(caminho).subscribe({
      next: (data) => {
        this.pastas = data;
        this.isLoading = false;
        this.errorMessage = '';
      },
      error: (error) => {
        this.errorMessage =
          'Erro ao carregar as pastas. Verifique o console para mais detalhes.';
        this.isLoading = false;
        console.error('Erro na requisição da API:', error);
      },
    });
  }

  navegarPara(item: ItemDTO): void {
    if (item.isDiretorio) {
      const novoCaminho = this.caminhoAtual
        ? `${this.caminhoAtual}/${item.nome}`
        : item.nome;
      this.router.navigate(['/protocolos', { caminho: novoCaminho }]);
    } else {
      this.selectedFile = item;
      this.showConfirmationModal = true;
    }
  }

  // Métodos para gerenciar o modal
  onDownloadClick(): void {
    if (this.selectedFile) {
      this.pastaService.downloadArquivo(
        this.caminhoAtual
          ? `${this.caminhoAtual}/${this.selectedFile.nome}`
          : this.selectedFile.nome
      );
      this.closeConfirmationModal();
    }
  }

  onOpenClick(): void {
    if (this.selectedFile) {
      const filePath = this.caminhoAtual
        ? `${this.caminhoAtual}/${this.selectedFile.nome}`
        : this.selectedFile.nome;
      // ✅ Correção: Abre a URL em uma nova janela em vez de chamar o serviço de download.
      const fileUrl = `http://localhost:8082/api/publico/arquivos/view?caminho=${encodeURIComponent(
        filePath
      )}`;
      window.open(fileUrl, '_blank');
      this.closeConfirmationModal();
    }
  }

  closeConfirmationModal(): void {
    this.showConfirmationModal = false;
    this.selectedFile = null;
  }
  voltar(): void {
    const caminhoPai = this.caminhoCompleto.slice(0, -1).join('/');
    this.router.navigate(['/protocolos', { caminho: caminhoPai }]);
  }

  navegarParaRaiz(): void {
    this.router.navigate(['/protocolos']);
  }

  navegarParaHome(): void {
    this.router.navigate(['/home']);
  }

  // ✅ Adicionado: Novo método para navegação do breadcrumb
  navegarParaCaminho(caminho: string): void {
    this.router.navigate(['/protocolos', { caminho: caminho }]);
  }

  obterIconePorTipo(nomeArquivo: string): string {
    const extensao = nomeArquivo.split('.').pop()?.toLowerCase();
    switch (extensao) {
      case 'pdf':
        return 'fa-file-pdf';
      case 'doc':
      case 'docx':
        return 'fa-file-word';
      case 'xls':
      case 'xlsx':
        return 'fa-file-excel';
      case 'ppt':
      case 'pptx':
        return 'fa-file-powerpoint';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
      case 'bmp':
        return 'fa-file-image';
      case 'zip':
      case 'rar':
      case '7z':
        return 'fa-file-archive';
      case 'mp4':
      case 'mov':
      case 'avi':
        return 'fa-file-video';
      case 'mp3':
      case 'wav':
        return 'fa-file-audio';
      case 'txt':
        return 'fa-file-alt';
      default:
        return 'fa-file';
    }
  }
}
