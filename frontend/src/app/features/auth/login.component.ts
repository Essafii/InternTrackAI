import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  fb = inject(FormBuilder);
  auth = inject(AuthService);
  router = inject(Router);

  loading = signal(false);
  error = signal('');
  showPassword = signal(false);
  readonly gridCells = Array.from({ length: 80 }, (_, i) => i);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const { email, password } = this.form.value;
    this.auth.login(email!, password!).subscribe({
      next: () => this.router.navigate(['/app/dashboard']),
      error: (e) => {
        this.error.set(e.status === 401 ? 'Email ou mot de passe incorrect.' : 'Une erreur est survenue.');
        this.loading.set(false);
      }
    });
  }

  togglePassword() { this.showPassword.update(v => !v); }
}
