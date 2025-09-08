import { Component, inject } from '@angular/core';
import { ResetSenhaRequest } from '../../models/reset-senha-request';
import { RecuperarSenhaService } from '../../services/recuperar-senha.service';
import { CommonModule } from '@angular/common'; // ✅ Importe CommonModule
import { FormsModule } from '@angular/forms'; // ✅ Importe FormsModule
import { Router } from '@angular/router';

@Component({
  selector: 'app-redefinicao-senha',
  imports: [
    CommonModule, // ✅ Adicione-o aqui
    FormsModule, // ✅ Adicione-o aqui
  ],
  templateUrl: './redefinicao-senha.component.html',
  styleUrl: './redefinicao-senha.component.scss',
})
export class RedefinicaoSenhaComponent {
  id: number | null = null;
  username: string = '';
  senhaProvisoria: string = '';
  novaSenha: string = '';
  mensagem: string | null = null;
  private recuperarSenhaService = inject(RecuperarSenhaService);
  private router = inject(Router);

  redefinirSenha(): void {
    if (this.username !== null && this.senhaProvisoria && this.novaSenha) {
      const dto: ResetSenhaRequest = {
        username: this.username,
        senhaProvisoria: this.senhaProvisoria,
        novaSenha: this.novaSenha,
      };
      this.recuperarSenhaService.atualizarSenha(dto).subscribe({
        next: (response) => {
          // ✅ Exibe a mensagem de sucesso para o usuário
          this.mensagem =
            'Sua senha foi redefinida com sucesso. Redirecionando para a página de login...';

          // ✅ Redireciona após um pequeno atraso para que o usuário possa ler a mensagem
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 3000); // 2 segundos de atraso
        },
        error: (err) => {
          if (err.error && err.error.message) {
            this.mensagem = err.error.message;
          } else {
            this.mensagem = 'Ocorreu um erro ao tentar redefinir a senha.';
          }
        },
      });
    }
  }

  // ✅ Método para cancelar e voltar para a tela de login
  cancelar(): void {
    this.router.navigate(['/login']);
  }
}
