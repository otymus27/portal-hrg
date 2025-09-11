export interface Paginacao<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number; // Índice da página atual (corresponde a 'page' no RequestParam do backend)
    numberOfElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
    sort: {
        sorted: boolean;
        unsorted: boolean;
        empty: boolean;
    };
}
