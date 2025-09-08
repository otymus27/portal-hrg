import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'cpfMask',
  standalone: true   // ✅ torna pipe standalone
})
export class CpfMaskPipe implements PipeTransform {
  transform(value: string | undefined): string {
    if (!value) return '';
    const v = value.replace(/\D/g, '');
    if (v.length !== 11) return value;
    return v.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }
}