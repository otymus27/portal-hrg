package br.com.carro.repositories;

import br.com.carro.entities.Usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Override
    Page<Usuario> findAll(Pageable pageable);

    // Para busca com filtro por modelo (case insensitive)
    Page<Usuario> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // ✅ Esta query é CRUCIAL para carregar as roles junto com o usuário
    //@Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    //Optional<Usuario> findByUsername(String username);



    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);


    // Buscar usuário pelo username
    Optional<Usuario> findByUsername(String username);

    // Verificar se existe username (para validações de duplicidade)
    boolean existsByUsername(String username);

    // Buscar usuários que tenham uma role específica
    List<Usuario> findByRolesNome(String nome);

    // Buscar usuários por username + role
    List<Usuario> findByUsernameAndRolesNome(String username, String nome);

    // Verificar se existe usuário com uma role específica
    boolean existsByRolesNome(String nome);


    boolean existsByUsernameAndRolesNome(String username, String admin);
}
