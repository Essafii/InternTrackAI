import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ActionLog } from '../../core/models';

const ACTION_TYPES = [
  'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT',
  'VALIDATE', 'REJECT', 'GENERATE', 'EXPORT'
];

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.scss'
})
export class AuditComponent implements OnInit {
  api = inject(ApiService);

  logs = signal<ActionLog[]>([]);
  loading = signal(true);
  error = signal('');
  total = signal(0);
  page = signal(0);
  readonly pageSize = 20;
  filterAction = signal('');
  readonly actionTypes = ['', ...ACTION_TYPES];

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getAuditLogs(this.page(), this.pageSize, this.filterAction() || undefined).subscribe({
      next: r => {
        this.logs.set(r.content);
        this.total.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des logs d\'audit.');
        this.loading.set(false);
      }
    });
  }

  onFilterAction(val: string) {
    this.filterAction.set(val);
    this.page.set(0);
    this.load();
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }

  actionClass(action: string): string {
    const danger = ['DELETE', 'REJECT'];
    const success = ['CREATE', 'VALIDATE'];
    const warning = ['UPDATE'];
    if (danger.includes(action)) return 'badge badge-danger';
    if (success.includes(action)) return 'badge badge-success';
    if (warning.includes(action)) return 'badge badge-warning';
    return 'badge badge-info';
  }
}
