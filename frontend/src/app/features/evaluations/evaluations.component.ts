import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { EvaluationDto, StagiaireDto, TypeEvaluation, EvaluationRequest } from '../../core/models';

const CRITERIA: Array<{ key: keyof EvaluationRequest; label: string }> = [
  { key: 'noteTechnique',    label: 'Compétences techniques' },
  { key: 'noteProgression',  label: 'Progression' },
  { key: 'noteDelais',       label: 'Respect des délais' },
  { key: 'noteQualite',      label: 'Qualité du travail' },
  { key: 'noteAutonomie',    label: 'Autonomie' },
  { key: 'noteCommunication',label: 'Communication' }
];

@Component({
  selector: 'app-evaluations',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './evaluations.component.html',
  styleUrl: './evaluations.component.scss'
})
export class EvaluationsComponent implements OnInit {
  api  = inject(ApiService);
  auth = inject(AuthService);
  fb   = inject(FormBuilder);

  evaluations = signal<EvaluationDto[]>([]);
  stagiaires  = signal<StagiaireDto[]>([]);
  loading      = signal(true);
  error        = signal('');
  success      = signal('');
  total        = signal(0);
  page         = signal(0);
  readonly pageSize = 10;
  selectedStagiaireId = signal<number | null>(null);

  showForm   = signal(false);
  submitting = signal(false);
  validating = signal<number | null>(null);

  readonly criteria   = CRITERIA;
  readonly typeOptions: TypeEvaluation[] = ['MENSUELLE', 'FINALE', 'AUTO_EVALUATION'];
  readonly moisOptions = [1,2,3,4,5,6,7,8,9,10,11,12];

  role        = this.auth.currentUser()?.role;
  isRhOrAdmin = () => ['RH', 'ADMIN'].includes(this.role ?? '');

  form = this.fb.group({
    stagiaireId:       [null as number | null, Validators.required],
    type:              ['MENSUELLE' as TypeEvaluation, Validators.required],
    mois:              [null as number | null],
    noteTechnique:     [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    noteProgression:   [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    noteDelais:        [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    noteQualite:       [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    noteAutonomie:     [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    noteCommunication: [10, [Validators.required, Validators.min(0), Validators.max(20)]],
    pointsForts:       [''],
    pointsAmeliorer:   [''],
    commentaire:       ['']
  });

  isMensuelle = computed(() => this.form.get('type')?.value === 'MENSUELLE');

  ngOnInit() {
    const user = this.auth.currentUser();
    if (!user) return;

    if (this.isRhOrAdmin()) {
      // Load stagiaire list for the filter/form dropdown
      this.api.getStagiaires(0, 100).subscribe({ next: r => this.stagiaires.set(r.content), error: () => {} });
      this.loading.set(false); // Table stays empty until a stagiaire is selected
    } else if (user.role === 'ENCADRANT') {
      // Load encadrant's stagiaires + evaluations for first one
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

  loadForStagiaire(stagiaireId: number) {
    this.selectedStagiaireId.set(stagiaireId);
    this.load();
  }

  load() {
    const sid = this.selectedStagiaireId();
    if (!sid) { this.loading.set(false); return; }
    this.loading.set(true);
    this.api.getEvaluationsByStagiaire(sid, this.page(), this.pageSize).subscribe({
      next: r => { this.evaluations.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onFilterStagiaire(id: number) {
    this.page.set(0);
    this.loadForStagiaire(id);
  }

  /** Live average preview (0-20) */
  get formAverage(): number {
    const keys = ['noteTechnique','noteProgression','noteDelais','noteQualite','noteAutonomie','noteCommunication'];
    const vals = keys.map(k => +(this.form.get(k)?.value ?? 0));
    return vals.reduce((a, b) => a + b, 0) / vals.length;
  }

  avgColor(avg: number): string {
    if (avg >= 14) return '#4caf50';
    if (avg >= 10) return '#FFAE41';
    return '#D14600';
  }

  onSubmit() {
    if (this.form.invalid) return;
    const v = this.form.value;
    if (!v.stagiaireId) return;
    this.submitting.set(true);
    this.error.set('');

    const req: EvaluationRequest = {
      type:              v.type as TypeEvaluation,
      mois:              v.mois ?? undefined,
      noteTechnique:     v.noteTechnique!,
      noteProgression:   v.noteProgression!,
      noteDelais:        v.noteDelais!,
      noteQualite:       v.noteQualite!,
      noteAutonomie:     v.noteAutonomie!,
      noteCommunication: v.noteCommunication!,
      pointsForts:       v.pointsForts || undefined,
      pointsAmeliorer:   v.pointsAmeliorer || undefined,
      commentaire:       v.commentaire || undefined
    };

    this.api.createEvaluation(v.stagiaireId, req).subscribe({
      next: () => {
        this.success.set('Évaluation enregistrée avec succès.');
        this.form.reset({ type: 'MENSUELLE', noteTechnique: 10, noteProgression: 10, noteDelais: 10, noteQualite: 10, noteAutonomie: 10, noteCommunication: 10 });
        this.showForm.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: (e) => { this.error.set(e.error?.message ?? 'Erreur lors de l\'enregistrement.'); this.submitting.set(false); }
    });
  }

  valider(id: number) {
    this.validating.set(id);
    this.api.validerEvaluation(id).subscribe({
      next: () => { this.validating.set(null); this.load(); },
      error: () => this.validating.set(null)
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  noteColor(n: number): string {
    if (n >= 14) return '#4caf50';
    if (n >= 10) return '#FFAE41';
    return '#D14600';
  }

  typeLabel(t: TypeEvaluation): string {
    return { MENSUELLE: 'Mensuelle', FINALE: 'Finale', AUTO_EVALUATION: 'Auto-éval.' }[t] ?? t;
  }
}
