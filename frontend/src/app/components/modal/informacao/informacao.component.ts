import { Component, Input } from '@angular/core';
import { MdbModalRef } from 'mdb-angular-ui-kit/modal';

@Component({
  selector: 'app-informacao',
  imports: [],
  templateUrl: './informacao.component.html',
  styleUrl: './informacao.component.scss',
})
export class InformacaoComponent {
  // ✅ Este input receberá a senha do componente pai
  @Input() password: string = '';

  constructor(public modalRef: MdbModalRef<InformacaoComponent>) {}

  ngOnInit(): void {}
}
