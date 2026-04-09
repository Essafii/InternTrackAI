import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

interface ExportOption {
  label: string;
  description: string;
  key: string;
  action: () => void;
}

@Component({
  selector: 'app-export',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './export.component.html',
  styleUrl: './export.component.scss'
})
export class ExportComponent {
  api = inject(ApiService);
  error      = signal('');
  loadingKey = signal<string | null>(null);

  readonly exports: ExportOption[] = [
    {
      key: 'stagiaires',
      label: 'Stagiaires — Excel',
      description: 'Liste complète des stagiaires, statuts, scores et encadrants (.xlsx)',
      action: () => this.doExport('stagiaires', 'stagiaires.xlsx', this.api.exportStagiaires())
    },
    {
      key: 'absences',
      label: 'Absences — Excel',
      description: 'Toutes les absences avec taux d\'assiduité (.xlsx)',
      action: () => this.doExport('absences', 'absences.xlsx', this.api.exportAbsences())
    },
    {
      key: 'taches',
      label: 'Tâches — Excel',
      description: 'Toutes les tâches et leur état d\'avancement (.xlsx)',
      action: () => this.doExport('taches', 'taches.xlsx', this.api.exportTaches())
    }
  ];

  private doExport(key: string, filename: string, obs: ReturnType<ApiService['exportStagiaires']>) {
    this.loadingKey.set(key);
    this.error.set('');
    obs.subscribe({
      next: (blob: Blob) => { this.downloadFile(blob, filename); this.loadingKey.set(null); },
      error: () => { this.error.set(`Erreur lors de l'export ${key}.`); this.loadingKey.set(null); }
    });
  }

  private downloadFile(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  isLoading(key: string): boolean { return this.loadingKey() === key; }
}
