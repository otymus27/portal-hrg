package br.com.carro.repositories;


import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    Optional<Arquivo> findByNomeArquivo(String nomeArquivo);

    // Método para listar todos os arquivos de uma pasta específica
    List<Arquivo> findByPastaId(Long pastaId);

    // Método para buscar arquivos pelo nome (ou parte dele)
    Page<Arquivo> findByNomeArquivoContainingIgnoreCase(String nomeArquivo, Pageable pageable);

    // Método para excluir todos arquivos de uma pasta
    @Modifying
    @Query("DELETE FROM Arquivo a WHERE a.pasta.id = :pastaId")
    void deleteAllByPastaId(@Param("pastaId") Long pastaId);

    //<T> ScopedValue<T> findByCaminhoArmazenamento(String string);

    List<Arquivo> findByPasta(Pasta pasta);
}