import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StagiaireService } from '../../core/services/stagiaire.service';
import { StagiaireDto } from '../../core/models/stagiaire.model';

@Component({
  selector: 'app-stagiaires',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stagiaires.component.html',
  styleUrl: './stagiaires.component.scss'
})
export class StagiairesComponent implements OnInit {
  stagiaires: StagiaireDto[] = [];
  total = 0;
  page = 0;
  loading = true;

  constructor(private service: StagiaireService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.service.getAll(this.page).subscribe({
      next: res => { this.stagiaires = res.content; this.total = res.totalElements; this.loading = false; },
      error: () => this.loading = false
    });
  }

  statusBadge(statut: string): string {
    const map: Record<string, string> = {
      'ACTIF': 'badge-success', 'EN_RETARD': 'badge-warning',
      'TERMINE': 'badge-neutral', 'ARCHIVE': 'badge-neutral'
    };
    return map[statut] ?? 'badge-neutral';
  }
}
