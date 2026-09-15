import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCalendar,
  lucideCalendarPlus,
  lucideCheck,
  lucideChevronDown,
  lucideChevronLeft,
  lucideChevronRight,
  lucideChevronUp,
  lucideClock3,
  lucideEllipsis,
  lucideFlag,
  lucideHeart,
  lucideHeartPulse,
  lucideHouse,
  lucideList,
  lucideMenu,
  lucidePlay,
  lucidePlus,
  lucideSettings,
  lucideSparkles,
  lucideSquare,
  lucideTrash2,
  lucideTriangleAlert,
  lucideUmbrella,
  lucideX,
} from '@ng-icons/lucide';

import { routes } from './app.routes';
import { provideZard } from '@/shared/core/provider/providezard';
import { ThemeService } from './core/services/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withXhr()),
    provideIcons({
      lucideCalendar,
      lucideCalendarPlus,
      lucideCheck,
      lucideChevronDown,
      lucideChevronLeft,
      lucideChevronRight,
      lucideChevronUp,
      lucideClock3,
      lucideEllipsis,
      lucideFlag,
      lucideHeart,
      lucideHeartPulse,
      lucideHouse,
      lucideList,
      lucideMenu,
      lucidePlay,
      lucidePlus,
      lucideSettings,
      lucideSparkles,
      lucideSquare,
      lucideTrash2,
      lucideTriangleAlert,
      lucideUmbrella,
      lucideX,
    }),
    provideZard(),
    provideAppInitializer(() => {
      const themeService = inject(ThemeService);
      themeService.loadTheme();
    }),
  ],
};
