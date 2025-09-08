import { Marca } from "./marca";
import { Proprietario } from "./proprietario";

export class Carro {

  // quando colocamos a ! em frente ao atributo é para não precisar inicializar o atributo
  id!: number;
  modelo!: string;   
  cor!: string;
  ano!: number  
  marca!: Marca;                     // ✅ Marca completa (id + nome)
  proprietarios!: Proprietario[];    // ✅ Lista de proprietários

  


}
