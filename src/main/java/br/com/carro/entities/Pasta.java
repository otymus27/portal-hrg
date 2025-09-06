package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "tb_pasta")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome_pasta", nullable = false)
    private String nomePasta;

    @Column(name = "caminho_completo", nullable = false)
    private String caminhoCompleto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_pai_id")
    @JsonIgnore
    private Pasta pastaPai;

    @OneToMany(mappedBy = "pastaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Pasta> subPastas;

    @OneToMany(mappedBy = "pasta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Arquivo> arquivos;

    @CreatedDate
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    private LocalDateTime dataAtualizacao;

    @Column(name = "is_publico")
    private boolean publica;

    @CreatedBy
    // Dentro de Pasta.java, logo após data_criacao
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_permissao_pasta",
            joinColumns = @JoinColumn(name = "pasta_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonIgnore
    private Set<Usuario> usuariosComPermissao;
}