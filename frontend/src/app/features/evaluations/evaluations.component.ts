import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Evaluation, Stagiaire } from '../../core/models';

const CRITERIA: Array<{ key: string; label: string }> = [
  { key: 'autonomie', label: 'Autonomie' },
  { key: 'competencesTechniques', label: 'Compétences techniques' },
  { key: 'communication', label: 'Communication' },
  { key: 'ponctualite', label: 'Ponctualité' },
  { key: 'resolutionProblemes', label: 'Résolution de problèmes' },
  { key: 'travailEquipe', label: 'Travail en équipe' }
];

@Component({
  selector: 'app-evaluations',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './evaluations.component.html',
  styleUrl: './evaluations.component.scss'
})
export class EvaluationsComponent implements OnInit {
  api = inject(ApiService);
  fb = inject(FormBuilder);

  evaluations = signal<Evaluation[]>([]);
  stagiaires = signal<Stagiaire[]>([]);
  loading = signal(true);
  error = signal('');
  success = signal('');
  total = signal(0);
  page = signal(0);
  readonly pageSize = 10;
  showForm = signal(false);
  submitting = signal(false);
  readonly criteria = CRITERIA;

  form: FormGroup = this.fb.group({
    stagiaireId: [null, Validators.required],
    periode: ['', Validators.required],
    autonomie: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    competencesTechniques: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    communication: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    ponctualite: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    resolutionProblemes: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    travailEquipe: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    commentaire: ['']
  });

  ngOnInit() {
    this.api.getStagiaires(0, 100).subscribe({
      next: r => this.stagiaires.set(r.content),
      error: () => {}
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.api.getEvaluations(this.page(), this.pageSize).subscribe({
      next: r => {
        this.evaluations.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des évaluations.');
        this.loading.set(false);
      }
    });
  }

  get formAverage(): number {
    const vals = this.criteria.map(c => this.form.get(c.key)?.value ?? 0);
    return vals.reduce((a, b) => a + b, 0) / vals.length;
  }

  setStars(criterionKey: string, value: number) {
    this.form.get(criterionKey)?.setValue(value);
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }

  onSubmit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.api.createEvaluation(this.form.value).subscribe({
      next: () => {
        this.success.set('Évaluation enregistrée.');
        this.form.reset({ autonomie: 3, competencesTechniques: 3, communication: 3, ponctualite: 3, resolutionProblemes: 3, travailEquipe: 3 });
        this.showForm.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Erreur lors de l\'enregistrement.');
        this.submitting.set(false);
      }
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  scoreColor(score: number): string {
    if (score >= 4) return 'text-success';
    if (score >= 3) return 'text-warning';
    return 'text-danger';
  }
}
