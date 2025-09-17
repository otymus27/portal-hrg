package br.com.carro.entities.DTO;

import lombok.Data;

@Data
public class PastaFilterDTO {
    private String ordenarPor = "nome"; // "nome", "data", "tamanho"
    private boolean ordemAsc = true; // true = ascendente, false = descendente
    private String extensaoArquivo; // ex: "pdf", "docx", null = todos
    private Long tamanhoMinArquivo; // em bytes
    private Long tamanhoMaxArquivo; // em bytes
    private Integer profundidadeMax; // limite de níveis de subpastas, null = ilimitado
}
