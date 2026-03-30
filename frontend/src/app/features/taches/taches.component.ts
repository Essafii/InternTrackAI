import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TacheService } from '../../core/services/tache.service';
import { AuthService } from '../../core/services/auth.service';
import { StagiaireService } from '../../core/services/stagiaire.service';
import { TacheDto } from '../../core/models/tache.model';

@Component({
  selector: 'app-taches',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './taches.component.html',
  styleUrl: './taches.component.scss'
})
export class TachesComponent implements OnInit {
  taches: TacheDto[] = [];
  loading = true;

  constructor(
    private tacheService: TacheService,
    private stagiaireService: StagiaireService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser()!;
    if (user.role === 'STAGIAIRE') {
      this.stagiaireService.getByUserId(user.userId).subscribe(s => this.load(s.id));
    }
  }

  load(stagiaireId: number): void {
    this.tacheService.getByStagiaire(stagiaireId).subscribe({
      next: res => { this.taches = res.content; this.loading = false; }
    });
  }

  updateEtat(tache: TacheDto, etat: string): void {
    this.tacheService.update(tache.id, { titre: tache.titre, etat }).subscribe(updated => {
      const idx = this.taches.findIndex(t => t.id === tache.id);
      if (idx >= 0) this.taches[idx] = updated;
    });
  }

  etatBadge(etat: string): string {
    const map: Record<string, string> = {
      'A_FAIRE': 'badge-neutral', 'EN_COURS': 'badge-info',
      'TERMINE': 'badge-success', 'EN_RETARD': 'badge-danger'
    };
    return map[etat] ?? 'badge-neutral';
  }
}
