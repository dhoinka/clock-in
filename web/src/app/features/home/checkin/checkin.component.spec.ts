import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { CheckinComponent } from './checkin.component';
import { WorklogService } from '../../../core/services/worklog.service';
import { StatusResponse } from '../../../core/models/worklog.model';

describe('CheckinComponent', () => {
  let component: CheckinComponent;
  let fixture: ComponentFixture<CheckinComponent>;
  let worklogServiceMock: {
    getStatus: ReturnType<typeof vi.fn>;
    postStatus: ReturnType<typeof vi.fn>;
  };

  const defaultStatus: StatusResponse = {
    checkedIn: false,
    balance: '08:00',
    gross: '09:00',
  };

  beforeEach(async () => {
    worklogServiceMock = {
      getStatus: vi.fn().mockResolvedValue(defaultStatus),
      postStatus: vi.fn().mockResolvedValue({
        checkedIn: true,
        balance: '04:00',
        gross: '04:30',
      } satisfies StatusResponse),
    };

    await TestBed.configureTestingModule({
      imports: [CheckinComponent],
      providers: [
        { provide: WorklogService, useValue: worklogServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckinComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    component.ngOnDestroy();
    vi.clearAllTimers();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialise with checkedIn false and empty balance', () => {
    expect(component.model()).toEqual({
      checkedIn: false,
      balance: '',
      gross: '',
    });
    expect(component.isLoading()).toBe(false);
  });

  it('should fetch balance on init and update model', async () => {
    fixture.detectChanges(); // triggers ngOnInit
    await fixture.whenStable();
    expect(worklogServiceMock.getStatus).toHaveBeenCalledTimes(1);
    expect(component.model()).toEqual({
      checkedIn: false,
      balance: '08:00',
      gross: '09:00',
    });
  });

  it('should update model with checkedIn true after handleClick', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    await component.handleClick();
    expect(worklogServiceMock.postStatus).toHaveBeenCalledTimes(1);
    expect(component.model()).toEqual({
      checkedIn: true,
      balance: '04:00',
      gross: '04:30',
    });
  });

  it('should set isLoading to false after handleClick resolves', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    await component.handleClick();
    expect(component.isLoading()).toBe(false);
  });

  it('should set isLoading to false even when postStatus rejects', async () => {
    worklogServiceMock.postStatus.mockRejectedValue(new Error('Server error'));
    fixture.detectChanges();
    await fixture.whenStable();
    await component.handleClick();
    expect(component.isLoading()).toBe(false);
  });

  it('should return correct greeting for morning hours', () => {
    vi.setSystemTime(new Date('2024-01-01T08:00:00'));
    component.currentTime.set(new Date());
    expect(component.greeting()).toBe('morning');
    vi.useRealTimers();
  });

  it('should return correct greeting for afternoon hours', () => {
    vi.setSystemTime(new Date('2024-01-01T14:00:00'));
    component.currentTime.set(new Date());
    expect(component.greeting()).toBe('afternoon');
    vi.useRealTimers();
  });

  it('should return correct greeting for evening hours', () => {
    vi.setSystemTime(new Date('2024-01-01T20:00:00'));
    component.currentTime.set(new Date());
    expect(component.greeting()).toBe('evening');
    vi.useRealTimers();
  });

  it('should clear intervals on destroy', () => {
    fixture.detectChanges();
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval');
    component.ngOnDestroy();
    expect(clearIntervalSpy).toHaveBeenCalled();
    clearIntervalSpy.mockRestore();
  });
});
