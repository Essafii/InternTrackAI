import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ClassementEntry } from '../../core/models';

@Component({
  selector: 'app-classement',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './classement.component.html',
  styleUrl: './classement.component.scss'
})
export class ClassementComponent implements OnInit {
  api = inject(ApiService);

  entries = signal<ClassementEntry[]>([]);
  loading = signal(true);
  error = signal('');
  filterEquipe = signal('');

  get equipes(): string[] {
    const set = new Set(this.entries().map(e => e.equipe).filter(Boolean));
    return Array.from(set);
  }

  get filtered(): ClassementEntry[] {
    const eq = this.filterEquipe();
    return eq ? this.entries().filter(e => e.equipe === eq) : this.entries();
  }

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.getClassement(this.filterEquipe() || undefined).subscribe({
      next: r => {
        this.entries.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du classement.');
        this.loading.set(false);
      }
    });
  }

  onFilterEquipe(val: string) {
    this.filterEquipe.set(val);
    this.load();
  }

  medalIcon(rang: number): string {
    if (rang === 1) return '🥇';
    if (rang === 2) return '🥈';
    if (rang === 3) return '🥉';
    return `${rang}`;
  }

  scoreBarColor(score: number): string {
    if (score >= 80) return 'var(--color-success, #28a745)';
    if (score >= 60) return 'var(--color-warning, #f0a500)';
    return 'var(--color-danger, #dc3545)';
  }

  risqueClass(risque: number): string {
    if (risque >= 70) return 'badge badge-danger';
    if (risque >= 40) return 'badge badge-warning';
    return 'badge badge-success';
  }
}
