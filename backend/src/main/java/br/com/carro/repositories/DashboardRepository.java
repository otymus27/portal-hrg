package br.com.carro.repositories;

import br.com.carro.entities.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Arquivo, Long> {

    // ========================
    // MÉTRICAS GERAIS
    // ========================

    /**
     * Conta o total de arquivos armazenados no sistema.
     */
    @Query("SELECT COUNT(a) FROM Arquivo a")
    long contarTotalArquivos();

    /**
     * Conta o total de pastas cadastradas no sistema.
     * Aqui usamos uma query separada para Pasta pois o Repository é baseado em Arquivo.
     */
    @Query("SELECT COUNT(p) FROM Pasta p")
    long contarTotalPastas();

    /**
     * Soma o tamanho de todos os arquivos em bytes.
     */
    @Query("SELECT COALESCE(SUM(a.tamanho), 0) FROM Arquivo a")
    long somarEspacoTotalBytes();

    // ========================
    // SÉRIE TEMPORAL
    // ========================

    /**
     * Retorna a quantidade de uploads agrupados por data (últimos N dias).
     * Útil para gerar gráfico de evolução de uploads.
     */
    @Query("SELECT a.dataUpload, COUNT(a) " +
            "FROM Arquivo a " +
            "WHERE a.dataUpload >= :dataInicial " +
            "GROUP BY a.dataUpload " +
            "ORDER BY a.dataUpload ASC")
    List<Object[]> contarUploadsPorDia(@Param("dataInicial") LocalDateTime dataInicial);

    // ========================
    // RANKINGS DE USUÁRIOS
    // ========================

    /**
     * Retorna os top N usuários que mais enviaram arquivos.
     * Resultado: lista de Object[] -> [0]=nomeUsuario, [1]=quantidadeArquivos
     */
    @Query("SELECT a.criadoPor.username, COUNT(a) " +
            "FROM Arquivo a " +
            "GROUP BY a.criadoPor.username " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> topUsuariosPorUpload();

    /**
     * Retorna os top N usuários que mais consumiram espaço (em bytes).
     * Resultado: lista de Object[] -> [0]=nomeUsuario, [1]=totalBytes
     */
    @Query("SELECT a.criadoPor.username, COALESCE(SUM(a.tamanho), 0) " +
            "FROM Arquivo a " +
            "GROUP BY a.criadoPor.username " +
            "ORDER BY SUM(a.tamanho) DESC")
    List<Object[]> topUsuariosPorEspaco();

    // ========================
    // DISTRIBUIÇÃO POR TIPO
    // ========================

    /**
     * Retorna a quantidade de arquivos agrupados por tipo/extensão.
     * Resultado: lista de Object[] -> [0]=extensao, [1]=quantidade
     */
    @Query("SELECT a.tipoMime, COUNT(a) " +
            "FROM Arquivo a " +
            "GROUP BY a.tipoMime " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> distribuicaoPorTipo();

    /**
     * Retorna o consumo de espaço agrupado por tipo/extensão.
     * Resultado: lista de Object[] -> [0]=extensao, [1]=totalBytes
     */
    @Query("SELECT a.tipoMime, COALESCE(SUM(a.tamanho), 0) " +
            "FROM Arquivo a " +
            "GROUP BY a.tipoMime " +
            "ORDER BY SUM(a.tamanho) DESC")
    List<Object[]> topTiposPorEspaco();

    // Usuários que logaram hoje
    @Query(value = "SELECT COUNT(DISTINCT username) FROM tb_login_audit WHERE DATE(data_login) = CURRENT_DATE", nativeQuery = true)
    long contarUsuariosLogaramHoje();

    // Usuários ativos agora (últimos 5 minutos, por exemplo)
    @Query(value = "SELECT COUNT(DISTINCT username) FROM tb_login_audit WHERE data_login >= (NOW() - INTERVAL 5 MINUTE)", nativeQuery = true)
    long contarUsuariosAtivosAgora();
}
