// ...
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // ✅ Importe CommonModule
import { FormsModule } from '@angular/forms'; // ✅ Importe FormsModule
import { MarcaService } from '../../../services/marca.service';
import { RelatorioService } from '../../../services/relatorio.service';
import { Marca } from '../../../models/marca';

@Component({
  selector: 'app-relatorio-marcas',
  imports: [
    CommonModule, // ✅ Adicione-o aqui
    FormsModule, // ✅ Adicione-o aqui
  ],
  templateUrl: './relatorio.marca.component.html',
})
export class MarcaComponentRelatorio implements OnInit {
  nomeFiltro: string = '';
  marcas: any[] = [];

  // ✅ Adicionando variáveis de estado para ordenação
  sortField: keyof Marca = 'id';
  sortDir: 'asc' | 'desc' = 'asc';

  //Variáveis para configuração para paginação
  page = 0;
  size = 5;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private marcaService: MarcaService,
    private relatorioService: RelatorioService
  ) {}

  // ✅ Remove a busca automática ao carregar a página
  ngOnInit(): void {
    // Agora a tela carrega vazia, esperando a ação do usuário
  }

  // ✅ Novo método: Limpa o filtro e busca todos os registros
  buscarTodosRegistros(): void {
    this.buscarMarcas();
    this.page = 0; // Volta para a primeira página
    this.nomeFiltro = ''; // Limpa o campo de filtro
  }

  // ✅ Novo método: Busca com base no valor do filtro
  buscarPersonalizada(): void {
    this.page = 0; // Volta para a primeira página ao fazer uma nova busca
    this.buscarMarcas();
    this.nomeFiltro = ''; // Limpa o campo de filtro
  }

  // ✅ Método que busca dados para a tabela, usando o MarcaService
  buscarMarcas(): void {
    this.marcaService
      .listar(
        this.nomeFiltro,
        this.page,
        this.size,
        this.sortField,
        this.sortDir
      )
      .subscribe((response: any) => {
        // ✅ Adicione esta linha de teste!
        console.log(
          'Valor do filtro "nomeFiltro" antes de chamar o serviço:',
          this.nomeFiltro
        );
        // Assume que o listar do service já faz a chamada para o seu backend
        this.marcas = response.content;

        // ✅ Atualiza o total de páginas com a resposta do backend
        this.totalPages = response.totalPages;
      });
  }

  // ✅ Navega para a próxima página
  proximaPagina(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      console.log('Navegando para a página:', this.page); // ✅ Adicione esta linha
      this.buscarMarcas();
    }
  }

  // ✅ Navega para a página anterior
  paginaAnterior(): void {
    if (this.page > 0) {
      this.page--;
      console.log('Navegando para a página:', this.page); // ✅ Adicione esta linha
      this.buscarMarcas();
    }
  }

  // ✅ Navega para uma página específica
  irParaPagina(page: number): void {
    this.page = page;
    console.log('Navegando para a página:', this.page); // ✅ Adicione esta linha
    this.buscarMarcas();
  }

  // ✅ Novo método para alternar a ordenação
  alternarOrdenacao(campo: keyof Marca): void {
    if (this.sortField === campo) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = campo;
      this.sortDir = 'asc';
    }
    this.buscarPersonalizada();
  }

  // ✅ Método que gera o relatório, usando o RelatorioService
  gerarRelatorioMarca(formato: string): void {
    // ✅ ADICIONE ESTA LINHA PARA TESTE
    console.log('Valor do filtro "nomeFiltro":', this.nomeFiltro);
    this.relatorioService
      .gerarRelatorioMarcas(
        formato,
        this.nomeFiltro,
        this.sortField,
        this.sortDir
      )
      .subscribe((response: Blob) => {
        // Lógica de download
        const blob = new Blob([response], { type: response.type });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `relatorio-marcas.${formato === 'xls' ? 'xlsx' : formato}`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
      });
  }
}
