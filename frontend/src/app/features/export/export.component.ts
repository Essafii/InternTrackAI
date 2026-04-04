import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

interface ExportOption {
  label: string;
  description: string;
  action: () => void;
  format: string;
  type: 'stagiaires' | 'classement';
  icon: string;
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
  error = signal('');
  loadingKey = signal<string | null>(null);

  readonly exports: ExportOption[] = [
    {
      label: 'Stagiaires — CSV',
      description: 'Exporter la liste complète des stagiaires au format CSV',
      format: 'csv',
      type: 'stagiaires',
      icon: '📄',
      action: () => this.doExport('stagiaires', 'csv', 'stagiaires.csv')
    },
    {
      label: 'Stagiaires — Excel',
      description: 'Exporter la liste complète des stagiaires au format Excel (.xlsx)',
      format: 'xlsx',
      type: 'stagiaires',
      icon: '📊',
      action: () => this.doExport('stagiaires', 'xlsx', 'stagiaires.xlsx')
    },
    {
      label: 'Classement — CSV',
      description: 'Exporter le classement et les scores au format CSV',
      format: 'csv',
      type: 'classement',
      icon: '🏆',
      action: () => this.doExport('classement', 'csv', 'classement.csv')
    },
    {
      label: 'Classement — Excel',
      description: 'Exporter le classement et les scores au format Excel (.xlsx)',
      format: 'xlsx',
      type: 'classement',
      icon: '📈',
      action: () => this.doExport('classement', 'xlsx', 'classement.xlsx')
    }
  ];

  private doExport(type: 'stagiaires' | 'classement', format: string, filename: string) {
    const key = `${type}-${format}`;
    this.loadingKey.set(key);
    this.error.set('');
    const obs = type === 'stagiaires'
      ? this.api.exportStagiaires(format)
      : this.api.exportClassement(format);

    obs.subscribe({
      next: blob => {
        this.downloadFile(blob, filename);
        this.loadingKey.set(null);
      },
      error: () => {
        this.error.set(`Erreur lors de l'export ${type} (${format.toUpperCase()}).`);
        this.loadingKey.set(null);
      }
    });
  }

  private downloadFile(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  isLoading(type: string, format: string): boolean {
    return this.loadingKey() === `${type}-${format}`;
  }
}
