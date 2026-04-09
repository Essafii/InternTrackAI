import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { LivrableDto, StagiaireDto, StatutLivrable, LivrableValidationRequest } from '../../core/models';

@Component({
  selector: 'app-livrables',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './livrables.component.html',
  styleUrl: './livrables.component.scss'
})
export class LivrablesComponent implements OnInit {
  api  = inject(ApiService);
  auth = inject(AuthService);
  fb   = inject(FormBuilder);

  livrables  = signal<LivrableDto[]>([]);
  stagiaires = signal<StagiaireDto[]>([]);
  loading    = signal(true);
  error      = signal('');
  success    = signal('');
  total      = signal(0);
  page       = signal(0);
  readonly pageSize = 10;

  filterStatut          = signal('');
  selectedStagiaireId   = signal<number | null>(null);
  showForm              = signal(false);
  submitting            = signal(false);
  validatingId          = signal<number | null>(null);
  commentaires          = signal<Record<number, string>>({});
  selectedFile          = signal<File | null>(null);

  readonly allStatuts: StatutLivrable[] = ['SOUMIS','EN_REVISION','VALIDE','REJETE','CORRECTION_DEMANDEE'];

  form = this.fb.group({
    stagiaireId: [null as number | null],
    titre:       ['', Validators.required],
    description: ['']
  });

  role        = this.auth.currentUser()?.role;
  isRhOrAdmin = () => ['RH', 'ADMIN'].includes(this.role ?? '');
  isEncadrant = () => this.role === 'ENCADRANT';
  isStagiaire = () => this.role === 'STAGIAIRE';

  ngOnInit() {
    const user = this.auth.currentUser();
    if (!user) return;

    if (this.isRhOrAdmin()) {
      this.api.getStagiaires(0, 100).subscribe({ next: r => this.stagiaires.set(r.content), error: () => {} });
      this.loading.set(false);
    } else if (this.isEncadrant()) {
      this.api.getStagiaires(0, 100).subscribe({
        next: r => {
          this.stagiaires.set(r.content);
          if (r.content.length > 0) this.loadForStagiaire(r.content[0].id);
          else this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else {
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
    this.api.getLivrablesByStagiaire(sid, this.page(), this.pageSize, this.filterStatut() || undefined).subscribe({
      next: r => { this.livrables.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onFilterStagiaire(idStr: string) { if (idStr) this.loadForStagiaire(+idStr); }
  onFilterStatut(val: string) { this.filterStatut.set(val); this.page.set(0); this.load(); }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.selectedFile.set(input.files[0]);
  }

  onSubmit() {
    if (this.form.invalid || !this.selectedFile()) return;
    const sid = this.form.value.stagiaireId ?? this.selectedStagiaireId();
    if (!sid) return;
    this.submitting.set(true);
    const fd = new FormData();
    fd.append('titre', this.form.value.titre!);
    if (this.form.value.description) fd.append('description', this.form.value.description);
    fd.append('fichier', this.selectedFile()!);
    this.api.uploadLivrable(sid, fd).subscribe({
      next: () => {
        this.success.set('Livrable déposé.');
        this.form.reset();
        this.selectedFile.set(null);
        this.showForm.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: (e) => { this.error.set(e.error?.message ?? 'Erreur.'); this.submitting.set(false); }
    });
  }

  setCommentaire(id: number, val: string) { this.commentaires.update(m => ({ ...m, [id]: val })); }

  valider(livrable: LivrableDto, statut: StatutLivrable) {
    this.validatingId.set(livrable.id);
    const req: LivrableValidationRequest = { statut, commentaire: this.commentaires()[livrable.id] ?? '' };
    this.api.validerLivrable(livrable.id, req).subscribe({
      next: updated => { this.livrables.update(list => list.map(l => l.id === updated.id ? updated : l)); this.validatingId.set(null); },
      error: () => this.validatingId.set(null)
    });
  }

  download(id: number, fileName: string) {
    this.api.downloadLivrable(id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {}
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  statutLabel(s: string): string {
    const m: Record<string, string> = {
      SOUMIS: 'Soumis', EN_REVISION: 'En révision', VALIDE: 'Validé',
      REJETE: 'Rejeté', CORRECTION_DEMANDEE: 'Correction demandée'
    };
    return m[s] ?? s;
  }

  canValidate(): boolean { return this.isEncadrant() || this.isRhOrAdmin(); }
}
