import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { ReportJob, TypeRapport } from '../../core/models';

const REPORT_TYPES: Array<{ type: TypeRapport; label: string; description: string }> = [
  { type: 'RAPPORT_STAGIAIRE',  label: 'Rapport stagiaire',  description: 'Bilan individuel d\'un stagiaire' },
  { type: 'RAPPORT_CAMPAGNE',   label: 'Rapport campagne',   description: 'Vue globale de la campagne de stage' },
  { type: 'RAPPORT_ABSENCES',   label: 'Rapport absences',   description: 'Taux de présence et alertes absences' },
  { type: 'DECISION_RH',        label: 'Décision RH',        description: 'Rapport de décision pour les RH' }
];

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reporting.component.html',
  styleUrl: './reporting.component.scss'
})
export class ReportingComponent implements OnInit {
  api = inject(ApiService);

  reports        = signal<ReportJob[]>([]);
  loading        = signal(true);
  error          = signal('');
  generatingType = signal<string | null>(null);
  readonly reportTypes = REPORT_TYPES;

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getReportJobs().subscribe({
      next: r => { this.reports.set(r.content); this.loading.set(false); },
      error: () => { this.error.set('Erreur lors du chargement.'); this.loading.set(false); }
    });
  }

  generate(type: TypeRapport) {
    this.generatingType.set(type);
    this.api.generateReport(type).subscribe({
      next: job => {
        this.reports.update(list => [job, ...list]);
        this.generatingType.set(null);
        if (job.statut === 'EN_COURS') setTimeout(() => this.load(), 3000);
      },
      error: () => { this.error.set(`Erreur génération ${type}.`); this.generatingType.set(null); }
    });
  }

  download(id: number, type: string) {
    this.api.downloadReport(id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `rapport-${type}.pdf`; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {}
    });
  }

  statutLabel(s: string): string {
    return { EN_ATTENTE: 'En attente', EN_COURS: 'En cours', TERMINE: 'Terminé', ERREUR: 'Erreur' }[s] ?? s;
  }
}
