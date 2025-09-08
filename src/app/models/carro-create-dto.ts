export class CarroCreateDTO {
    modelo!: string;
    cor!: string;
    ano!: number;
    marca!: { id: number };
    proprietarios!: { id: number }[];
}
