import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { StagiaireDto, TacheDto, AbsenceDto, EvaluationDto, LivrableDto } from '../../core/models';

@Component({
  selector: 'app-stagiaire-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './stagiaire-detail.component.html',
  styleUrl: './stagiaire-detail.component.scss'
})
export class StagiaireDetailComponent implements OnInit {
  api   = inject(ApiService);
  route = inject(ActivatedRoute);

  stagiaire   = signal<StagiaireDto | null>(null);
  taches      = signal<TacheDto[]>([]);
  absences    = signal<AbsenceDto[]>([]);
  evaluations = signal<EvaluationDto[]>([]);
  livrables   = signal<LivrableDto[]>([]);
  assiduite   = signal<number>(0);
  loading     = signal(true);
  activeTab   = signal<'taches'|'absences'|'evaluations'|'livrables'>('taches');

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getStagiaire(id).subscribe({
      next: s => { this.stagiaire.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.api.getTachesByStagiaire(id, 0, 20).subscribe({ next: r => this.taches.set(r.content), error: () => {} });
    this.api.getAbsencesByStagiaire(id, 0, 20).subscribe({ next: r => this.absences.set(r.content), error: () => {} });
    this.api.getEvaluationsByStagiaire(id, 0, 20).subscribe({ next: r => this.evaluations.set(r.content), error: () => {} });
    this.api.getLivrablesByStagiaire(id, 0, 20).subscribe({ next: r => this.livrables.set(r.content), error: () => {} });
    this.api.getAssiduite(id).subscribe({ next: taux => this.assiduite.set(taux), error: () => {} });
  }
}
