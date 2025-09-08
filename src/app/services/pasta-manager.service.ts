import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';

// Interface para um item de arquivo ou pasta (baseado no que a API retorna)
export interface FileItem {
  id: number;
  name: string;
  isFolder: boolean;
  parentId?: number;
  // O tamanho e a data virão de forma diferente, então vamos remover por enquanto.
}

@Injectable({
  providedIn: 'root',
})
export class PastaManager {
  private apiUrl = 'http://localhost:8082/api/pastas';

  constructor(private http: HttpClient) {}

  /**
   * Traz o conteúdo da pasta de nível superior (raiz)
   * e os mapeia para a interface do front-end.
   * @returns Observable com a lista de itens.
   */
  getTopLevelFolders(): Observable<FileItem[]> {
    return this.http.get<FileItem[]>(`${this.apiUrl}/arvore`).pipe(
      map((response) =>
        response.map((item) => ({
          id: item.id,
          name: item.name,
          isFolder: true, // Se for do endpoint top-level, é sempre pasta
          parentId: item.parentId,
        }))
      )
    );
  }

  /**
   * Traz o conteúdo de uma subpasta específica (incluindo subpastas e arquivos).
   * Nota: A sua API só retorna pastas em `subpastas/{id}`.
   * Você precisará de outro endpoint na sua API para trazer os arquivos da pasta também.
   * Vou assumir que existe um endpoint `/files/by-pasta/{id}` para os arquivos.
   *
   * @param pastaId O ID da pasta pai.
   * @returns Observable com a lista de itens (subpastas e arquivos).
   */
  getFolderContent(pastaId: number): Observable<FileItem[]> {
    // Exemplo de como usar forkJoin se você precisar de dois endpoints para Pastas e Arquivos
    const pastas$ = this.http.get<FileItem[]>(
      `${this.apiUrl}/subpastas/${pastaId}`
    );
    const arquivos$ = this.http.get<any[]>(
      `${this.apiUrl}/files/by-pasta/${pastaId}`
    ); // Este endpoint não existe no seu controller, é um exemplo.

    return forkJoin([pastas$, arquivos$]).pipe(
      map(([pastas, arquivos]) => {
        const pastaItems = pastas.map((item) => ({
          id: item.id,
          name: item.name,
          isFolder: true,
          parentId: item.parentId,
        }));
        // Mapeie os arquivos para a interface FileItem
        const arquivoItems = arquivos.map((item) => ({
          id: item.id,
          name: item.name,
          isFolder: false,
          size: item.size,
          modifiedDate: item.modifiedDate,
          parentId: item.parentId,
        }));
        return [...pastaItems, ...arquivoItems];
      })
    );
  }
}
