import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { PastaManager } from '../../services/pasta-manager.service';

// Interface para definir a estrutura de um item (pasta ou arquivo)
interface FileItem {
  id: string;
  name: string;
  isFolder: boolean;
  parentId: string | null;
  size: string | null;
  modifiedDate: Date;
  isSelected?: boolean;
}

@Component({
  selector: 'app-pasta',
  imports: [
    CommonModule,
    FormsModule, // <-- Add it to the imports array
  ],
  templateUrl: './pasta.component.html',
  styleUrls: ['./pasta.component.scss'],
})
export class PastaComponent implements OnInit {
   // Substitua a simulação de dados pela lógica real
  // private allItems: FileItem[] = [...];

  // O ID da pasta atual será um número (Long)
  currentFolderId: number | null = null;
  currentItems: FileItem[] = [];
  breadcrumbs: FileItem[] = [];

  hasSelection: boolean = false;
  isSingleSelection: boolean = false;

  // Injetamos o serviço no construtor
  constructor(private pastaManagerService: PastaManager) {}

  ngOnInit(): void {
    this.loadFolder(null); // Carrega a pasta raiz ao iniciar
  }

  private loadFolder(folderId: number | null) {
    this.currentFolderId = folderId;

    if (folderId === null) {
      // Carrega pastas de nível superior se o ID for nulo
      this.pastaManagerService.getTopLevelFolders().subscribe(
        (items) => {
          //this.currentItems = items;
          this.updateBreadcrumbs();
          this.checkSelection();
        },
        (error) => console.error('Erro ao carregar pastas de nível superior:', error)
      );
    } else {
      // Carrega o conteúdo de uma subpasta específica
      this.pastaManagerService.getFolderContent(folderId).subscribe(
        (items) => {
          //this.currentItems = items;
          this.updateBreadcrumbs();
          this.checkSelection();
        },
        (error) => console.error('Erro ao carregar o conteúdo da pasta:', error)
      );
    }
  }

  private updateBreadcrumbs() {
    // Esta lógica agora deve ser mais inteligente, já que os dados não estão em um array único
    // A melhor abordagem é ter um serviço que, dado um ID, retorna a hierarquia até a raiz.
    // Por enquanto, vamos manter uma versão simplificada.
    this.breadcrumbs = [];
    if (this.currentFolderId) {
        // Se o currentFolderId existe, significa que não estamos na raiz.
        // Você precisará buscar o caminho da API.
        // A sua API atual não tem um endpoint para isso, então
        // este método não funcionará como o esperado até que a API seja adaptada.
        // Por enquanto, ele apenas mostra a pasta atual.
        //const currentFolder = this.currentItems.find(item => item.id === this.currentFolderId && item.isFolder);
        // if (currentFolder) {
        //     this.breadcrumbs.push(currentFolder);
        // }
    }
  }

  onItemDoubleClick(item: FileItem) {
    if (item.isFolder) {
      //this.loadFolder(item.id);
    }
  }

  goToRoot() {
    this.loadFolder(null);
  }

  goToFolder(folderId: number) {
    this.loadFolder(folderId);
  }

  checkSelection() {
    const selectedItems = this.currentItems.filter((item) => item.isSelected);
    this.hasSelection = selectedItems.length > 0;
    this.isSingleSelection = selectedItems.length === 1;
  }

  toggleAllSelection(event: any) {
    const isChecked = event.target.checked;
    this.currentItems.forEach((item) => (item.isSelected = isChecked));
    this.checkSelection();
  }

  // Ações que chamarão os métodos do service
  onUpload() {
    console.log('Botão de upload clicado!');
    // Lógica para abrir o seletor de arquivo e chamar o service
    // this.fileManagerService.uploadFile(...).subscribe(...)
  }

  onCreateFolder() {
    console.log('Botão de nova pasta clicado!');
    // Lógica para criar uma nova pasta chamando o service
    // this.fileManagerService.createFolder(...).subscribe(...)
  }

  onDownload() {
    const selectedItems = this.currentItems.filter((item) => item.isSelected);
    console.log('Download dos itens:', selectedItems);
    // Lógica para baixar arquivos ou pastas selecionados
    // this.fileManagerService.downloadFile(...).subscribe(...)
  }

  onRename(item?: FileItem) {
    console.log('Renomeando item:', item);
    // Lógica para renomear item
    // this.fileManagerService.renameItem(...).subscribe(...)
  }
  
  onCopy(item?: FileItem) {
    console.log('Copiando item:', item);
    // Lógica para copiar item
    // this.fileManagerService.copyItem(...).subscribe(...)
  }

  onDelete(item?: FileItem) {
    // if (item) {
    //   this.pastaManagerService.deleteItems([item.id]).subscribe(
    //     () => this.loadFolder(this.currentFolderId), // Recarrega a pasta ao excluir
    //     (error) => console.error('Erro ao excluir item:', error)
    //   );
    // } else {
    //   const selectedItems = this.currentItems.filter((i) => i.isSelected);
    //   const itemIds = selectedItems.map(i => i.id);
    //   this.pastaManagerService.deleteItems(itemIds).subscribe(
    //     () => this.loadFolder(this.currentFolderId),
    //     (error) => console.error('Erro ao excluir múltiplos itens:', error)
    //   );
    // }
  }
}
