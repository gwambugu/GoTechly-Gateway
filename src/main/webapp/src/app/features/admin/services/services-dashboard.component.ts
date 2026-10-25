import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServiceHealthService } from '../../../core/services/service-health.service';
import { NodeRequest, ServiceHealth } from '../../../shared/models/service.model';

@Component({
  selector: 'app-services-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './services-dashboard.component.html',
  styleUrl: './services-dashboard.component.scss'
})
export class ServicesDashboardComponent implements OnInit, OnDestroy {
  services = signal<ServiceHealth[]>([]);
  loading = signal(false);
  error = signal('');
  notice = signal('');
  autoRefresh = signal(true);

  /** Instance name currently mid-drain/undrain, so its buttons can be disabled. */
  busyNode = signal<string | null>(null);

  /**
   * Instance whose drain the last-routable-node guard just refused. The Force override appears only
   * after that refusal — taking the whole platform out of service should never be a button sitting
   * on the page waiting to be misclicked.
   */
  guardRefused = signal<string | null>(null);

  private refreshInterval: ReturnType<typeof setInterval> | null = null;
  private readonly REFRESH_INTERVAL_MS = 15000;
  /** While an instance is draining the operator is watching a countdown, not a dashboard. */
  private readonly DRAINING_REFRESH_INTERVAL_MS = 3000;
  /** One LB probe interval (10s) + the LB instance-list cache TTL (35s) + slack. */
  private readonly LB_CATCH_UP_MS = 60000;
  /**
   * Past this, a drain is no longer a deploy in progress. Matches Core's own `gotechly.drain.warn-after`
   * default, so the page and the node's WARN log escalate at the same moment.
   */
  private readonly STALE_DRAIN_SECONDS = 15 * 60;

  constructor(private serviceHealthService: ServiceHealthService) {}

  ngOnInit(): void {
    this.loadServices();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  loadServices(): void {
    this.loading.set(true);
    this.serviceHealthService.getServiceHealth().subscribe({
      next: (services) => {
        this.services.set(services);
        this.loading.set(false);
        this.error.set('');
        this.retuneRefresh();
      },
      error: () => {
        this.error.set('Failed to load service health');
        this.loading.set(false);
      }
    });
  }

  /** UP | DRAINING | DOWN — drain deliberately does not move `status`, so it is merged here. */
  displayStatus(service: ServiceHealth): string {
    if (service.status === 'DOWN') {
      return 'DOWN';
    }
    return service.drainState === 'DRAINING' ? 'DRAINING' : 'UP';
  }

  canDrain(service: ServiceHealth): boolean {
    return service.status === 'UP' && service.drainState === 'ROUTABLE';
  }

  canUndrain(service: ServiceHealth): boolean {
    return service.drainState === 'DRAINING';
  }

  /**
   * A drain nobody came back to. Nothing fails — the other instances serve everything — so the
   * cluster just runs short until peak load or a second instance dies. The card escalates so the
   * next person to open this page cannot miss it.
   */
  isStaleDrain(service: ServiceHealth): boolean {
    return service.drainState === 'DRAINING' && service.stateForSeconds >= this.STALE_DRAIN_SECONDS;
  }

  /** "3h 12m" — a duration an operator can judge at a glance, unlike a seconds count. */
  drainedFor(service: ServiceHealth): string {
    const total = service.stateForSeconds ?? 0;
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor((total % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return minutes > 0 ? `${minutes}m` : `${total}s`;
  }

  /**
   * What the operator is still waiting for. `quiet` is the only safe-to-deploy signal — an idle
   * request count is not enough while an EOD archival is still running plugin code.
   */
  waitingOn(service: ServiceHealth): string {
    if (service.drainState !== 'DRAINING') {
      return '';
    }
    if (service.quiet) {
      return 'Quiet — safe to deploy and restart';
    }
    const parts: string[] = [];
    if (service.inFlightHttp > 0) {
      parts.push(`${service.inFlightHttp} request(s) in flight`);
    }
    if (service.activeBackgroundJobs?.length) {
      parts.push(service.activeBackgroundJobs.join(', '));
    }
    return parts.length ? `Waiting on ${parts.join('; ')}` : 'Waiting…';
  }

  /** "POST /payment/api/v1/... from 10.0.0.5 (via 41.2.3.4) · 120ms" */
  describeRequest(r: NodeRequest): string {
    const via = r.forwardedFor ? ` (via ${r.forwardedFor})` : '';
    return `${r.method ?? '?'} ${r.uri ?? '?'} from ${r.remoteAddr ?? 'unknown'}${via} · ${r.ageMs}ms`;
  }

  /**
   * Arrivals shortly after a drain are expected: the LB only notices at its next probe (10s) and
   * then serves its cached list for up to ~35s more. The latest arrival landing after that window
   * means some route is not asking `/routable` at all.
   */
  arrivalsLookWrong(service: ServiceHealth): boolean {
    const latest = service.recentArrivalsWhileDraining?.[0];
    if (!latest) {
      return false;
    }
    const drainStartedMs = Date.now() - (service.stateForSeconds ?? 0) * 1000;
    return Date.parse(latest.startedAt) - drainStartedMs > this.LB_CATCH_UP_MS;
  }

  drain(service: ServiceHealth): void {
    this.act(service.name, this.serviceHealthService.drain(service.name),
      `${service.name} is draining. It will leave the load-balancer pool within ~10s; wait for "Quiet" before deploying.`);
  }

  undrain(service: ServiceHealth): void {
    this.act(service.name, this.serviceHealthService.undrain(service.name),
      `${service.name} is back in the pool.`);
  }

  forceDrain(service: ServiceHealth): void {
    this.act(service.name, this.serviceHealthService.drain(service.name, true),
      `${service.name} drained with the last-node guard overridden — no instance may be taking traffic now.`);
  }

  toggleAutoRefresh(): void {
    if (this.autoRefresh()) {
      this.autoRefresh.set(false);
      this.stopAutoRefresh();
    } else {
      this.autoRefresh.set(true);
      this.startAutoRefresh();
    }
  }

  private act(name: string, call: ReturnType<ServiceHealthService['drain']>, success: string): void {
    this.busyNode.set(name);
    this.notice.set('');
    this.error.set('');
    call.subscribe({
      next: () => {
        this.busyNode.set(null);
        this.guardRefused.set(null);
        this.notice.set(success);
        this.loadServices();
      },
      error: (err) => {
        this.busyNode.set(null);
        this.guardRefused.set(err?.status === 409 ? name : null);
        this.error.set(this.describeFailure(err, name));
        this.loadServices();
      }
    });
  }

  /**
   * The 409 is the one an operator must actually read: it means draining this node would leave the
   * pool empty and 503 every request, so it is surfaced verbatim rather than as "request failed".
   */
  private describeFailure(err: { status?: number; error?: { message?: string } }, name: string): string {
    if (err?.error?.message) {
      return err.error.message;
    }
    if (err?.status === 409) {
      return `${name} is the last instance taking traffic — draining it would take the platform down.`;
    }
    return `Could not change the drain state of ${name}.`;
  }

  /** Poll faster while any instance is draining, so "Quiet" appears promptly. */
  private retuneRefresh(): void {
    if (this.autoRefresh()) {
      this.startAutoRefresh();
    }
  }

  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    const anyDraining = this.services().some(s => s.drainState === 'DRAINING');
    const interval = anyDraining ? this.DRAINING_REFRESH_INTERVAL_MS : this.REFRESH_INTERVAL_MS;
    this.refreshInterval = setInterval(() => this.loadServices(), interval);
  }

  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }
}
