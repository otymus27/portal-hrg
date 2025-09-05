package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;

import java.util.List;

public record ItemDTO(
        Long id,
        String nome,
        boolean diretorio,
        List<ItemDTO> filhos,
        Double tamanho,
        Integer contagem,
        String dataCriacao
) {
    public static ItemDTO fromPasta(Pasta pasta) {
        int contagem = (pasta.getArquivos() != null ? pasta.getArquivos().size() : 0)
                + (pasta.getSubpastas() != null ? pasta.getSubpastas().size() : 0);
        return new ItemDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                true,
                null,
                null,
                contagem,
                pasta.getDataCriacao().toString()
        );
    }

    public static ItemDTO fromArquivo(Arquivo arquivo) {
        return new ItemDTO(
                arquivo.getId(),
                arquivo.getNomeArquivo(),
                false,
                null,
                (double) arquivo.getTamanhoBytes(),
                null,
                arquivo.getDataUpload().toString()
        );
    }
}
