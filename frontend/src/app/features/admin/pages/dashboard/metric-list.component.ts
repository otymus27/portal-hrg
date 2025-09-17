import { Component, Input } from '@angular/core';
import { CommonModule, NgForOf, NgIf } from '@angular/common';
import { MetricData } from '../../services/dashboard.service';

@Component({
  selector: 'app-metric-list',
  standalone: true,
  imports: [CommonModule, NgForOf, NgIf],
  templateUrl: './metric-list.component.html',
  styleUrls: ['./metric-list.component.scss'],
})
export class MetricListComponent {
  @Input() title!: string;
  @Input() data: MetricData[] = [];
  @Input() unit: string = '';

  // ---------------- Utils ----------------
  getMaxValue(): number {
    return this.data.length ? Math.max(...this.data.map(d => d.value)) : 1;
  }

  getBarWidth(value: number): number {
    return (value / this.getMaxValue()) * 100;
  }

  getBarColor(value: number): string {
    if (value < this.getMaxValue() * 0.3) return '#28a745'; // verde
    if (value < this.getMaxValue() * 0.7) return '#ffc107'; // amarelo
    return '#dc3545'; // vermelho
  }

  // ---------------- Sorting ----------------
  sortByValue(desc: boolean = true): void {
    this.data.sort((a, b) => desc ? b.value - a.value : a.value - b.value);
  }

  sortByLabel(): void {
    this.data.sort((a, b) => a.label.localeCompare(b.label));
  }
}
