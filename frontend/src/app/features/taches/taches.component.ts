import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { TacheDto, StagiaireDto, EtatTache, TacheRequest } from '../../core/models';

@Component({
  selector: 'app-taches',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './taches.component.html',
  styleUrl: './taches.component.scss'
})
export class TachesComponent implements OnInit {
  api  = inject(ApiService);
  auth = inject(AuthService);
  fb   = inject(FormBuilder);

  taches     = signal<TacheDto[]>([]);
  stagiaires = signal<StagiaireDto[]>([]);
  loading    = signal(true);
  error      = signal('');
  success    = signal('');
  total      = signal(0);
  page       = signal(0);
  readonly pageSize = 10;

  filterEtat        = signal('');
  selectedStagiaireId = signal<number | null>(null);
  updatingId        = signal<number | null>(null);
  showForm          = signal(false);
  submitting        = signal(false);

  readonly etats: Array<{ value: EtatTache | ''; label: string }> = [
    { value: '',          label: 'Tous les états' },
    { value: 'A_FAIRE',   label: 'À faire' },
    { value: 'EN_COURS',  label: 'En cours' },
    { value: 'TERMINE',   label: 'Terminé' },
    { value: 'EN_RETARD', label: 'En retard' }
  ];

  form = this.fb.group({
    stagiaireId: [null as number | null, Validators.required],
    titre:       ['', Validators.required],
    description: [''],
    dateDebut:   [''],
    deadline:    [''],
    priorite:    [1],
    commentaire: ['']
  });

  role        = this.auth.currentUser()?.role;
  isRhOrAdmin = () => ['RH', 'ADMIN'].includes(this.role ?? '');

  ngOnInit() {
    const user = this.auth.currentUser();
    if (!user) return;

    if (this.isRhOrAdmin()) {
      this.api.getStagiaires(0, 100).subscribe({ next: r => this.stagiaires.set(r.content), error: () => {} });
      this.loading.set(false);
    } else if (user.role === 'ENCADRANT') {
      this.api.getStagiaires(0, 100).subscribe({
        next: r => {
          this.stagiaires.set(r.content);
          if (r.content.length > 0) { this.loadForStagiaire(r.content[0].id); }
          else { this.loading.set(false); }
        },
        error: () => this.loading.set(false)
      });
    } else if (user.role === 'STAGIAIRE') {
      this.api.getStagiaireByUserId(user.userId).subscribe({
        next: s => this.loadForStagiaire(s.id),
        error: () => this.loading.set(false)
      });
    }
  }

  loadForStagiaire(id: number) {
    this.selectedStagiaireId.set(id);
    this.load();
  }

  load() {
    const sid = this.selectedStagiaireId();
    if (!sid) { this.loading.set(false); return; }
    this.loading.set(true);
    this.api.getTachesByStagiaire(sid, this.page(), this.pageSize, this.filterEtat() || undefined).subscribe({
      next: r => { this.taches.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => { this.error.set('Erreur lors du chargement.'); this.loading.set(false); }
    });
  }

  onFilterEtat(val: string)       { this.filterEtat.set(val); this.page.set(0); this.load(); }
  onFilterStagiaire(idStr: string) { if (idStr) this.loadForStagiaire(+idStr); }

  updateEtat(tache: TacheDto, newEtat: string) {
    if (tache.etat === newEtat) return;
    this.updatingId.set(tache.id);
    this.api.updateTacheEtat(tache.id, newEtat).subscribe({
      next: updated => { this.taches.update(list => list.map(t => t.id === updated.id ? updated : t)); this.updatingId.set(null); },
      error: () => this.updatingId.set(null)
    });
  }

  onSubmit() {
    if (this.form.invalid) return;
    const v = this.form.value;
    const sid = v.stagiaireId ?? this.selectedStagiaireId();
    if (!sid) return;
    this.submitting.set(true);
    const req: TacheRequest = {
      titre:       v.titre!,
      description: v.description || undefined,
      dateDebut:   v.dateDebut || undefined,
      deadline:    v.deadline || undefined,
      priorite:    v.priorite ?? undefined,
      commentaire: v.commentaire || undefined
    };
    this.api.createTache(sid, req).subscribe({
      next: () => { this.success.set('Tâche créée.'); this.form.reset({ priorite: 1 }); this.showForm.set(false); this.submitting.set(false); this.load(); },
      error: (e) => { this.error.set(e.error?.message ?? 'Erreur.'); this.submitting.set(false); }
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  etatLabel(etat: string): string {
    const map: Record<string, string> = { A_FAIRE: 'À faire', EN_COURS: 'En cours', TERMINE: 'Terminé', EN_RETARD: 'En retard' };
    return map[etat] ?? etat;
  }

  isOverdue(deadline: string): boolean { return !!deadline && new Date(deadline) < new Date(); }
}
