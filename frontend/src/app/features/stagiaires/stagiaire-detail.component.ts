import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { Stagiaire, Tache, Absence, Evaluation, Livrable } from '../../core/models';

@Component({
  selector: 'app-stagiaire-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './stagiaire-detail.component.html',
  styleUrl: './stagiaire-detail.component.scss'
})
export class StagiaireDetailComponent implements OnInit {
  api = inject(ApiService);
  route = inject(ActivatedRoute);

  stagiaire = signal<Stagiaire | null>(null);
  taches = signal<Tache[]>([]);
  absences = signal<Absence[]>([]);
  evaluations = signal<Evaluation[]>([]);
  livrables = signal<Livrable[]>([]);
  assiduite = signal<number>(0);
  loading = signal(true);
  activeTab = signal<'taches'|'absences'|'evaluations'|'livrables'>('taches');

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getStagiaire(id).subscribe(s => {
      this.stagiaire.set(s);
      this.loading.set(false);
    });
    this.api.getTaches(0, 20, id).subscribe(r => this.taches.set(r.content));
    this.api.getAbsences(0, 20, id).subscribe(r => this.absences.set(r.content));
    this.api.getEvaluations(0, 20, id).subscribe(r => this.evaluations.set(r.content));
    this.api.getLivrables(0, 20, id).subscribe(r => this.livrables.set(r.content));
    this.api.getAssiduite(id).subscribe(r => this.assiduite.set(r.tauxAssiduite));
  }
}
