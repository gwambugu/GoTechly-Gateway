import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  JposMetricsService,
  JposInstanceMetrics,
  TransactionManagerMetrics
} from '../../../core/services/jpos-metrics.service';

@Component({
  selector: 'app-jpos-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './jpos-dashboard.component.html',
  styleUrl: './jpos-dashboard.component.scss'
})
export class JposDashboardComponent implements OnInit, OnDestroy {
  instances = signal<JposInstanceMetrics[]>([]);
  loading = signal(false);
  error = signal('');
  autoRefresh = signal(true);
  actionMessage = signal('');
  actionError = signal('');

  // Scale sessions form
  scaleSessionsForm = {
    instance: '',
    tmName: '',
    sessions: 10,
    maxSessions: 20
  };

  // Scale TM count form
  scaleTmCountForm = {
    instance: '',
    prefix: 'mpesa',
    count: 2
  };

  showScaleSessionsPanel = signal(false);
  showScaleTmCountPanel = signal(false);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  constructor(private jposService: JposMetricsService) {}

  ngOnInit(): void {
    this.loadData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.jposService.getMetrics().subscribe({
      next: (data) => {
        this.instances.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load jPOS metrics');
        this.loading.set(false);
      }
    });
  }

  toggleAutoRefresh(): void {
    this.autoRefresh.update(v => !v);
    if (this.autoRefresh()) {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  getTotalSummary() {
    const all = this.instances();
    let totalTMs = 0, totalSessions = 0, totalActive = 0, totalIdle = 0, totalOutstanding = 0, totalInTransit = 0;
    for (const inst of all) {
      if (inst.summary) {
        totalTMs += inst.summary.totalTMs;
        totalSessions += inst.summary.totalSessions;
        totalActive += inst.summary.totalActiveSessions;
        totalIdle += inst.summary.totalIdleSessions;
        totalOutstanding += inst.summary.totalOutstanding;
        totalInTransit += inst.summary.totalInTransit;
      }
    }
    return { totalTMs, totalSessions, totalActive, totalIdle, totalOutstanding, totalInTransit };
  }

  getInstanceNames(): string[] {
    return this.instances().map(i => i.instance);
  }

  getSessionUtilization(tm: TransactionManagerMetrics): number {
    if (tm.sessions === 0) return 0;
    return (tm.activeSessions / tm.sessions) * 100;
  }

  prefillScaleSessions(instance: string, tm: TransactionManagerMetrics): void {
    this.scaleSessionsForm = {
      instance,
      tmName: tm.name,
      sessions: tm.sessions,
      maxSessions: tm.maxSessions
    };
    this.showScaleSessionsPanel.set(true);
    this.showScaleTmCountPanel.set(false);
  }

  submitScaleSessions(): void {
    this.actionMessage.set('');
    this.actionError.set('');

    const req = {
      instance: this.scaleSessionsForm.instance || undefined,
      tmName: this.scaleSessionsForm.tmName,
      sessions: this.scaleSessionsForm.sessions,
      maxSessions: this.scaleSessionsForm.maxSessions
    };

    this.jposService.scaleSessions(req).subscribe({
      next: (results) => {
        this.actionMessage.set('Sessions scaled successfully');
        this.showScaleSessionsPanel.set(false);
        setTimeout(() => this.loadData(), 2000);
      },
      error: (err) => {
        this.actionError.set('Failed to scale sessions: ' + (err.error?.message || err.message));
      }
    });
  }

  submitScaleTmCount(): void {
    this.actionMessage.set('');
    this.actionError.set('');

    const req = {
      instance: this.scaleTmCountForm.instance || undefined,
      prefix: this.scaleTmCountForm.prefix,
      count: this.scaleTmCountForm.count
    };

    this.jposService.scaleTmCount(req).subscribe({
      next: (results) => {
        this.actionMessage.set('TM count scaled successfully. Q2 will hot-reload.');
        this.showScaleTmCountPanel.set(false);
        setTimeout(() => this.loadData(), 3000);
      },
      error: (err) => {
        this.actionError.set('Failed to scale TM count: ' + (err.error?.message || err.message));
      }
    });
  }

  clearMessages(): void {
    this.actionMessage.set('');
    this.actionError.set('');
  }

  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    this.refreshInterval = setInterval(() => this.loadData(), 10000);
  }

  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }
}
