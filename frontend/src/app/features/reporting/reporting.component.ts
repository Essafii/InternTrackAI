import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ReportJob } from '../../core/models';

interface ReportType {
  type: string;
  label: string;
  description: string;
  icon: string;
}

const REPORT_TYPES: ReportType[] = [
  { type: 'MI_PARCOURS', label: 'Rapport mi-parcours', description: 'Bilan intermédiaire à la semaine 12', icon: '📊' },
  { type: 'FINAL', label: 'Rapport final', description: 'Évaluation finale et classement global', icon: '🏆' },
  { type: 'ASSIDUITE', label: 'Rapport assiduité', description: 'Taux de présence et alertes absences', icon: '📅' }
];

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reporting.component.html',
  styleUrl: './reporting.component.scss'
})
export class ReportingComponent implements OnInit {
  api = inject(ApiService);

  reports = signal<ReportJob[]>([]);
  loading = signal(true);
  error = signal('');
  generatingType = signal<string | null>(null);
  readonly reportTypes = REPORT_TYPES;

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getReports().subscribe({
      next: r => {
        this.reports.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des rapports.');
        this.loading.set(false);
      }
    });
  }

  generate(type: string) {
    this.generatingType.set(type);
    this.api.generateReport(type).subscribe({
      next: newReport => {
        this.reports.update(list => [newReport, ...list]);
        this.generatingType.set(null);
        // Poll for completion
        if (newReport.statut === 'EN_COURS') {
          setTimeout(() => this.load(), 3000);
        }
      },
      error: () => {
        this.error.set(`Erreur lors de la génération du rapport ${type}.`);
        this.generatingType.set(null);
      }
    });
  }

  statutClass(statut: string): string {
    const map: Record<string, string> = {
      EN_COURS: 'badge badge-warning',
      TERMINE: 'badge badge-success',
      ERREUR: 'badge badge-danger'
    };
    return map[statut] ?? 'badge';
  }

  statutLabel(statut: string): string {
    const map: Record<string, string> = {
      EN_COURS: 'En cours', TERMINE: 'Terminé', ERREUR: 'Erreur'
    };
    return map[statut] ?? statut;
  }

  typeLabel(type: string): string {
    return this.reportTypes.find(r => r.type === type)?.label ?? type;
  }
}
