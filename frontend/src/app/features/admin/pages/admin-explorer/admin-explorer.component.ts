import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  PastaAdmin,
  ArquivoAdmin,
  ConteudoPasta,
  PastaExcluirDTO,
  UsuarioResumoDTO,
  PastaPermissaoAcaoDTO,
} from '../../services/admin.service';
import { UsuarioService } from '../../../../services/usuario.service';
import { Usuario } from '../../../../models/usuario';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Paginacao } from '../../../../models/paginacao';
import { ModalUsuarioComponent } from '../modal-usuario/modal-usuario.component';
import { ErrorMessage } from '../../../../services/usuario.service';
import { ToastService, ToastMessage } from '../../../../services/toast.service';

@Component({
  selector: 'app-admin-explorer',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule, ModalUsuarioComponent],
  templateUrl: './admin-explorer.component.html',
  styleUrls: ['./admin-explorer.component.scss'],
})
export class AdminExplorerComponent implements OnInit {
  // ---------------- Propriedades ----------------
  // Conteúdo
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
  modalPermissaoAberto = false;

  // CRUD
  novoNomePasta = '';
  itemParaRenomear: PastaAdmin | ArquivoAdmin | null = null;
  novoNomeItem = '';
  itemParaExcluir: PastaAdmin | ArquivoAdmin | null = null;

  // Usuários
  usuarios: Usuario[] = []; // Lista de todos os usuários
  usuariosSelecionados: Usuario[] = []; // Lista de usuários para permissão na nova pasta

  // ✅ Novo: Variável para rastrear qual modal está ativo
  modalAtivo: 'criarPasta' | 'permissao' | null = null;

  // Permissões
  usuariosComPermissao: UsuarioResumoDTO[] = [];
  usuariosDisponiveis: Usuario[] = [];
  private usuariosIniciaisComPermissao: UsuarioResumoDTO[] = [];
  usuariosComPermissaoIds: number[] = [];

  // Pasta para permissões
  pastaParaPermissao: PastaAdmin | null = null;

  // Seleção
  itensSelecionados: (PastaAdmin | ArquivoAdmin)[] = [];

  // Upload múltiplo
  arquivosParaUpload: File[] = [];

  modalSelecionarUsuarioAberto = false;

  constructor(
    private adminService: AdminService,
    private usuarioService: UsuarioService,
    public toastService: ToastService
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
    if (!this.novoNomePasta) return;

    const body = {
      nome: this.novoNomePasta,
      pastaPaiId: this.obterPastaAtual()?.id,
      // se não houver seleção, envia array vazio → backend resolve
      usuariosComPermissaoIds:
        this.usuariosSelecionados.length > 0
          ? this.usuariosSelecionados.map((u) => u.id)
          : [],
    };

    this.adminService.criarPasta(body).subscribe({
      next: () => {
        this.toastService.showSuccess('Pasta criada com sucesso!');
        this.recarregarConteudo();
        this.fecharModalCriarPasta();
      },
      error: (err: ErrorMessage) =>
        this.handleError('Erro ao criar pasta', err),
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

    const novoNome = this.novoNomeItem.trim();

    // Nome atual normalizado
    const nomeAtual = this.isPasta(this.itemParaRenomear)
      ? (this.itemParaRenomear.nomePasta || '').trim()
      : (this.itemParaRenomear.nome || '').trim();

    // Se não mudou (ignorando case), não chama o back-end
    if (nomeAtual.toLowerCase() === novoNome.toLowerCase()) {
      this.toastService.showInfo('O nome não foi alterado.');
      this.fecharModalRenomear();
      return;
    }

    const request: Observable<any> = this.isPasta(this.itemParaRenomear)
      ? this.adminService.renomearPasta(this.itemParaRenomear.id, novoNome)
      : this.adminService.renomearArquivo(this.itemParaRenomear.id, novoNome); // se tiver similar para arquivo

    request.subscribe({
      next: () => {
        this.toastService.showSuccess('Nome alterado com sucesso!');
        this.recarregarConteudo();
      },
      error: (err) => this.handleError('Erro ao renomear item', err),
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
      return this.handleError('Selecione um arquivo para fazer upload.');
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

  downloadArquivo(arquivo: ArquivoAdmin | null) {
    if (!arquivo) return;
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

  get todosItens(): (PastaAdmin | ArquivoAdmin)[] {
    return [...this.pastas, ...this.arquivos];
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
        this.toastService.showSuccess('Exclusão feita com sucesso!');
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
        this.toastService.showSuccess('Exclusão feita com sucesso!');
        this.itensSelecionados = [];
        this.recarregarConteudo();
        this.fecharModalExcluirSelecionados();
        this.loading = false;
      },
      error: (err: any) =>
        this.handleError('Erro ao excluir itens selecionados', err),
    });
  }

  //-----------------Copiar Arquivo-----------------
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
      },
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
        this.pastasDisponiveisParaCopiar = conteudo.pastas.filter(
          (p) => p.id !== pasta.id
        );
      },
      error: (err) =>
        this.handleError('Erro ao carregar pastas para destino', err),
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
  
    const destinoId = this.pastaDestinoSelecionada?.id?.toString();
  
    this.loading = true;
    this.adminService
      .copiarPasta(this.pastaParaCopiar.id.toString(), destinoId)
      .subscribe({
        next: (novaPasta) => {
          this.recarregarConteudo();
          this.fecharModalCopiarPasta();
          this.loading = false;
        },
        error: (err) => {
          this.handleError('Erro ao copiar pasta', err);
          this.loading = false;
        },
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
        this.pastasDisponiveisParaMover = conteudo.pastas.filter(
          (p) => p.id !== pasta.id
        );
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Erro ao carregar pastas para mover', err);
        this.loading = false;
      },
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

    const destinoId = this.pastaDestinoMover
      ? this.pastaDestinoMover.id
      : undefined;
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
      },
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

  // -------------Abrir arquivo clicando em cima dele --------------//
  abrirArquivo(arquivo: ArquivoAdmin | null) {
    if (!arquivo) return;
    this.adminService.abrirArquivo(arquivo.id).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank'); // abre em nova aba
    });
  }

  arquivoSelecionado: ArquivoAdmin | null = null;
  modalOpcoesArquivoAberto = false;

  abrirOpcoesArquivo(arquivo: ArquivoAdmin) {
    this.arquivoSelecionado = arquivo;
    this.modalOpcoesArquivoAberto = true;
  }

  fecharModalOpcoesArquivo() {
    this.modalOpcoesArquivoAberto = false;
    this.arquivoSelecionado = null;
  }

  // ---------------- Usuários ----------------
  usuariosExcluidosIds: number[] = [];

  carregarUsuarios(): void {
    this.adminService.listarUsuarios().subscribe({
      next: (resposta: Paginacao<Usuario>) => {
        this.usuarios = resposta.content || [];
      },
      error: (err: any) => {
        this.handleError('Erro ao carregar usuários', err);
      },
    });
  }

  selecionarUsuario(usuario: Usuario): void {
    const idx = this.usuariosSelecionados.findIndex((u) => u.id === usuario.id);
    idx === -1
      ? this.usuariosSelecionados.push(usuario)
      : this.usuariosSelecionados.splice(idx, 1);
  }

  public isUsuarioSelecionado(usuario: Usuario): boolean {
    return this.usuariosSelecionados.some((u) => u.id === usuario.id);
  }

  // ---------------- Permissões ----------------
  // ✅ Novo: Método único para abrir o modal de seleção de usuário
  abrirModalSelecionarUsuario(modal: 'criarPasta' | 'permissao') {
    this.modalAtivo = modal;
    // Define os usuários que já foram selecionados para serem excluídos da lista
    this.usuariosExcluidosIds =
      modal === 'criarPasta'
        ? this.usuariosSelecionados.map((u) => u.id)
        : this.usuariosComPermissao.map((u) => u.id);

    this.modalSelecionarUsuarioAberto = true;
  }

  // ✅ Refatore o método para aceitar "Usuario" ou "null"
  onUsuarioSelecionado(usuario: Usuario | null) {
    // Adiciona a verificação de nulo
    if (usuario) {
      if (this.modalAtivo === 'criarPasta') {
        this.usuariosSelecionados.push(usuario);
      } else if (this.modalAtivo === 'permissao') {
        this.usuariosComPermissao.push(usuario);
      }
    }

    this.modalSelecionarUsuarioAberto = false;
    this.modalAtivo = null;
  }

  abrirModalPermissao(pasta: PastaAdmin): void {
    this.pastaParaPermissao = pasta;
    this.loading = true;

    this.adminService.listarUsuariosPorPasta(pasta.id).subscribe({
      // ✅ Corrigido: adicionado o tipo `UsuarioResumoDTO[]` ao parâmetro
      next: (usuariosPermitidos: UsuarioResumoDTO[]) => {
        this.usuariosComPermissao = usuariosPermitidos;
        this.usuariosIniciaisComPermissao = [...usuariosPermitidos];
        this.atualizarListaDeDisponiveis();
        this.modalPermissaoAberto = true;
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Erro ao carregar usuários da pasta', err);
        this.loading = false;
      },
    });
  }

  // ✅ Método para remover um usuário da lista de selecionados
  removerUsuarioSelecionado(usuario: Usuario) {
    this.usuariosSelecionados = this.usuariosSelecionados.filter(
      (u) => u.id !== usuario.id
    );
  }

  adicionarUsuarioPermissao(usuario: Usuario): void {
    this.usuariosComPermissao.push({
      id: usuario.id,
      username: usuario.username,
    });
    this.atualizarListaDeDisponiveis();
  }

  removerUsuarioPermissao(usuario: UsuarioResumoDTO): void {
    this.usuariosComPermissao = this.usuariosComPermissao.filter(
      (u) => u.id !== usuario.id
    );
    this.atualizarListaDeDisponiveis();
  }

  private atualizarListaDeDisponiveis(): void {
    this.usuariosDisponiveis = this.usuarios.filter(
      (u) => !this.usuariosComPermissao.some((p) => p.id === u.id)
    );
    this.usuariosComPermissaoIds = this.usuariosComPermissao.map((u) => u.id);
  }

  atualizarPermissoes(): void {
    if (!this.pastaParaPermissao || !this.pastaParaPermissao.id) {
      this.toastService.showError(
        'ID da pasta para permissões não encontrado.'
      );
      return;
    }

    const idsAtuais = new Set(this.usuariosComPermissao.map((u) => u.id));
    const idsIniciais = new Set(
      this.usuariosIniciaisComPermissao.map((u) => u.id)
    );

    const adicionarUsuariosIds = this.usuariosComPermissao
      .filter((u) => !idsIniciais.has(u.id))
      .map((u) => u.id);

    const removerUsuariosIds = this.usuariosIniciaisComPermissao
      .filter((u) => !idsAtuais.has(u.id))
      .map((u) => u.id);

    // ⚠️ Se não houve mudanças, não chama o backend
    if (adicionarUsuariosIds.length === 0 && removerUsuariosIds.length === 0) {
      this.toastService.showInfo('Nenhuma alteração de permissão foi feita.');
      this.fecharModalPermissao();
      return;
    }

    const dto: PastaPermissaoAcaoDTO = {
      pastaId: this.pastaParaPermissao.id,
      adicionarUsuariosIds,
      removerUsuariosIds,
    };

    this.loading = true;
    this.adminService.atualizarPermissoesAcao(dto).subscribe({
      next: () => {
        this.toastService.showSuccess('Permissões atualizadas com sucesso!');
        this.recarregarConteudo();
        this.fecharModalPermissao();
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Erro ao atualizar permissões', err);
        this.loading = false;
      },
    });
  }

  fecharModalPermissao(): void {
    this.modalPermissaoAberto = false;
    this.pastaParaPermissao = null;
    this.usuariosComPermissao = [];
    this.usuariosDisponiveis = [];
    this.usuariosIniciaisComPermissao = [];
  }

  // ---------------- Helpers ----------------
  isPasta(item: PastaAdmin | ArquivoAdmin): item is PastaAdmin {
    return (item as PastaAdmin).subPastas !== undefined;
  }

  getNomeItem(item: PastaAdmin | ArquivoAdmin | null): string {
    return item ? (this.isPasta(item) ? item.nomePasta : item.nome) : '';
  }

  private handleError(mensagemPadrao: string, err?: any): void {
    console.error(mensagemPadrao, err);

    let errorMessage = mensagemPadrao;

    // Caso 1: Angular entregou um HttpErrorResponse com o JSON dentro de err.error
    if (
      err &&
      err.error &&
      typeof err.error === 'object' &&
      'status' in err.error
    ) {
      const backend = err.error as ErrorMessage;
      errorMessage = `Erro (${backend.status}${
        backend.error ? ' - ' + backend.error : ''
      }): ${backend.message || mensagemPadrao}`;
    }
    // Caso 2: Já é ErrorMessage (como no criarPasta que funcionava antes)
    else if (err && typeof err === 'object' && 'status' in err) {
      const backend = err as ErrorMessage;
      errorMessage = `Erro (${backend.status}${
        backend.error ? ' - ' + backend.error : ''
      }): ${backend.message || mensagemPadrao}`;
    }
    // Fallback
    else {
      errorMessage = mensagemPadrao;
    }

    this.toastService.showError(errorMessage);
    this.loading = false;
  }
}
