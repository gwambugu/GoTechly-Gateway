import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MetricsService } from '../../../core/services/metrics.service';
import { RouteMetrics, MetricsSummary } from '../../../shared/models/metrics.model';

@Component({
  selector: 'app-metrics-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './metrics-dashboard.component.html',
  styleUrl: './metrics-dashboard.component.scss'
})
export class MetricsDashboardComponent implements OnInit, OnDestroy {
  metrics = signal<RouteMetrics[]>([]);
  summary = signal<MetricsSummary | null>(null);
  loading = signal(true);
  error = signal('');
  autoRefresh = signal(true);
  private initialLoad = true;

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  constructor(private metricsService: MetricsService) {}

  ngOnInit(): void {
    this.loadData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadData(): void {
    // Only show loading spinner on first load, not on auto-refresh
    if (this.initialLoad) {
      this.loading.set(true);
    }

    this.metricsService.getGatewaySummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => {}
    });

    this.metricsService.getGatewayMetrics().subscribe({
      next: (metrics) => {
        this.metrics.set(metrics ?? []);
        this.loading.set(false);
        this.initialLoad = false;
        this.error.set('');
      },
      error: () => {
        this.loading.set(false);
        this.initialLoad = false;
        this.error.set('Failed to load route metrics');
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

  getErrorRate(metric: RouteMetrics): number {
    if (metric.totalRequests === 0) return 0;
    return (metric.errorCount / metric.totalRequests) * 100;
  }

  getLatencyBarWidth(value: number): number {
    const maxLatency = Math.max(...this.metrics().map(m => m.maxLatencyMs), 1);
    return Math.min((value / maxLatency) * 100, 100);
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
