import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Tache, Stagiaire } from '../../core/models';

@Component({
  selector: 'app-taches',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './taches.component.html',
  styleUrl: './taches.component.scss'
})
export class TachesComponent implements OnInit {
  api = inject(ApiService);

  taches = signal<Tache[]>([]);
  stagiaires = signal<Stagiaire[]>([]);
  loading = signal(true);
  error = signal('');
  total = signal(0);
  page = signal(0);
  readonly pageSize = 10;

  filterEtat = signal('');
  filterStagiaireId = signal<number | undefined>(undefined);

  updatingId = signal<number | null>(null);

  readonly etats: Array<{ value: string; label: string }> = [
    { value: '', label: 'Tous les états' },
    { value: 'A_FAIRE', label: 'À faire' },
    { value: 'EN_COURS', label: 'En cours' },
    { value: 'TERMINE', label: 'Terminé' },
    { value: 'EN_RETARD', label: 'En retard' }
  ];

  ngOnInit() {
    this.api.getStagiaires(0, 100).subscribe({
      next: r => this.stagiaires.set(r.content),
      error: () => {}
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.getTaches(
      this.page(),
      this.pageSize,
      this.filterStagiaireId(),
      this.filterEtat() || undefined
    ).subscribe({
      next: r => {
        this.taches.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des tâches.');
        this.loading.set(false);
      }
    });
  }

  onFilterEtat(val: string) {
    this.filterEtat.set(val);
    this.page.set(0);
    this.load();
  }

  onFilterStagiaire(val: string) {
    this.filterStagiaireId.set(val ? +val : undefined);
    this.page.set(0);
    this.load();
  }

  updateEtat(tache: Tache, newEtat: string) {
    if (tache.etat === newEtat) return;
    this.updatingId.set(tache.id);
    this.api.updateTacheEtat(tache.id, newEtat).subscribe({
      next: updated => {
        this.taches.update(list =>
          list.map(t => t.id === updated.id ? updated : t)
        );
        this.updatingId.set(null);
      },
      error: () => this.updatingId.set(null)
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  etatLabel(etat: string): string {
    const map: Record<string, string> = {
      A_FAIRE: 'À faire', EN_COURS: 'En cours', TERMINE: 'Terminé', EN_RETARD: 'En retard'
    };
    return map[etat] ?? etat;
  }

  etatClass(etat: string): string {
    const map: Record<string, string> = {
      A_FAIRE: 'badge badge-info',
      EN_COURS: 'badge badge-warning',
      TERMINE: 'badge badge-success',
      EN_RETARD: 'badge badge-danger'
    };
    return map[etat] ?? 'badge';
  }

  prioriteLabel(p: number): string {
    if (p >= 3) return 'Haute';
    if (p === 2) return 'Moyenne';
    return 'Basse';
  }

  prioriteClass(p: number): string {
    if (p >= 3) return 'badge badge-danger';
    if (p === 2) return 'badge badge-warning';
    return 'badge badge-info';
  }

  isOverdue(deadline: string): boolean {
    return new Date(deadline) < new Date();
  }
}
