package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_arquivo")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "caminho_armazenamento", nullable = false)
    private String caminhoArmazenamento;

    @Column(name = "tamanho_bytes")
    private Long tamanho;

    @CreatedDate
    @Column(name = "data_upload", nullable = false, updatable = false)
    private LocalDateTime dataUpload;

    @LastModifiedDate
    private LocalDateTime dataAtualizacao;

    @Column(name = "hash_arquivo", length = 64)
    private String hashArquivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_id")
    @JsonIgnore
    private Pasta pasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    @JsonIgnore
    private Usuario criadoPor;

    @Column(name = "is_publico")
    private boolean publico;
}