package br.com.carro.repositories;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.lang.ScopedValue;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface PastaRepository extends JpaRepository<Pasta, Long> {

    // ✅ Método para carregar a lista de pastas principais com subpastas e arquivos
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Page<Pasta> findByPastaPaiIsNull(Pageable pageable);

    // ✅ Método para carregar a lista de subpastas de uma pasta pai com subpastas e arquivos
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Page<Pasta> findByPastaPaiId(Long pastaPaiId, Pageable pageable);

    // ✅ Método para buscar uma única pasta por ID, carregando todas as suas relações
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Optional<Pasta> findById(Long id);

    // ✅ Método para buscar pastas principais por permissão
    Page<Pasta> findAllByIdIn(Set<Long> pastasPrincipaisAcessadas, Pageable pageable);

    Optional<Pasta> findByNomePastaAndPastaPai(String nomePasta, Pasta pastaPai);

    // Método para buscar arquivos pelo nome (ou parte dele)
    Page<Pasta> findByNomePastaContainingIgnoreCase(String nomePasta, Pageable pageable);

    // No seu PastaRepository.java

    @Query("SELECT DISTINCT p FROM Pasta p LEFT JOIN FETCH p.subpastas sp LEFT JOIN FETCH p.arquivos a WHERE p.id = :id")
    Optional<Pasta> findByIdWithChildrenAndFiles(@Param("id") Long id);

    Optional<Pasta> findByPastaPaiAndNomePasta(Pasta pastaPai, String nomeSubpasta);

    // Ensure this method exists and is correctly defined
    Optional<Pasta> findByCaminhoCompleto(String caminhoCompleto);

    List<Pasta> findByPastaPai(Pasta pastaPai);

    // Método para encontrar pastas por ID de pasta pai
    List<Pasta> findByPastaPaiId(Long pastaPaiId);

    // Método que encontra a pasta raiz, que é única.
    Optional<Pasta> findByPastaPaiIsNull();

    // Novo método para encontrar TODAS as pastas raiz.
    List<Pasta> findAllByPastaPaiIsNull();
}