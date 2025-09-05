package br.com.carro.utilitarios;

import java.util.List;

public class PaginacaoResponse <T>{
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
