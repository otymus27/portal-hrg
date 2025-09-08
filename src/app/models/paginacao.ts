export interface Paginacao<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    page: number;
}
