import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AbsenceService } from '../../core/services/absence.service';
import { AuthService } from '../../core/services/auth.service';
import { StagiaireService } from '../../core/services/stagiaire.service';
import { AbsenceDto } from '../../core/models/absence.model';

@Component({
  selector: 'app-absences',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './absences.component.html',
  styleUrl: './absences.component.scss'
})
export class AbsencesComponent implements OnInit {
  absences: AbsenceDto[] = [];
  assiduite: number | null = null;
  stagiaireId!: number;
  loading = true;
  showForm = false;
  form: FormGroup;
  error = '';
  success = '';

  constructor(
    private absenceService: AbsenceService,
    private stagiaireService: StagiaireService,
    private auth: AuthService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      dateAbsence: ['', Validators.required],
      type:        ['NON_JUSTIFIEE', Validators.required],
      motif:       [''],
      justifiee:   [false]
    });
  }

  ngOnInit(): void {
    const user = this.auth.getCurrentUser()!;
    if (user.role === 'STAGIAIRE') {
      this.stagiaireService.getByUserId(user.userId).subscribe(s => {
        this.stagiaireId = s.id;
        this.load();
      });
    }
  }

  load(): void {
    this.absenceService.getByStagiaire(this.stagiaireId).subscribe({
      next: res => { this.absences = res.content; this.loading = false; }
    });
    this.absenceService.getAssiduite(this.stagiaireId).subscribe(v => this.assiduite = v);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.absenceService.enregistrer(this.stagiaireId, this.form.value).subscribe({
      next: () => { this.success = 'Absence enregistrée.'; this.showForm = false; this.load(); },
      error: () => this.error = 'Erreur lors de l\'enregistrement.'
    });
  }

  assiduiteClass(): string {
    if (this.assiduite === null) return '';
    return this.assiduite >= 80 ? 'badge-success' : 'badge-danger';
  }
}
