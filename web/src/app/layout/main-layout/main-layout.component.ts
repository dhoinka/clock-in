import {
  Component,
  signal,
  HostListener,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { RouterOutlet } from '@angular/router';
import { NavigationComponent } from '../navigation/navigation.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, NavigationComponent, NgIcon],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  isMobile = signal(false);
  isMobileMenuOpen = signal(false);

  constructor() {
    this.checkScreenSize();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.checkScreenSize();
  }

  private checkScreenSize(): void {
    this.isMobile.set(window.innerWidth < 768);
  }
}
