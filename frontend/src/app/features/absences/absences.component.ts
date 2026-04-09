import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { AbsenceDto, StagiaireDto, TypeAbsence, AbsenceRequest } from '../../core/models';

@Component({
  selector: 'app-absences',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './absences.component.html',
  styleUrl: './absences.component.scss'
})
export class AbsencesComponent implements OnInit {
  api  = inject(ApiService);
  auth = inject(AuthService);
  fb   = inject(FormBuilder);

  absences   = signal<AbsenceDto[]>([]);
  stagiaires = signal<StagiaireDto[]>([]);
  loading    = signal(true);
  error      = signal('');
  success    = signal('');
  total      = signal(0);
  page       = signal(0);
  readonly pageSize = 10;

  selectedStagiaireId = signal<number | null>(null);
  assiduite           = signal<number | null>(null);
  showForm            = signal(false);
  submitting          = signal(false);
  validatingId        = signal<number | null>(null);

  readonly typeOptions: TypeAbsence[] = ['JUSTIFIEE', 'NON_JUSTIFIEE', 'CONGE', 'MALADIE'];

  form = this.fb.group({
    stagiaireId:  [null as number | null],
    dateAbsence:  ['', Validators.required],
    type:         ['NON_JUSTIFIEE' as TypeAbsence, Validators.required],
    motif:        ['']
  });

  role        = this.auth.currentUser()?.role;
  isRhOrAdmin = () => ['RH', 'ADMIN'].includes(this.role ?? '');
  isEncadrant = () => this.role === 'ENCADRANT';

  ngOnInit() {
    const user = this.auth.currentUser();
    if (!user) return;

    if (this.isRhOrAdmin() || this.isEncadrant()) {
      this.api.getStagiaires(0, 100).subscribe({
        next: r => {
          this.stagiaires.set(r.content);
          if (r.content.length > 0 && !this.isRhOrAdmin()) { this.loadForStagiaire(r.content[0].id); }
          else { this.loading.set(false); }
        },
        error: () => this.loading.set(false)
      });
    } else {
      // STAGIAIRE — load own absences
      this.api.getStagiaireByUserId(user.userId).subscribe({
        next: s => this.loadForStagiaire(s.id),
        error: () => this.loading.set(false)
      });
    }
  }

  loadForStagiaire(id: number) {
    this.selectedStagiaireId.set(id);
    this.load();
    this.api.getAssiduite(id).subscribe({
      next: taux => this.assiduite.set(taux),
      error: () => {}
    });
  }

  load() {
    const sid = this.selectedStagiaireId();
    if (!sid) { this.loading.set(false); return; }
    this.loading.set(true);
    this.api.getAbsencesByStagiaire(sid, this.page(), this.pageSize).subscribe({
      next: r => { this.absences.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onFilterStagiaire(idStr: string) { if (idStr) this.loadForStagiaire(+idStr); }

  onSubmit() {
    if (this.form.invalid) return;
    const v = this.form.value;
    const sid = v.stagiaireId ?? this.selectedStagiaireId();
    if (!sid) return;
    this.submitting.set(true);
    this.error.set('');
    const req: AbsenceRequest = {
      dateAbsence: v.dateAbsence!,
      type:        v.type as TypeAbsence,
      motif:       v.motif || undefined
    };
    this.api.createAbsence(sid, req).subscribe({
      next: () => {
        this.success.set('Absence enregistrée.');
        this.form.reset({ type: 'NON_JUSTIFIEE' });
        this.showForm.set(false);
        this.submitting.set(false);
        this.loadForStagiaire(sid);
      },
      error: (e) => { this.error.set(e.error?.message ?? 'Erreur.'); this.submitting.set(false); }
    });
  }

  valider(id: number) {
    this.validatingId.set(id);
    this.api.validerAbsence(id).subscribe({
      next: updated => {
        this.absences.update(list => list.map(a => a.id === updated.id ? updated : a));
        this.validatingId.set(null);
        const sid = this.selectedStagiaireId();
        if (sid) this.api.getAssiduite(sid).subscribe({ next: t => this.assiduite.set(t), error: () => {} });
      },
      error: () => this.validatingId.set(null)
    });
  }

  typeLabel(t: TypeAbsence): string {
    return { JUSTIFIEE: 'Justifiée', NON_JUSTIFIEE: 'Non justifiée', CONGE: 'Congé', MALADIE: 'Maladie' }[t] ?? t;
  }

  assiduiteColor(taux: number): string {
    if (taux >= 80) return '#4caf50';
    if (taux >= 60) return '#FFAE41';
    return '#D14600';
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }
}
