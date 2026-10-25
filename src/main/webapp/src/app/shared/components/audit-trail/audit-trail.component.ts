import { Component, Input, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionService } from '../../../core/services/transaction.service';
import { AuditLogDTO } from '../../../shared/models/transaction.model';

@Component({
  selector: 'app-audit-trail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-trail.component.html',
  styleUrl: './audit-trail.component.scss'
})
export class AuditTrailComponent implements OnInit, OnChanges {
  @Input() requestId!: string;

  auditLogs: AuditLogDTO[] = [];
  loading = true;
  errorMessage: string | null = null;
  expandedStage: number | null = null;

  constructor(private transactionService: TransactionService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadAuditTrail();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['requestId'] && !changes['requestId'].firstChange) {
      this.loadAuditTrail();
    }
  }

  loadAuditTrail(): void {
    if (!this.requestId) return;
    this.loading = true;
    this.errorMessage = null;
    console.debug('[AuditTrail] Fetching audit logs for requestId:', this.requestId);
    this.transactionService.getAuditLogsForTransaction(this.requestId).subscribe({
      next: (logs) => {
        console.debug('[AuditTrail] Received', logs?.length ?? 0, 'records');
        this.auditLogs = logs;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[AuditTrail] Error fetching audit logs:', err.status, err.message, err);
        this.errorMessage = `Error loading audit trail: ${err.status} ${err.statusText || err.message || 'Unknown error'}`;
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleStage(index: number): void {
    this.expandedStage = this.expandedStage === index ? null : index;
  }

  getEventIcon(eventType?: string): string {
    switch (eventType) {
      case 'STAGE_EXECUTED': return '\u2713';
      case 'STAGE_HALTED': return '\u26A0';
      case 'STAGE_ERROR': return '\u2715';
      case 'PIPELINE_COMPLETED': return '\u25CF';
      default: return '\u25CB';
    }
  }

  getEventClass(eventType?: string): string {
    switch (eventType) {
      case 'STAGE_EXECUTED': return 'success';
      case 'STAGE_HALTED': return 'warning';
      case 'STAGE_ERROR': return 'error';
      case 'PIPELINE_COMPLETED': return 'info';
      default: return '';
    }
  }

  parseJson(value?: string): Record<string, string> | null {
    if (!value) return null;
    try { return JSON.parse(value); } catch { return null; }
  }

  parseChangedFields(value?: string): string[] {
    if (!value) return [];
    try { return JSON.parse(value); } catch { return []; }
  }

  getObjectEntries(obj: Record<string, string> | null): [string, string][] {
    if (!obj) return [];
    return Object.entries(obj);
  }
}
