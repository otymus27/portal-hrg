export class Proprietario {

    // quando colocamos a ? em frente ao atributo é para não precisar inicializar o atributo e dizer que ele é opcional
  // ou seja, não precisa passar o id quando for criar uma nova marca
  // mas quando for editar, o id é obrigatório
  // se não colocar a ? o id é obrigatório em ambos os casos  
  id!: number;
  cpf!: string;
  nome!: string;
  telefone!: string;
}

