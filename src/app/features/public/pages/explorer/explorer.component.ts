import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import {
  PublicService,
  PastaPublica,
  ArquivoPublico,
} from '../../../../services/public.service';
import { PastaItemComponent } from '../pasta-item/pasta-item.component';

@Component({
  selector: 'app-explorer',
  standalone: true,
  imports: [CommonModule, DecimalPipe, PastaItemComponent],
  templateUrl: './explorer.component.html',
  styleUrls: ['./explorer.component.scss'],
})
export class ExplorerComponent implements OnInit {
  pastasRaiz: PastaPublica[] = [];
  pastaAtual?: PastaPublica;
  breadcrumb: PastaPublica[] = [];
  arquivoSelecionado?: ArquivoPublico;
  loading = false;

  constructor(private publicService: PublicService) {}

  ngOnInit(): void {
    this.carregarPastasRaiz();
  }

  // Carrega pastas raiz
  carregarPastasRaiz(): void {
    this.loading = true;
    this.publicService.listarPastas().subscribe({
      next: pastas => {
        this.pastasRaiz = pastas.map(p => ({
          ...p,
          subPastas: p.subPastas ?? [],
          arquivos: p.arquivos ?? []
        }));
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // Abrir pasta
  abrirPasta(pasta: PastaPublica): void {
    this.pastaAtual = { ...pasta, subPastas: pasta.subPastas ?? [], arquivos: pasta.arquivos ?? [] };
    this.breadcrumb.push(this.pastaAtual);
  }

  // Navegar pelo breadcrumb
  navegarPara(index: number): void {
    if (index === -1) { // voltar para raiz
      this.pastaAtual = undefined;
      this.breadcrumb = [];
      return;
    }
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    this.pastaAtual = this.breadcrumb[this.breadcrumb.length - 1];
  }

  resetExplorer(): void {
    this.pastaAtual = undefined;
    this.breadcrumb = [];
  }
  

  // Modal de arquivo
  abrirArquivoModal(arquivo: ArquivoPublico): void {
    this.arquivoSelecionado = arquivo;
  }

  fecharModal(): void {
    this.arquivoSelecionado = undefined;
  }

  visualizarArquivo(arquivo: ArquivoPublico): void {
    this.publicService.downloadArquivo(arquivo.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    });
  }

  baixarArquivo(arquivo: ArquivoPublico): void {
    this.publicService.downloadArquivo(arquivo.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = arquivo.nome;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}