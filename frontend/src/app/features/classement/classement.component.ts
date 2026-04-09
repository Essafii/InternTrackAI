import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ClassementDto } from '../../core/models';

@Component({
  selector: 'app-classement',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './classement.component.html',
  styleUrl: './classement.component.scss'
})
export class ClassementComponent implements OnInit {
  api = inject(ApiService);

  entries      = signal<ClassementDto[]>([]);
  loading      = signal(true);
  error        = signal('');
  filterEquipe = signal('');

  get equipes(): string[] {
    const set = new Set(this.entries().map(e => e.equipe).filter(Boolean) as string[]);
    return Array.from(set);
  }

  get filtered(): ClassementDto[] {
    const eq = this.filterEquipe();
    return eq ? this.entries().filter(e => e.equipe === eq) : this.entries();
  }

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.getClassement(undefined, this.filterEquipe() || undefined).subscribe({
      next: r => { this.entries.set(r); this.loading.set(false); },
      error: () => { this.error.set('Erreur lors du chargement du classement.'); this.loading.set(false); }
    });
  }

  onFilterEquipe(val: string) { this.filterEquipe.set(val); this.load(); }

  medalIcon(rang: number): string {
    if (rang === 1) return '🥇';
    if (rang === 2) return '🥈';
    if (rang === 3) return '🥉';
    return `${rang}`;
  }

  scoreBarColor(score: number): string {
    if (score >= 14) return '#4caf50';
    if (score >= 10) return '#FFAE41';
    return '#D14600';
  }
}
