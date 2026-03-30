import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from '../shared/components/sidebar/sidebar.component';
import { TopbarComponent } from '../shared/components/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterModule, SidebarComponent, TopbarComponent],
  template: `
    <div class="shell">
      <app-sidebar></app-sidebar>
      <div class="main">
        <app-topbar></app-topbar>
        <div class="content">
          <router-outlet></router-outlet>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .shell { display: flex; min-height: 100vh; }
    .main  { flex: 1; margin-left: 240px; display: flex; flex-direction: column; }
    .content { padding: 28px; flex: 1; }
  `]
})
export class ShellComponent {}
