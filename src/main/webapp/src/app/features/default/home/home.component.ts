import { Component, OnInit, OnDestroy, signal, computed, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { AdminUserDTO } from '../../../shared/models/user.model';
import { MetricsService } from '../../../core/services/metrics.service';
import { ServiceHealthService } from '../../../core/services/service-health.service';
import { PluginService } from '../../../core/services/plugin.service';
import { RecoveryService } from '../../../core/services/recovery.service';
import { MetricsSummary } from '../../../shared/models/metrics.model';
import { ServiceHealth } from '../../../shared/models/service.model';
import { RecoveryMetrics } from '../../../shared/models/recovery.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit, OnDestroy {
  loading = signal(true);
  refreshing = signal(false);
  lastUpdated = signal<Date | null>(null);

  user = signal<AdminUserDTO | null>(null);
  metricsSummary = signal<MetricsSummary | null>(null);
  services = signal<ServiceHealth[]>([]);
  activePluginCount = signal(0);
  recoveryMetrics = signal<RecoveryMetrics | null>(null);

  needsAttentionCount = computed(() => this.recoveryMetrics()?.needsAttention ?? 0);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  });

  successRateClass = computed(() => {
    const r = this.metricsSummary()?.successRate ?? 0;
    if (r >= 99) return 'value--success';
    if (r >= 95) return 'value--warning';
    return 'value--danger';
  });

  maxLatencyMs = computed(() =>
    Math.max(...this.services().map((s) => s.responseTimeMs), 1),
  );

  // ── Donut chart ──────────────────────────────────────────────────────────────
  readonly DONUT_CIRC = +(2 * Math.PI * 56).toFixed(2); // 351.86

  successRateDash = computed(() => {
    const rate = this.metricsSummary()?.successRate ?? 0;
    const filled = +((rate / 100) * this.DONUT_CIRC).toFixed(1);
    return `${filled} ${this.DONUT_CIRC}`;
  });

  successRateStroke = computed(() => {
    const r = this.metricsSummary()?.successRate ?? 0;
    if (r >= 99) return '#059669';
    if (r >= 95) return '#d97706';
    return '#dc2626';
  });

  // ── Performance bars ──────────────────────────────────────────────────────────
  gatewayLatencyPct = computed(() =>
    Math.min(Math.round(((this.metricsSummary()?.avgLatencyMs ?? 0) / 2000) * 100), 100),
  );

  errorRatePct = computed(() =>
    Math.min(+(100 - (this.metricsSummary()?.successRate ?? 100)).toFixed(1), 100),
  );

  // ── System status ─────────────────────────────────────────────────────────────
  servicesUpCount = computed(() => this.services().filter(s => s.status === 'UP').length);
  servicesDownCount = computed(() => this.services().filter(s => s.status === 'DOWN').length);

  constructor(
    private authService: AuthService,
    private metricsService: MetricsService,
    private serviceHealthService: ServiceHealthService,
    private pluginService: PluginService,
    private recoveryService: RecoveryService,
  ) {
    // After login the auth service fetches /api/account asynchronously, so the
    // user signal may still be null when ngOnInit runs.  This effect re-fires
    // as soon as currentUser is populated and triggers the initial data load,
    // which fixes the blank dashboard that required a manual refresh.
    effect(() => {
      const user = this.authService.currentUser();
      this.user.set(user);
      if (user && !untracked(() => this.lastUpdated())) {
        queueMicrotask(() => this.loadDashboardData(true));
      }
    });
  }

  ngOnInit(): void {
    this.refreshInterval = setInterval(() => this.loadDashboardData(false), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  isAdmin(): boolean {
    return this.user()?.authorities?.includes('ROLE_ADMIN') ?? false;
  }

  manualRefresh(): void {
    this.loadDashboardData(false);
  }

  latencyPct(ms: number): number {
    return Math.round((ms / this.maxLatencyMs()) * 100);
  }

  loadDashboardData(initial: boolean): void {
    if (!this.isAdmin()) {
      this.loading.set(false);
      return;
    }

    if (initial) this.loading.set(true);
    else this.refreshing.set(true);

    forkJoin({
      metrics: this.metricsService.getGatewaySummary().pipe(catchError(() => of(null))),
      services: this.serviceHealthService.getServiceHealth().pipe(catchError(() => of([]))),
      plugins: this.pluginService.getPlugins().pipe(catchError(() => of([]))),
      recovery: this.recoveryService.getMetrics().pipe(catchError(() => of(null))),
    }).subscribe(({ metrics, services, plugins, recovery }) => {
      if (metrics) this.metricsSummary.set(metrics);
      this.services.set(services as ServiceHealth[]);
      this.activePluginCount.set(
        (plugins as { paymentTypes: unknown[] }[]).reduce(
          (sum, p) => sum + p.paymentTypes.length,
          0,
        ),
      );
      if (recovery) this.recoveryMetrics.set(recovery as RecoveryMetrics);
      this.loading.set(false);
      this.refreshing.set(false);
      this.lastUpdated.set(new Date());
    });
  }
}
