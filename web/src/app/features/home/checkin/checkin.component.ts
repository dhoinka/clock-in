import {
  Component,
  signal,
  computed,
  inject,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { format, getHours } from 'date-fns';
import { WorklogService } from '@/core/services/worklog.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { CheckInModel } from '@/core/models/worklog.model';

@Component({
  selector: 'app-checkin',
  imports: [ZardButtonComponent, NgIcon],
  templateUrl: './checkin.component.html',
})
export class CheckinComponent implements OnInit, OnDestroy {
  private worklogService = inject(WorklogService);

  model = signal<CheckInModel>({ checkedIn: false, balance: '', gross: '' });
  currentTime = signal(new Date());
  isLoading = signal(false);

  private timeInterval?: ReturnType<typeof setInterval>;
  private balanceInterval?: ReturnType<typeof setInterval>;

  readonly greeting = computed(() => {
    const hour = getHours(this.currentTime());
    if (hour < 12) return 'morning';
    if (hour < 18) return 'afternoon';
    return 'evening';
  });

  readonly formattedTime = computed(() => format(this.currentTime(), 'PPpp'));

  ngOnInit(): void {
    this.timeInterval = setInterval(() => {
      this.currentTime.set(new Date());
    }, 1000);

    this.fetchBalance();
    this.balanceInterval = setInterval(() => this.fetchBalance(), 10000);
  }

  ngOnDestroy(): void {
    if (this.timeInterval) clearInterval(this.timeInterval);
    if (this.balanceInterval) clearInterval(this.balanceInterval);
  }

  private async fetchBalance(): Promise<void> {
    try {
      const balance = await this.worklogService.getStatus();
      this.model.set({
        checkedIn: balance.checkedIn ?? false,
        balance: balance.balance ?? '',
        gross: balance.gross ?? '',
      });
    } catch (error) {
      console.error('Failed to fetch balance:', error);
    }
  }

  async handleClick(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await this.worklogService.postStatus();
      this.model.set({
        checkedIn: result.checkedIn ?? false,
        balance: result.balance ?? '',
        gross: result.gross ?? '',
      });
    } catch (error) {
      console.error('Check-in failed:', error);
    } finally {
      this.isLoading.set(false);
    }
  }
}
