import { Component, output } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SeparatorComponent } from '@/shared/components/separator/separator.component';

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
    NgIcon,
    SeparatorComponent,
  ],
  templateUrl: './navigation.component.html',
})
export class NavigationComponent {
  linkClick = output<void>();

  readonly menu: MenuItem[] = [
    { display: 'Home', link: '/', icon: 'lucideHouse' },
    { display: 'Bookings', link: '/bookings', icon: 'lucideClock3' },
    { display: 'Calendar', link: '/calendar', icon: 'lucideCalendar' },
    { display: 'Overview', link: '/overview', icon: 'lucideList' },
  ];

  onLinkClick(): void {
    this.linkClick.emit();
  }

}
