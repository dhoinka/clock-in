import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendar,
  lucideCheck,
  lucideChevronDown,
  lucideChevronLeft,
  lucideChevronRight,
  lucideChevronUp,
  lucideClock3,
  lucideEllipsis,
  lucideHeart,
  lucideHouse,
  lucideList,
  lucideLogOut,
  lucideMenu,
  lucidePlay,
  lucidePlus,
  lucideSettings,
  lucideSquare,
  lucideTrash2,
  lucideTriangleAlert,
  lucideX,
} from '@ng-icons/lucide';

import { routes } from './app.routes';
import { provideZard } from '@/shared/core/provider/providezard';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { ThemeService } from './core/services/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideIcons({
      lucideCalendar,
      lucideCheck,
      lucideChevronDown,
      lucideChevronLeft,
      lucideChevronRight,
      lucideChevronUp,
      lucideClock3,
      lucideEllipsis,
      lucideHeart,
      lucideHouse,
      lucideList,
      lucideLogOut,
      lucideMenu,
      lucidePlay,
      lucidePlus,
      lucideSettings,
      lucideSquare,
      lucideTrash2,
      lucideTriangleAlert,
      lucideX,
    }),
    provideZard(),
    provideAppInitializer(() => {
      const themeService = inject(ThemeService);
      themeService.loadTheme();
    }),
  ],
};
