import { Component, OnInit } from '@angular/core';
import { PastaService, Pasta } from '../../services/pasta.service';

@Component({
  selector: 'app-pastas-page',
  templateUrl: './../pastas/pastas.component.html',
  styleUrls: ['./../pastas/pastas.component.scss']
})
export class PastasPage implements OnInit {
  pastas: Pasta[] = [];
  modalAberto = false;
  pastaEditando?: Pasta;

  constructor(private pastaService: PastaService) {}

  ngOnInit() { this.carregarPastas(); }

  carregarPastas() {
    this.pastaService.listarPastasRaiz().subscribe({
      next: pastas => this.pastas = pastas,
      error: err => alert(err)
    });
  }

  abrirModal() { this.modalAberto = true; this.pastaEditando = undefined; }

  editarPasta(pasta: Pasta) { this.pastaEditando = pasta; this.modalAberto = true; }

  salvarPasta(dados: {id?: number, nome: string}) {
    const request = dados.id
      ? this.pastaService.renomearPasta(dados.id, { nome: dados.nome })
      : this.pastaService.criarPasta({ nome: dados.nome });

    request.subscribe({
      next: () => this.carregarPastas(),
      error: err => alert(err)
    });
  }

  excluirPasta(id: number) {
    this.pastaService.excluirPasta(id).subscribe({
      next: () => this.carregarPastas(),
      error: err => alert(err)
    });
  }

  abrirPasta(pasta: Pasta) {
    console.log('Abrir pasta:', pasta);
  }
}
