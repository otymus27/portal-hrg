package br.com.carro.entities.DTO;

import java.util.List;

/**
 * DTO para representar os dados necessários para mover um arquivo.
 * O ID do arquivo é recebido como parte da URL no endpoint.
 * @param pastaDestinoId O ID da pasta para onde o arquivo será movido.
 */
// Para mover/copiar arquivos
public record ArquivoMoverCopiarDTO(
        List<Long> idsArquivos,
        Long pastaDestinoId,
        boolean copiar // true = copia, false = move
) {}