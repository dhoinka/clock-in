import { Component, output, inject } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '@/core/services/auth.service';
import { MeService } from '@/core/services/me.service';
import { ZardAvatarComponent } from '@/shared/components/avatar';
import { SeparatorComponent } from '@/shared/components/separator/separator.component';
import {
  ZardDropdownMenuComponent,
  ZardDropdownMenuItemComponent,
} from '@/shared/components/dropdown';

type MenuIconName =
  | 'lucideHouse'
  | 'lucideClock3'
  | 'lucideCalendar'
  | 'lucideList';

interface MenuItem {
  display: string;
  link: string;
  icon: MenuIconName;
}

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    ZardAvatarComponent,
    SeparatorComponent,
    NgIcon,
    ZardDropdownMenuComponent,
    ZardDropdownMenuItemComponent,
  ],
  templateUrl: './navigation.component.html',
})
export class NavigationComponent {
  linkClick = output<void>();

  private authService = inject(AuthService);
  private meService = inject(MeService);
  private router = inject(Router);

  readonly user = this.authService.user;

  readonly menu: MenuItem[] = [
    { display: 'Home', link: '/home', icon: 'lucideHouse' },
    { display: 'Bookings', link: '/home/bookings', icon: 'lucideClock3' },
    { display: 'Calendar', link: '/home/calendar', icon: 'lucideCalendar' },
    { display: 'Overview', link: '/home/overview', icon: 'lucideList' },
  ];

  getName(): string {
    return this.meService.getName();
  }

  getInitials(): string {
    const name = this.getName();
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  onLinkClick(): void {
    this.linkClick.emit();
  }

  navigateToSettings(): void {
    this.router.navigate(['/home/settings']);
    this.linkClick.emit();
  }

  logout(): void {
    this.authService.logout();
  }
}
