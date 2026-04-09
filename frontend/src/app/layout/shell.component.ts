import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';
import { TopbarComponent } from '../shared/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="min-h-screen bg-[#0B0D1A]">
      <app-sidebar />
      <app-topbar />
      <main class="ml-[248px] pt-14 min-h-screen">
        <router-outlet />
      </main>
    </div>
  `
})
export class ShellComponent {}
