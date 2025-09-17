// src/app/admin/admin-explorer/modal-usuario/modal-usuario.component.ts

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AdminService } from '../../services/admin.service';
import { Paginacao } from '../../../../models/paginacao';
import { Usuario } from '../../../../models/usuario';

@Component({
  selector: 'app-modal-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-usuario.component.html',
  styleUrls: ['./modal-usuario.component.scss'],
})
export class ModalUsuarioComponent implements OnInit {
  @Input() usuariosExcluidosIds: number[] = [];
  @Output() usuarioSelecionado = new EventEmitter<Usuario | null>();

  usuariosDisponiveis: Usuario[] = [];
  termoBusca: string = '';

  // ✅ Propriedades de paginação
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.buscarUsuarios();
  }

  // ✅ Método para buscar usuários com paginação
  buscarUsuarios(): void {
    this.adminService
      .listarUsuarios(this.termoBusca, this.currentPage, this.pageSize)
      .subscribe({
        next: (resposta: Paginacao<Usuario>) => {
          this.currentPage = resposta.number; // Atualiza a página atual a partir da resposta do backend
          this.totalPages = resposta.totalPages;
          this.totalElements = resposta.totalElements;
          const todosUsuarios = resposta.content || [];
          this.usuariosDisponiveis = todosUsuarios.filter(
            (u) => !this.usuariosExcluidosIds.includes(u.id)
          );
        },
        error: (err) => {
          console.error('Erro ao buscar usuários', err);
        },
      });
  }

  adicionarUsuario(usuario: Usuario): void {
    this.usuarioSelecionado.emit(usuario);
    this.fechar();
  }

  fechar(): void {
    this.usuarioSelecionado.emit(null);
  }

  // ✅ Lógica de navegação da paginação
  onPreviousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.buscarUsuarios();
    }
  }

  onNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.buscarUsuarios();
    }
  }
}