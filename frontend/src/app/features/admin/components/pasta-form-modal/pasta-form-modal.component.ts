import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pasta-form-modal',
  templateUrl: './pasta-form-modal.component.html',
  styleUrls: ['./pasta-form-modal.component.css']
})
export class PastaFormModalComponent {
  @Input() pastaId?: number;
  @Input() nome: string = '';
  @Output() salvarPasta = new EventEmitter<{id?: number, nome: string}>();
  @Output() fechar = new EventEmitter<void>();

  salvar() {
    if (this.nome.trim()) {
      this.salvarPasta.emit({ id: this.pastaId, nome: this.nome });
      this.close();
    }
  }

  close() { this.fechar.emit(); }
}
