import { Component, Input, Output, EventEmitter } from '@angular/core';
import { PastaPublica, ArquivoPublico } from '../../../../services/public.service';
import { CommonModule, DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-pasta-item',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './pasta-item.component.html',
  styleUrls: ['./pasta-item.component.scss'],
})
export class PastaItemComponent {
  @Input() pasta: PastaPublica = {
    id: 0,
    nome: '',
    caminhoCompleto: '',
    subPastas: [],
    arquivos: [],
  };

  @Output() abrir = new EventEmitter<PastaPublica>();
  @Output() baixar = new EventEmitter<ArquivoPublico>();

  abrirPasta(): void {
    this.abrir.emit(this.pasta);
  }

  baixarArquivo(arquivo: ArquivoPublico): void {
    this.baixar.emit(arquivo);
  }
}
