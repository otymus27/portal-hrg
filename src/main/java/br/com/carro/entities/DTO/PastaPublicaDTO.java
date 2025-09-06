package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PastaPublicaDTO {
    private Long id;
    private String nome;
    private String caminhoCompleto;
    private LocalDateTime dataCriacao;
    private String nomeUsuarioCriador;
    private List<PastaPublicaDTO> subPastas = new ArrayList<>();
    private List<ArquivoDTO> arquivos = new ArrayList<>();

    public static PastaPublicaDTO fromEntity(Pasta pasta) {
        PastaPublicaDTO dto = new PastaPublicaDTO();
        dto.setId(pasta.getId());
        dto.setNome(pasta.getNomePasta());
        dto.setCaminhoCompleto(pasta.getCaminhoCompleto());
        dto.setDataCriacao(pasta.getDataCriacao());
        dto.setNomeUsuarioCriador(pasta.getCriadoPor() != null ? pasta.getCriadoPor().getUsername() : null);

        if (pasta.getArquivos() != null) {
            pasta.getArquivos().stream()
                    .filter(Arquivo::isPublico)
                    .forEach(arquivo -> dto.getArquivos().add(ArquivoDTO.fromEntity(arquivo)));
        }

        if (pasta.getSubPastas() != null) {
            pasta.getSubPastas().stream()
                    .filter(Pasta::isPublica)
                    .forEach(sub -> dto.getSubPastas().add(PastaPublicaDTO.fromEntity(sub)));
        }

        return dto;
    }
}
