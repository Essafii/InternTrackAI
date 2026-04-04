import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Stagiaire } from '../../core/models';

@Component({
  selector: 'app-stagiaires',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './stagiaires.component.html',
  styleUrl: './stagiaires.component.scss'
})
export class StagiairesComponent implements OnInit {
  api = inject(ApiService);
  stagiaires = signal<Stagiaire[]>([]);
  loading = signal(true);
  total = signal(0);
  page = signal(0);
  search = signal('');
  statut = signal('');

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getStagiaires(this.page(), 10, this.search() || undefined, this.statut() || undefined).subscribe({
      next: r => { this.stagiaires.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onSearch(val: string) { this.search.set(val); this.page.set(0); this.load(); }
  onStatut(val: string) { this.statut.set(val); this.page.set(0); this.load(); }
  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / 10); }
  pages(): number[] { return Array.from({length: this.totalPages}, (_, i) => i); }
}
