import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
  PastaExcluirDTO,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';
import { Paginacao } from '../../../../models/paginacao';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-admin-explorer',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule],
  templateUrl: './admin-explorer.component.html',
  styleUrls: ['./admin-explorer.component.scss'],
})
export class AdminExplorerComponent implements OnInit {
  pastas: PastaAdmin[] = [];
  arquivos: ArquivoAdmin[] = [];
  breadcrumb: PastaAdmin[] = [];
  loading = false;

  // Modais
  modalCriarPastaAberto = false;
  modalRenomearAberto = false;
  modalUploadAberto = false;
  modalExcluirAberto = false;
  modalExcluirSelecionadosAberto = false;

  // CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  itemParaExcluir: PastaAdmin | ArquivoAdmin | null = null;

  // Usuários
  usuarios: Usuario[] = [];
  usuariosSelecionados: Usuario[] = [];

  // Seleção
  itensSelecionados: (PastaAdmin | ArquivoAdmin)[] = [];

  // Upload múltiplo
  arquivosParaUpload: File[] = [];

  constructor(
    private adminService: AdminService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.recarregarConteudo();
    this.carregarUsuarios();
  }

  // ---------------- Navegação ----------------
  recarregarConteudo(): void {
    const pastaAtual = this.obterPastaAtual();
    pastaAtual
      ? this.abrirPasta(pastaAtual, false)
      : this.carregarConteudoRaiz();
  }

  carregarConteudoRaiz(): void {
    this.loading = true;
    this.adminService.listarConteudoRaiz().subscribe({
      next: (conteudo: ConteudoPasta) => {
        this.atualizarConteudo(conteudo);
        this.breadcrumb = [];
      },
      error: (err: any) =>
        this.handleError('Erro ao carregar conteúdo raiz', err),
    });
  }

  abrirPasta(pasta: PastaAdmin, adicionarBreadcrumb = true): void {
    if (!pasta) return;
    if (adicionarBreadcrumb) this.breadcrumb.push(pasta);

    this.loading = true;
    this.adminService.listarConteudoPorId(pasta.id).subscribe({
      next: (conteudo: ConteudoPasta) => this.atualizarConteudo(conteudo),
      error: (err: any) => this.handleError('Erro ao abrir pasta', err),
    });
  }

  navegarPara(index: number): void {
    if (index < 0) return this.carregarConteudoRaiz();
    this.breadcrumb = this.breadcrumb.slice(0, index + 1);
    this.recarregarConteudo();
  }

  voltarUmNivel(): void {
    this.navegarPara(this.breadcrumb.length - 2);
  }

  private atualizarConteudo(conteudo: ConteudoPasta): void {
    this.pastas = conteudo.pastas;
    this.arquivos = conteudo.arquivos;
    this.loading = false;
    this.itensSelecionados = [];
  }

  public obterPastaAtual(): PastaAdmin | undefined {
    return this.breadcrumb[this.breadcrumb.length - 1];
  }

  // ---------------- CRUD ----------------
  abrirModalCriarPasta(): void {
    this.modalCriarPastaAberto = true;
    this.novoNomePasta = '';
    this.usuariosSelecionados = [];
  }

  fecharModalCriarPasta(): void {
    this.modalCriarPastaAberto = false;
  }

  criarPasta(): void {
    if (!this.novoNomePasta || this.usuariosSelecionados.length === 0) return;

    const body = {
      nome: this.novoNomePasta,
      pastaPaiId: this.obterPastaAtual()?.id,
      usuariosComPermissaoIds: this.usuariosSelecionados.map((u) => u.id),
    };

    this.adminService.criarPasta(body).subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalCriarPasta();
      },
      error: (err: any) => this.handleError('Erro ao criar pasta', err),
    });
  }

  abrirModalRenomear(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaRenomear = item;
    this.novoNomeItem = this.getNomeItem(item);
    this.modalRenomearAberto = true;
  }

  fecharModalRenomear(): void {
    this.modalRenomearAberto = false;
    this.itemParaRenomear = null;
    this.novoNomeItem = '';
  }

  renomearItem(): void {
    if (!this.itemParaRenomear || !this.novoNomeItem) return;

    const request: Observable<any> = this.isPasta(this.itemParaRenomear)
      ? this.adminService.renomearPasta(
          this.itemParaRenomear.id,
          this.novoNomeItem
        )
      : this.adminService.renomearArquivo(
          this.itemParaRenomear.id,
          this.novoNomeItem
        );

    request.subscribe({
      next: () => this.recarregarConteudo(),
      error: (err: any) => this.handleError('Erro ao renomear item', err),
    });

    this.fecharModalRenomear();
  }

  // ---------------- Upload ----------------
  abrirModalUpload(): void {
    this.modalUploadAberto = true;
  }

  fecharModalUpload(): void {
    this.modalUploadAberto = false;
    this.arquivosParaUpload = [];
  }

  onArquivosSelecionados(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input?.files) {
      // Acumula os arquivos selecionados, sem duplicatas pelo name + size
      const novosArquivos = Array.from(input.files).filter(
        (novo) =>
          !this.arquivosParaUpload.some(
            (existente) =>
              existente.name === novo.name && existente.size === novo.size
          )
      );
      this.arquivosParaUpload.push(...novosArquivos);
    }
  }

  removerArquivoSelecionado(file: File): void {
    this.arquivosParaUpload = this.arquivosParaUpload.filter((f) => f !== file);
  }

  uploadArquivos(): void {
    const pastaAtual = this.obterPastaAtual();
    if (!pastaAtual)
      return this.handleError('Selecione uma pasta antes do upload');
    if (!this.arquivosParaUpload.length) return;

    this.loading = true;
    this.adminService
      .uploadMultiplosArquivos(this.arquivosParaUpload, pastaAtual.id)
      .subscribe({
        next: () => {
          this.recarregarConteudo();
          this.fecharModalUpload();
          this.loading = false;
          this.arquivosParaUpload = [];
        },
        error: (err) =>
          this.handleError('Erro ao fazer upload dos arquivos', err),
      });
  }

  downloadArquivo(arquivo: ArquivoAdmin): void {
    this.adminService.downloadArquivo(arquivo.id).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = arquivo.nome;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  // ---------------- Seleção ----------------
  isSelecionado(item: PastaAdmin | ArquivoAdmin): boolean {
    return this.itensSelecionados.includes(item);
  }

  toggleSelecao(item: PastaAdmin | ArquivoAdmin): void {
    this.isSelecionado(item)
      ? (this.itensSelecionados = this.itensSelecionados.filter(
          (i) => i !== item
        ))
      : this.itensSelecionados.push(item);
  }

  marcarTodos(): void {
    this.itensSelecionados = this.todosItens;
  }

  desmarcarTodos(): void {
    this.itensSelecionados = [];
  }

  inverterSelecao(): void {
    this.itensSelecionados = this.todosItens.filter(
      (item) => !this.isSelecionado(item)
    );
  }

  // ---------------- Exclusão ----------------
  abrirModalExcluir(item: PastaAdmin | ArquivoAdmin): void {
    this.itemParaExcluir = item;
    this.modalExcluirAberto = true;
  }

  fecharModalExcluir(): void {
    this.modalExcluirAberto = false;
    this.itemParaExcluir = null;
  }

  confirmarExclusao(): void {
    if (!this.itemParaExcluir) return;

    const request: Observable<void> = this.isPasta(this.itemParaExcluir)
      ? this.adminService.excluirPasta(this.itemParaExcluir.id)
      : this.adminService.excluirArquivo(this.itemParaExcluir.id);

    this.loading = true;
    request.subscribe({
      next: () => {
        this.recarregarConteudo();
        this.fecharModalExcluir();
        this.loading = false;
      },
      error: (err: any) => this.handleError('Erro ao excluir item', err),
    });
  }

  abrirModalExcluirSelecionados(): void {
    if (this.itensSelecionados.length > 0)
      this.modalExcluirSelecionadosAberto = true;
  }

  fecharModalExcluirSelecionados(): void {
    this.modalExcluirSelecionadosAberto = false;
  }

  confirmarExclusaoSelecionados(): void {
    if (!this.itensSelecionados.length) return;

    const pastasSelecionadas = this.itensSelecionados.filter(this.isPasta);
    const arquivosSelecionados = this.itensSelecionados.filter(
      (i) => !this.isPasta(i)
    );

    this.loading = true;
    const requests: Observable<any>[] = [];

    if (pastasSelecionadas.length) {
      const dto: PastaExcluirDTO = {
        idsPastas: pastasSelecionadas.map((p) => p.id),
        excluirConteudo: true,
      };
      requests.push(
        this.adminService.excluirPastasEmLote(dto).pipe(
          catchError((err) => {
            this.handleError('Erro ao excluir pastas em lote', err);
            return of(null);
          })
        )
      );
    }

    arquivosSelecionados.forEach((arquivo) =>
      requests.push(
        this.adminService.excluirArquivo((arquivo as ArquivoAdmin).id).pipe(
          catchError((err) => {
            this.handleError(
              `Erro ao excluir arquivo ${(arquivo as ArquivoAdmin).nome}`,
              err
            );
            return of(null);
          })
        )
      )
    );

    forkJoin(requests).subscribe({
      next: () => {
        this.itensSelecionados = [];
        this.recarregarConteudo();
        this.fecharModalExcluirSelecionados();
        this.loading = false;
      },
      error: (err: any) =>
        this.handleError('Erro ao excluir itens selecionados', err),
    });
  }

  // ---------------- Usuários ----------------
  carregarUsuarios(): void {
    this.usuarioService.listar().subscribe({
      next: (resposta: Paginacao<Usuario>) =>
        (this.usuarios = resposta.content || []),
      error: (err: any) => this.handleError('Erro ao carregar usuários', err),
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    const idx = this.usuariosSelecionados.findIndex((u) => u.id === usuario.id);
    idx === -1
      ? this.usuariosSelecionados.push(usuario)
      : this.usuariosSelecionados.splice(idx, 1);
  }


  // ---Fazer copia de arquivos para outra pasta
  // ---------------- Copiar Arquivo ----------------
// Variáveis novas
modalCopiarAberto = false;
arquivoParaCopiar: ArquivoAdmin | null = null;
pastaDestinoSelecionada: PastaAdmin | null = null;
pastasDisponiveisParaCopiar: PastaAdmin[] = [];

// ---------------- Modal Copiar Arquivo ----------------
abrirModalCopiar(arquivo: ArquivoAdmin): void {
  this.arquivoParaCopiar = arquivo;
  this.modalCopiarAberto = true;
  this.pastaDestinoSelecionada = null;
  this.loading = true;

  this.adminService.listarConteudoRaiz().subscribe({
    next: (conteudo: ConteudoPasta) => {
      // Carregamos apenas as pastas (sem arquivos)
      this.pastasDisponiveisParaCopiar = conteudo.pastas;
      this.loading = false;
    },
    error: (err) => {
      this.handleError('Erro ao carregar pastas para copiar', err);
      this.loading = false;
    }
  });
}

// Fechar modal de copiar arquivo
fecharModalCopiar(): void {
  this.modalCopiarAberto = false;
  this.arquivoParaCopiar = null;
  this.pastaDestinoSelecionada = null;
  this.pastasDisponiveisParaCopiar = [];
}

// Confirmar cópia
copiarArquivo(): void {
  if (!this.arquivoParaCopiar || !this.pastaDestinoSelecionada) return;

  this.loading = true;

  this.adminService
    .copiarArquivo(this.arquivoParaCopiar.id, this.pastaDestinoSelecionada.id)
    .subscribe({
      next: (arquivoCopiado) => {
        this.recarregarConteudo();
        this.fecharModalCopiar();
        this.loading = false;
      },
      error: (err) =>
        this.handleError(
          `Erro ao copiar arquivo ${this.arquivoParaCopiar?.nome}`,
          err
        ),
    });
}


// ---------------- Copiar Pasta ----------------
modalCopiarPastaAberto = false;
pastaParaCopiar: PastaAdmin | null = null;
pastaDestinoSelec: PastaAdmin | null = null;

abrirModalCopiarPasta(pasta: PastaAdmin): void {
  this.pastaParaCopiar = pasta;
  this.pastaDestinoSelec = null;
  this.modalCopiarPastaAberto = true;

  // Carregar pastas disponíveis para seleção como destino
  this.adminService.listarConteudoRaiz().subscribe({
    next: (conteudo) => {
      // Exclui a pasta que está sendo copiada da lista de destinos possíveis
      this.pastasDisponiveisParaCopiar = conteudo.pastas.filter(p => p.id !== pasta.id);
    },
    error: (err) => this.handleError('Erro ao carregar pastas para destino', err)
  });
}

fecharModalCopiarPasta(): void {
  this.modalCopiarPastaAberto = false;
  this.pastaParaCopiar = null;
  this.pastaDestinoSelec = null;
  this.pastasDisponiveisParaCopiar = [];
}

copiarPasta(): void {
    if (!this.pastaParaCopiar) return;

    const destinoId = this.pastaDestinoSelecionada ? this.pastaDestinoSelecionada.id.toString() : undefined;

    this.loading = true;
    this.adminService.copiarPasta(this.pastaParaCopiar.id.toString(), destinoId).subscribe({
      next: (novaPasta) => {
        // Atualiza conteúdo
        this.recarregarConteudo();

        // Fecha modal
        this.fecharModalCopiarPasta();

        // Remove loading
        this.loading = false;
        
      },
      error: (err) => this.handleError('Erro ao copiar pasta', err)
    });
  }

  // -- Mover ou recortar pasta --
  // ---------------- Mover Pasta ----------------
modalMoverPastaAberto = false;
pastaParaMover: PastaAdmin | null = null;
pastaDestinoMover: PastaAdmin | null = null;
pastasDisponiveisParaMover: PastaAdmin[] = [];

// Abrir modal de mover pasta
abrirModalMoverPasta(pasta: PastaAdmin): void {
  this.pastaParaMover = pasta;
  this.pastaDestinoMover = null;
  this.modalMoverPastaAberto = true;

  this.loading = true;
  // Carregar pastas disponíveis como destino (exceto a própria pasta e suas subpastas)
  this.adminService.listarConteudoRaiz().subscribe({
    next: (conteudo: ConteudoPasta) => {
      // Filtra a pasta que está sendo movida
      this.pastasDisponiveisParaMover = conteudo.pastas.filter(p => p.id !== pasta.id);
      this.loading = false;
    },
    error: (err) => {
      this.handleError('Erro ao carregar pastas para mover', err);
      this.loading = false;
    }
  });
}

// Fechar modal de mover pasta
fecharModalMoverPasta(): void {
  this.modalMoverPastaAberto = false;
  this.pastaParaMover = null;
  this.pastaDestinoMover = null;
  this.pastasDisponiveisParaMover = [];
}

// Confirmar mover pasta
moverPasta(): void {
  if (!this.pastaParaMover) return;

  const destinoId = this.pastaDestinoMover ? this.pastaDestinoMover.id : undefined;
  this.loading = true;

  this.adminService.moverPasta(this.pastaParaMover.id, destinoId).subscribe({
    next: (pastaMovida) => {
      this.recarregarConteudo();
      this.fecharModalMoverPasta();
      this.loading = false;
    },
    error: (err) => this.handleError('Erro ao mover pasta', err),
  });
}

// ---------------- Mover Arquivo ----------------
modalMoverArquivoAberto = false;
arquivoParaMover: ArquivoAdmin | null = null;
pastaDestinoArquivo: PastaAdmin | null = null;

abrirModalMoverArquivo(arquivo: ArquivoAdmin): void {
  this.arquivoParaMover = arquivo;
  this.pastaDestinoArquivo = null;
  this.modalMoverArquivoAberto = true;
  this.loading = true;

  // Carrega as pastas disponíveis para destino
  this.adminService.listarConteudoRaiz().subscribe({
    next: (conteudo: ConteudoPasta) => {
      this.pastasDisponiveisParaMover = conteudo.pastas;
      this.loading = false;
    },
    error: (err) => {
      this.handleError('Erro ao carregar pastas para mover', err);
      this.loading = false;
    }
  });
}

fecharModalMoverArquivo(): void {
  this.modalMoverArquivoAberto = false;
  this.arquivoParaMover = null;
  this.pastaDestinoArquivo = null;
  this.pastasDisponiveisParaMover = [];
}

moverArquivo(): void {
  if (!this.arquivoParaMover || !this.pastaDestinoArquivo) return;

  this.loading = true;

  this.adminService
    .moverArquivo(this.arquivoParaMover.id, this.pastaDestinoArquivo.id)
    .subscribe({
      next: (arquivoMovido) => {
        this.recarregarConteudo();
        this.fecharModalMoverArquivo();
        this.loading = false;
      },
      error: (err) =>
        this.handleError(
          `Erro ao mover arquivo ${this.arquivoParaMover?.nome}`,
          err
        ),
    });
}









  // ---------------- Helpers ----------------
  isFolder(item: PastaAdmin | ArquivoAdmin | null): boolean {
    return !!item && 'subPastas' in item;
  }

  isPasta(item: PastaAdmin | ArquivoAdmin): item is PastaAdmin {
    return (item as PastaAdmin).subPastas !== undefined;
  }

  getNomeItem(item: PastaAdmin | ArquivoAdmin | null): string {
    return item ? (this.isPasta(item) ? item.nomePasta : item.nome) : '';
  }

  get todosItens(): (PastaAdmin | ArquivoAdmin)[] {
    return [...this.pastas, ...this.arquivos];
  }

  private handleError(msg: string, err?: any): void {
    console.error(msg, err);
    this.loading = false;
  }
}
