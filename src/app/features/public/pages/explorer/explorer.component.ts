import { Component, OnInit } from '@angular/core';
import { PublicService, PastaPublica, ArquivoPublico } from '../../../../services/public.service';
import { CommonModule, DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-explorer',
  standalone: true, // Adicionado para componentes autônomos
  imports: [CommonModule, DecimalPipe],
  templateUrl: './explorer.component.html',
  styleUrls: ['./explorer.component.scss'],
})
export class ExplorerComponent implements OnInit {
 pastas: PastaPublica[] = [];
  arquivos: ArquivoPublico[] = [];
  breadcrumb: PastaPublica[] = [];
  pastaAtual?: PastaPublica;
  loading = false;

  arquivoSelecionado?: ArquivoPublico;

  constructor(private publicService: PublicService) {}

  ngOnInit(): void {
    this.carregarPastas();
  }

  get pastasAtuais(): PastaPublica[] {
    return this.pastaAtual ? this.pastaAtual.subPastas : this.pastas;
  }

  carregarPastas(): void {
    this.loading = true;
    this.publicService.listarPastas().subscribe({
      next: pastas => {
        this.pastas = pastas.map(p => ({
          ...p,
          subPastas: p.subPastas ?? [],
          arquivos: p.arquivos ?? [],
        }));
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  abrirPasta(pasta: PastaPublica): void {
    this.breadcrumb.push(pasta);
    this.pastaAtual = {
      ...pasta,
      subPastas: pasta.subPastas ?? [],
      arquivos: pasta.arquivos ?? []
    };
    this.arquivos = this.pastaAtual.arquivos;
  }

  navegarPara(index: number): void {
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    const pasta = this.breadcrumb[index];
    this.pastaAtual = {
      ...pasta,
      subPastas: pasta.subPastas ?? [],
      arquivos: pasta.arquivos ?? []
    };
    this.arquivos = this.pastaAtual.arquivos;
  }

  abrirArquivo(arquivo: ArquivoPublico): void {
    this.arquivoSelecionado = arquivo;
  }

  fecharModal(): void {
    this.arquivoSelecionado = undefined;
  }

  baixarArquivo(arquivo: ArquivoPublico): void {
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

  visualizarArquivo(arquivo: ArquivoPublico): void {
    this.publicService.downloadArquivo(arquivo.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
      this.fecharModal();
    });
  }
}