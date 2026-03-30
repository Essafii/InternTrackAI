import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  submit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.error = '';

    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.status === 401
          ? 'Email ou mot de passe incorrect.'
          : 'Une erreur est survenue. Réessayez.';
        this.loading = false;
      }
    });
  }

  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
}
