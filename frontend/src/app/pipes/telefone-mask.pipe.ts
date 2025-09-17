import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'telefoneMask', 
  standalone: true
})
export class TelefoneMaskPipe implements PipeTransform {
  transform(value: string | undefined): string {
    if (!value) return '';
    const v = value.replace(/\D/g, '');
    if (v.length === 11) return v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    if (v.length === 10) return v.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    return value;
  }
}
