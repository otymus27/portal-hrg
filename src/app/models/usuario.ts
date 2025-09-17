import { Role } from './role';

/**
 * Representa um usuário no sistema.
 * A senha só é necessária para cadastro e atualização.
 * Em operações de listagem/busca, o backend NÃO deve retornar a senha por segurança.
 */
export class Usuario {
  id!: number; // Opcional, pois não existe em novos usuários
  username!: string;
  nome!: string;
  roles!: Role[]; // ✅ Lista de proprietários
  // Opcional para manter compatibilidade com DTOs, mas **nunca retornado pelo backend** em GETs.
  // Será usado principalmente em UserCreateDTO ou UserUpdateDTO.
  password?: string;
}

// /**
//  * DTO (Data Transfer Object) para criação de um novo usuário.
//  * A senha é obrigatória neste contexto.
//  */
// export class UsuarioCreateDTO {
//   username!: string;
//   password!: string; // A senha pode ser opcional aqui se o backend tiver um processo de geração ou redefinição
//   roles!: Role[]; // ✅ Lista de proprietários
// }

// /**
//  * DTO para atualização de um usuário existente.
//  * A senha é opcional, permitindo atualizar outras informações sem mudar a senha.
//  */
// export class UsuarioUpdateDTO {
//   username!: string;
//   password?: string;
//   roles!: Role[]; // ✅ Lista de proprietários
// }
