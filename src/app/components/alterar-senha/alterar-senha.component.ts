import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  AlterarSenhaRequest,
  RecuperarSenhaService,
} from '../../services/recuperar-senha.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-alterar-senha',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './alterar-senha.component.html',
  styleUrl: './alterar-senha.component.scss',
})
export class AlterarSenhaComponent {
  form: FormGroup;
  mensagem: string | null = null;
  erro: string | null = null;

  constructor(
    private fb: FormBuilder,
    private recuperarSenhaService: RecuperarSenhaService,
    private router: Router
  ) {
    this.form = this.fb.group({
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, Validators.minLength(6)]],
      confirmarSenha: ['', Validators.required],
    });
  }

  onSubmit(): void {
    this.mensagem = null;
    this.erro = null;

    if (this.form.value.novaSenha !== this.form.value.confirmarSenha) {
      this.erro = 'A nova senha e a confirmação não coincidem';
      return;
    }

    const dto: AlterarSenhaRequest = {
      senhaAtual: this.form.value.senhaAtual,
      novaSenha: this.form.value.novaSenha,
    };

    this.recuperarSenhaService.alterarSenha(dto).subscribe({
      next: (res) => {
        this.mensagem = res.mensagem;

        // ✅ Aguarda 1.5s para exibir mensagem e redireciona
        setTimeout(() => {
          this.router.navigate(['/admin/dashboard']);
        }, 1500);
      },
      error: (err) => this.erro = err.error.message || 'Erro ao alterar senha'
    });
  }

  onCancel(): void {
    this.router.navigate(['/admin/dashboard']);
  }
  
}
