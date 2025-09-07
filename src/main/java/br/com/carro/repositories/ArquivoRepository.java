package br.com.carro.repositories;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Interface de repositório para a entidade Arquivo.
 * Estende JpaRepository para operações básicas de CRUD e paginação.
 * <p>
 * Estende JpaSpecificationExecutor para permitir consultas dinâmicas
 * usando a API de Criteria do JPA (o que resolve o erro de `findAll` com Specification).
 */
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    Page<Arquivo> findByPasta(Pasta pasta, Pageable pageable);

    Page<Arquivo> findByPastaAndNomeArquivoEndingWithIgnoreCase(Pasta pasta, String extensao, Pageable pageable);


    default Page<Arquivo> findByPastaAndExtensaoIgnoreCase(Pasta pasta, String extensao, Pageable pageable) {
        if (!extensao.startsWith(".")) {
            extensao = "." + extensao;
        }
        return findByPastaAndNomeArquivoEndingWithIgnoreCase(pasta, extensao, pageable);
    }

    // 🔥 Novo método para buscar por intervalo de datas
    Page<Arquivo> findByPastaAndDataUploadBetween(Pasta pasta, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);


    List<Arquivo> findByPastaId(Long pastaId);


}