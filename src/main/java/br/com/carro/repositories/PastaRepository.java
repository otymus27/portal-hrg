package br.com.carro.repositories;

import br.com.carro.entities.Pasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PastaRepository extends JpaRepository<Pasta, Long> {

    /**
     * Busca uma pasta pelo seu ID, garantindo que o caminho do disco seja único.
     * Pode ser útil para verificar a existência de uma pasta.
     * @param caminhoCompleto O caminho absoluto da pasta no sistema de arquivos.
     * @return Um Optional contendo a Pasta encontrada ou vazio.
     */
    Optional<Pasta> findByCaminhoCompleto(String caminhoCompleto);

    /**
     * Busca uma pasta top-level (sem pai) pelo nome.
     * Ideal para encontrar pastas principais da área pública.
     * @param nomePasta O nome da pasta.
     * @return Um Optional contendo a Pasta encontrada ou vazio.
     */
    Optional<Pasta> findByNomePastaAndPastaPaiIsNull(String nomePasta);

    //✅ Novo método para buscar pastas de nível raiz
    List<Pasta> findByPastaPaiIsNull();
}