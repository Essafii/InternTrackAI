import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { StagiaireDto, UserDto, OnboardingRequest, StatutStagiaire } from '../../core/models';

@Component({
  selector: 'app-stagiaires',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, ReactiveFormsModule],
  templateUrl: './stagiaires.component.html',
  styleUrl: './stagiaires.component.scss'
})
export class StagiairesComponent implements OnInit {
  api  = inject(ApiService);
  auth = inject(AuthService);
  fb   = inject(FormBuilder);

  stagiaires = signal<StagiaireDto[]>([]);
  encadrants = signal<UserDto[]>([]);
  loading    = signal(true);
  total      = signal(0);
  page       = signal(0);
  search     = signal('');
  statut     = signal('');

  showOnboarding = signal(false);
  submitting     = signal(false);
  error          = signal('');
  success        = signal('');

  readonly statuts: StatutStagiaire[] = ['ACTIF', 'EN_RETARD', 'SUSPENDU', 'TERMINE', 'ARCHIVE'];
  readonly niveaux = ['Bac+2', 'Bac+3', 'Bac+4', 'Bac+5'];

  onboardForm = this.fb.group({
    // User fields
    firstName:    ['', Validators.required],
    lastName:     ['', Validators.required],
    email:        ['', [Validators.required, Validators.email]],
    password:     ['', [Validators.required, Validators.minLength(6)]],
    phone:        [''],
    // Internship fields
    sujet:        ['', Validators.required],
    equipe:       [''],
    dateDebut:    ['', Validators.required],
    dateFin:      ['', Validators.required],
    encadrantId:  [null as number | null],
    etablissement:[''],
    niveauEtude:  [''],
    specialite:   [''],
    description:  ['']
  });

  ngOnInit() {
    this.load();
    this.loadEncadrants();
  }

  load() {
    this.loading.set(true);
    this.api.getStagiaires(this.page(), 10, this.search() || undefined, this.statut() || undefined).subscribe({
      next: r => { this.stagiaires.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  loadEncadrants() {
    this.api.getUsers(0, 100, 'ENCADRANT').subscribe({
      next: r => this.encadrants.set(r.content),
      error: () => {}
    });
  }

  onSearch(val: string) { this.search.set(val); this.page.set(0); this.load(); }
  onStatut(val: string) { this.statut.set(val); this.page.set(0); this.load(); }
  goPage(p: number)     { this.page.set(p); this.load(); }

  get totalPages(): number { return Math.ceil(this.total() / 10); }
  pages(): number[]        { return Array.from({ length: this.totalPages }, (_, i) => i); }

  initials(fullName: string): string {
    return fullName.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  submit() {
    if (this.onboardForm.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    const v = this.onboardForm.value;
    const req: OnboardingRequest = {
      firstName: v.firstName!,
      lastName:  v.lastName!,
      email:     v.email!,
      password:  v.password!,
      phone:     v.phone || undefined,
      sujet:     v.sujet!,
      equipe:    v.equipe || undefined,
      dateDebut: v.dateDebut!,
      dateFin:   v.dateFin!,
      encadrantId:   v.encadrantId ?? undefined,
      etablissement: v.etablissement || undefined,
      niveauEtude:   v.niveauEtude || undefined,
      specialite:    v.specialite || undefined,
      description:   v.description || undefined
    };
    this.api.onboardStagiaire(req).subscribe({
      next: () => {
        this.success.set('Stagiaire créé et email de bienvenue envoyé.');
        this.onboardForm.reset();
        this.showOnboarding.set(false);
        this.submitting.set(false);
        this.load();
      },
      error: (e) => {
        this.error.set(e.error?.message ?? 'Erreur lors de la création.');
        this.submitting.set(false);
      }
    });
  }
}
