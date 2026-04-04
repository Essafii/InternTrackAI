import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Absence, Stagiaire } from '../../core/models';

@Component({
  selector: 'app-absences',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './absences.component.html',
  styleUrl: './absences.component.scss'
})
export class AbsencesComponent implements OnInit {
  api = inject(ApiService);
  fb = inject(FormBuilder);

  absences = signal<Absence[]>([]);
  stagiaires = signal<Stagiaire[]>([]);
  loading = signal(true);
  error = signal('');
  success = signal('');
  lowAssiduite = signal<Array<{ nom: string; taux: number }>>([]);
  total = signal(0);
  page = signal(0);
  readonly pageSize = 10;
  filterStagiaireId = signal<number | undefined>(undefined);

  form: FormGroup = this.fb.group({
    stagiaireId: [null, Validators.required],
    date: ['', Validators.required],
    motif: [''],
    justifie: [false]
  });

  submitting = signal(false);
  showForm = signal(false);

  ngOnInit() {
    this.api.getStagiaires(0, 100).subscribe({
      next: r => {
        this.stagiaires.set(r.content);
        this.checkAssiduite(r.content);
      },
      error: () => {}
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.getAbsences(this.page(), this.pageSize, this.filterStagiaireId()).subscribe({
      next: r => {
        this.absences.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des absences.');
        this.loading.set(false);
      }
    });
  }

  checkAssiduite(stagiaires: Stagiaire[]) {
    const low: Array<{ nom: string; taux: number }> = [];
    stagiaires.forEach(s => {
      this.api.getAssiduite(s.id).subscribe({
        next: r => {
          if (r.tauxAssiduite < 80) {
            low.push({
              nom: `${s.user.firstName} ${s.user.lastName}`,
              taux: r.tauxAssiduite
            });
            this.lowAssiduite.set([...low]);
          }
        },
        error: () => {}
      });
    });
  }

  onFilterStagiaire(val: string) {
    this.filterStagiaireId.set(val ? +val : undefined);
    this.page.set(0);
    this.load();
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.success.set('');
    this.api.createAbsence(this.form.value).subscribe({
      next: () => {
        this.success.set('Absence enregistrée avec succès.');
        this.form.reset({ justifie: false });
        this.showForm.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Erreur lors de l\'enregistrement de l\'absence.');
        this.submitting.set(false);
      }
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }
}
