import { Component, OnInit } from '@angular/core';
import { MdbModalRef } from 'mdb-angular-ui-kit/modal';

@Component({
  selector: 'app-confirmation-modal',
  template: `
    <div class="modal-header bg-danger text-white">
      <h5 class="modal-title fw-bold">Confirmação de Ação</h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="modalRef.close(false)"></button>
    </div>
    <div class="modal-body">
      <p>Tem certeza que deseja resetar a senha deste usuário?</p>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="modalRef.close(false)">Cancelar</button>
      <button type="button" class="btn btn-danger" (click)="modalRef.close(true)">Confirmar</button>
    </div>
  `,
  standalone: true,
})
export class ModalConfirmacaoComponent implements OnInit {
  constructor(public modalRef: MdbModalRef<ModalConfirmacaoComponent>) {}

  ngOnInit(): void {}
}
