import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Pasta } from '../../services/pasta.service';

@Component({
  selector: 'app-pasta-card',
  templateUrl: './pasta-card.component.html',
  styleUrls: ['./pasta-card.component.css']
})
export class PastaCardComponent {
  @Input() pasta!: Pasta;
  @Output() abrir = new EventEmitter<Pasta>();
  @Output() editar = new EventEmitter<Pasta>();
  @Output() excluir = new EventEmitter<number>();
}

