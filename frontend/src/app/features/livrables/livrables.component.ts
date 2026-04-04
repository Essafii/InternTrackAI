import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Livrable, Stagiaire } from '../../core/models';

@Component({
  selector: 'app-livrables',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './livrables.component.html',
  styleUrl: './livrables.component.scss'
})
export class LivrablesComponent implements OnInit {
  api = inject(ApiService);
  fb = inject(FormBuilder);

  livrables = signal<Livrable[]>([]);
  stagiaires = signal<Stagiaire[]>([]);
  loading = signal(true);
  error = signal('');
  success = signal('');
  total = signal(0);
  page = signal(0);
  readonly pageSize = 10;
  filterStatut = signal('');
  showForm = signal(false);
  submitting = signal(false);

  validatingId = signal<number | null>(null);
  commentaires = signal<Record<number, string>>({});

  selectedFile = signal<File | null>(null);

  form: FormGroup = this.fb.group({
    stagiaireId: [null, Validators.required],
    titre: ['', Validators.required],
    description: ['']
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
    this.api.getLivrables(this.page(), this.pageSize, undefined, this.filterStatut() || undefined).subscribe({
      next: r => {
        this.livrables.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des livrables.');
        this.loading.set(false);
      }
    });
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile.set(input.files[0]);
    }
  }

  onSubmit() {
    if (this.form.invalid || !this.selectedFile()) return;
    this.submitting.set(true);
    const fd = new FormData();
    fd.append('stagiaireId', this.form.value.stagiaireId);
    fd.append('titre', this.form.value.titre);
    fd.append('description', this.form.value.description ?? '');
    fd.append('fichier', this.selectedFile()!);
    this.api.uploadLivrable(fd).subscribe({
      next: () => {
        this.success.set('Livrable déposé avec succès.');
        this.form.reset();
        this.selectedFile.set(null);
        this.showForm.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Erreur lors du dépôt du livrable.');
        this.submitting.set(false);
      }
    });
  }

  setCommentaire(id: number, val: string) {
    this.commentaires.update(m => ({ ...m, [id]: val }));
  }

  valider(livrable: Livrable, statut: 'VALIDE' | 'REJETE') {
    this.validatingId.set(livrable.id);
    const commentaire = this.commentaires()[livrable.id] ?? '';
    this.api.validerLivrable(livrable.id, statut, commentaire).subscribe({
      next: updated => {
        this.livrables.update(list => list.map(l => l.id === updated.id ? updated : l));
        this.validatingId.set(null);
      },
      error: () => this.validatingId.set(null)
    });
  }

  onFilterStatut(val: string) {
    this.filterStatut.set(val);
    this.page.set(0);
    this.load();
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  statutClass(statut: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'badge badge-warning',
      VALIDE: 'badge badge-success',
      REJETE: 'badge badge-danger'
    };
    return map[statut] ?? 'badge';
  }

  statutLabel(statut: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'En attente', VALIDE: 'Validé', REJETE: 'Rejeté'
    };
    return map[statut] ?? statut;
  }
}
