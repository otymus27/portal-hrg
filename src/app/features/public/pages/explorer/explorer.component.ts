import { Component, OnInit } from '@angular/core';
import { PublicService, PastaPublica, ArquivoPublico } from '../../../../services/public.service';
import { CommonModule} from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-explorer',
  standalone: true, // Adicionado para componentes autônomos
  imports: [CommonModule],
  templateUrl: './explorer.component.html',
  styleUrls: ['./explorer.component.scss'],
})
export class ExplorerComponent implements OnInit {
  pastas: PastaPublica[] = [];
  pastaAtual: PastaPublica | null = null;
  breadcrumb: PastaPublica[] = [];
  arquivos: ArquivoPublico[] = [];
  loading = false;

  // Modal
  arquivoSelecionado?: ArquivoPublico;

  constructor(public publicService: PublicService,private router: Router) {}

  ngOnInit(): void {
    this.carregarPastas();
  }

  carregarPastas(): void {
    this.loading = true;
    this.publicService.listarPastas().subscribe({
      next: pastas => {
        this.pastas = pastas.map(p => ({
          ...p,
          subPastas: p.subPastas ?? [],
          arquivos: p.arquivos ?? []
        }));
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  abrirPasta(pasta: PastaPublica): void {
    this.breadcrumb.push(pasta);
    this.pastaAtual = { ...pasta, subPastas: pasta.subPastas ?? [], arquivos: pasta.arquivos ?? [] };
    this.arquivos = this.pastaAtual.arquivos;
  }

  abrirSubPasta(subPasta: PastaPublica): void {
    this.abrirPasta(subPasta);
  }

  navegarPara(index: number): void {
    if(index < 0) {
      this.breadcrumb = [];
      this.pastaAtual = null;
      this.arquivos = [];
      return;
    }
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    this.pastaAtual = this.breadcrumb[this.breadcrumb.length - 1];
    this.arquivos = this.pastaAtual.arquivos ?? [];
  }

  navegarParaHome(): void {
    this.router.navigate(['/home']);
  }

  abrirArquivo(arquivo: ArquivoPublico) {
    this.arquivoSelecionado = arquivo;
  }

  fecharModal() {
    this.arquivoSelecionado = undefined;
  }

  baixarArquivo(arquivo: ArquivoPublico) {
    this.publicService.downloadArquivo(arquivo.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = arquivo.nome;
      a.click();
      URL.revokeObjectURL(url);
      this.fecharModal();
    });
  }

  // src/app/features/public/pages/explorer/explorer.component.ts

// ...
visualizarArquivo(arquivo: ArquivoPublico): void {
  this.publicService.visualizarArquivo(arquivo.id).subscribe((blob) => {
    let tipoMimeCorrigido = arquivo.tipoMime;
    
    // Verifica se o nome do arquivo termina com .pdf (insensível a maiúsculas/minúsculas)
    // ou se o tipoMime contém 'pdf'.
    if (arquivo.nome.toLowerCase().endsWith('.pdf') || (arquivo.tipoMime && arquivo.tipoMime.includes('pdf'))) {
      tipoMimeCorrigido = 'application/pdf';
    }

    const blobComTipo = new Blob([blob], { type: tipoMimeCorrigido });
    const url = window.URL.createObjectURL(blobComTipo);
    
    // Abre a URL em uma nova aba
    window.open(url, '_blank');
    
    // Fecha o modal após abrir o arquivo
    this.fecharModal();
    
    // É uma boa prática revogar a URL para liberar memória,
    // mas isso pode ser feito após a nova aba ser carregada
    // ou em um evento de fechamento da nova aba.
  });
}
// ...



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


  voltar() {
    this.navegarPara(this.breadcrumb.length - 2);
  }
}