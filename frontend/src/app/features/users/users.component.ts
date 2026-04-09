import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { UserDto, CreateUserRequest, Role } from '../../core/models';

const ROLES: Role[] = ['RH', 'ENCADRANT', 'STAGIAIRE', 'ADMIN'];

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  api = inject(ApiService);
  fb  = inject(FormBuilder);

  users    = signal<UserDto[]>([]);
  loading  = signal(true);
  error    = signal('');
  success  = signal('');
  total    = signal(0);
  page     = signal(0);
  readonly pageSize = 10;

  filterRole    = signal('');
  showForm      = signal(false);
  submitting    = signal(false);
  deletingId    = signal<number | null>(null);
  togglingId    = signal<number | null>(null);
  confirmDeleteId = signal<number | null>(null);
  readonly roles = ROLES;

  form = this.fb.group({
    firstName:  ['', Validators.required],
    lastName:   ['', Validators.required],
    email:      ['', [Validators.required, Validators.email]],
    password:   ['', [Validators.required, Validators.minLength(8)]],
    role:       ['' as Role, Validators.required],
    department: ['']
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getUsers(this.page(), this.pageSize, this.filterRole() || undefined).subscribe({
      next: r => { this.users.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: () => { this.error.set('Erreur lors du chargement.'); this.loading.set(false); }
    });
  }

  onFilterRole(val: string) { this.filterRole.set(val); this.page.set(0); this.load(); }

  onSubmit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.api.createUser(this.form.value as CreateUserRequest).subscribe({
      next: () => { this.success.set('Utilisateur créé.'); this.form.reset(); this.showForm.set(false); this.submitting.set(false); this.load(); },
      error: (e) => { this.error.set(e?.error?.message ?? 'Erreur.'); this.submitting.set(false); }
    });
  }

  toggleEnabled(user: UserDto) {
    this.togglingId.set(user.id);
    this.api.toggleUser(user.id).subscribe({
      next: () => { this.users.update(list => list.map(u => u.id === user.id ? { ...u, enabled: !u.enabled } : u)); this.togglingId.set(null); },
      error: () => this.togglingId.set(null)
    });
  }

  requestDelete(id: number)  { this.confirmDeleteId.set(id); }
  cancelDelete()             { this.confirmDeleteId.set(null); }

  confirmDelete(id: number) {
    this.deletingId.set(id);
    this.confirmDeleteId.set(null);
    this.api.deleteUser(id).subscribe({
      next: () => { this.users.update(list => list.filter(u => u.id !== id)); this.deletingId.set(null); this.total.update(t => t - 1); },
      error: () => { this.error.set('Erreur lors de la suppression.'); this.deletingId.set(null); }
    });
  }

  goPage(p: number) { this.page.set(p); this.load(); }
  get totalPages(): number { return Math.ceil(this.total() / this.pageSize); }
  pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }
}
