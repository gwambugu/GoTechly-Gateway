import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecoveryService } from '../../../core/services/recovery.service';
import {
  DeclaredOutcome,
  DeferredRecovery,
  RecoveryMetrics,
  RecoveryState
} from '../../../shared/models/recovery.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

/**
 * Operator view for the deferred reconciler.
 *
 * Every row is a transaction where core banking may not match the answer a scheme already holds, so
 * the screen is built around one number — NEEDS_ATTENTION — and two things an operator can do about
 * it. It opens on that filter rather than on everything, because a list of reconciled transactions
 * is not what anyone comes here for.
 */
@Component({
  selector: 'app-recovery-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './recovery-management.component.html',
  styleUrl: './recovery-management.component.scss'
})
export class RecoveryManagementComponent implements OnInit {
  metrics = signal<RecoveryMetrics | null>(null);
  rows = signal<DeferredRecovery[]>([]);
  total = signal(0);
  loading = signal(false);
  error = signal('');
  notice = signal('');

  /** Opens on the rows that need a person, not on everything. */
  stateFilter = signal<RecoveryState | 'ALL'>('NEEDS_ATTENTION');
  currentPage = signal(0);
  pageSize = signal(20);

  expandedRequestId = signal<string | null>(null);
  actingOn = signal<string | null>(null);

  /** Per-row form state for the declared-outcome dialog. */
  outcomeChoice = signal<DeclaredOutcome>('FAILED');
  outcomeReason = signal('');
  cancelReason = signal('');

  readonly states: (RecoveryState | 'ALL')[] = [
    'NEEDS_ATTENTION', 'PENDING', 'IN_PROGRESS', 'RECONCILED', 'CANCELLED', 'ALL'
  ];

  /** Only SUCCESS/FAILED/PENDING may be supplied; UNKNOWN resolves nothing and Core refuses it. */
  readonly selectableOutcomes: DeclaredOutcome[] = ['SUCCESS', 'FAILED', 'PENDING'];

  needsAttention = computed(() => this.metrics()?.needsAttention ?? 0);

  constructor(private recovery: RecoveryService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loadMetrics();
    this.loadPage();
  }

  private loadMetrics(): void {
    this.recovery.getMetrics().subscribe({
      next: m => this.metrics.set(m),
      error: () => this.metrics.set(null)
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.error.set('');
    this.recovery.list(this.stateFilter(), this.currentPage(), this.pageSize()).subscribe({
      next: page => {
        this.rows.set(page.rows);
        this.total.set(page.total);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.messageFrom(err, 'Could not load recovery transactions'));
        this.loading.set(false);
      }
    });
  }

  onStateFilterChange(state: RecoveryState | 'ALL'): void {
    this.stateFilter.set(state);
    this.currentPage.set(0);
    this.loadPage();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadPage();
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadPage();
  }

  toggleExpanded(requestId: string): void {
    this.expandedRequestId.set(this.expandedRequestId() === requestId ? null : requestId);
    this.outcomeReason.set('');
    this.cancelReason.set('');
  }

  /**
   * An UNKNOWN row is the only one that needs — and accepts — a supplied outcome. Everything else
   * already knows what the scheme was told, and Core answers 409 rather than letting it be changed.
   */
  needsDeclaredOutcome(row: DeferredRecovery): boolean {
    return row.declaredOutcome === 'UNKNOWN';
  }

  supplyOutcome(row: DeferredRecovery): void {
    this.act(row.requestId, this.recovery.setDeclaredOutcome(
      row.requestId, this.outcomeChoice(), this.outcomeReason().trim()),
      `Declared outcome set to ${this.outcomeChoice()} — reconciliation resumed`);
  }

  requeue(row: DeferredRecovery): void {
    this.act(row.requestId, this.recovery.requeue(row.requestId),
      'Requeued with a fresh attempt budget');
  }

  cancel(row: DeferredRecovery): void {
    const reason = this.cancelReason().trim() || 'Cancelled by operator';
    this.act(row.requestId, this.recovery.cancel(row.requestId, reason),
      'Cancelled — the row stays as the record of what happened');
  }

  private act(requestId: string, call: { subscribe: Function }, successMessage: string): void {
    this.actingOn.set(requestId);
    this.error.set('');
    this.notice.set('');
    (call as any).subscribe({
      next: () => {
        this.notice.set(`${requestId}: ${successMessage}`);
        this.actingOn.set(null);
        this.expandedRequestId.set(null);
        this.refresh();
      },
      error: (err: any) => {
        this.error.set(this.messageFrom(err, `Action failed for ${requestId}`));
        this.actingOn.set(null);
      }
    });
  }

  /** Core returns `{ error: "..." }` for 400/409; surface that rather than a bare status code. */
  private messageFrom(err: any, fallback: string): string {
    return err?.error?.error ?? err?.message ?? fallback;
  }

  stateRowClass(state: RecoveryState): string {
    switch (state) {
      case 'NEEDS_ATTENTION': return 'row-alert';
      case 'PENDING':         return 'row-warn';
      case 'RECONCILED':      return 'row-success';
      default:                return '';
    }
  }

  stateClass(state: RecoveryState): string {
    switch (state) {
      case 'NEEDS_ATTENTION': return 'badge-danger';
      case 'RECONCILED': return 'badge-success';
      case 'CANCELLED': return 'badge-muted';
      case 'IN_PROGRESS': return 'badge-info';
      default: return 'badge-warn';
    }
  }

  outcomeClass(outcome: DeclaredOutcome): string {
    switch (outcome) {
      case 'UNKNOWN': return 'badge-danger';
      case 'SUCCESS': return 'badge-success';
      case 'FAILED': return 'badge-warn';
      default: return 'badge-info';
    }
  }
}
