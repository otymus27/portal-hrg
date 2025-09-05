package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PastaCompletaDTO(
        Long id,
        String nomePasta,
        String caminhoCompleto,
        long tamanhoTotalBytes, // Tamanho total de todos os arquivos na pasta e subpastas
        long totalItens, // Total de arquivos + subpastas
        LocalDateTime dataCriacao,
        List<PastaCompletaDTO> subpastas,
        List<ArquivoDTO> arquivos
) {
    public PastaCompletaDTO(Pasta pasta) {
        this(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                calcularTamanhoTotal(pasta), // Calcula o tamanho total da pasta e de seus arquivos
                calcularTotalItens(pasta), // Calcula a contagem total de itens
                pasta.getDataCriacao(),
                pasta.getSubpastas().stream()
                        .map(PastaCompletaDTO::new)
                        .collect(Collectors.toList()),
                pasta.getArquivos().stream()
                        .map(ArquivoDTO::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    // Métodos auxiliares para cálculo
    private static long calcularTamanhoTotal(Pasta pasta) {
        long tamanhoArquivos = pasta.getArquivos().stream()
                .mapToLong(Arquivo::getTamanhoBytes)
                .sum();

        long tamanhoSubpastas = pasta.getSubpastas().stream()
                .mapToLong(PastaCompletaDTO::calcularTamanhoTotal)
                .sum();

        return tamanhoArquivos + tamanhoSubpastas;
    }

    private static long calcularTotalItens(Pasta pasta) {
        long totalArquivos = pasta.getArquivos().size();
        long totalSubpastas = pasta.getSubpastas().size();

        return totalArquivos + totalSubpastas;
    }
}
