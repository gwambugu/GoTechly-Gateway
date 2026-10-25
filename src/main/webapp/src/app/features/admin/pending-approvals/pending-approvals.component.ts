import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigManagementService } from '../../../core/services/config-management.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/auth/auth.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { PendingChangeDTO, ENTITY_LABELS, EntityType } from '../../../shared/models/config.model';

@Component({
  selector: 'app-pending-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './pending-approvals.component.html',
  styleUrl: './pending-approvals.component.scss'
})
export class PendingApprovalsComponent implements OnInit {

  items: PendingChangeDTO[] = [];
  totalCount = 0;
  currentPage = 0;
  pageSize = 20;
  loading = true;

  filterEntityType = '';
  filterStatus = 'PENDING';

  /** Entity-type filter options, driven off ENTITY_LABELS so new maker-checker
   *  entities (e.g. LINKED_WALLET_SUBSCRIPTION) appear automatically. */
  readonly entityTypeOptions = (Object.entries(ENTITY_LABELS) as [EntityType, string][])
    .map(([value, label]) => ({ value, label }));

  // Detail view
  selectedItem: PendingChangeDTO | null = null;
  rejectRemarks = '';
  parsedPayload: Record<string, any> | null = null;
  parsedPrevious: Record<string, any> | null = null;
  diffKeys: string[] = [];

  constructor(
    private configService: ConfigManagementService,
    private toast: ToastService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadItems();
  }

  loadItems(): void {
    this.loading = true;
    const filters: Record<string, string> = {};
    if (this.filterEntityType) filters['entityType.equals'] = this.filterEntityType;
    if (this.filterStatus) filters['status.equals'] = this.filterStatus;

    this.configService.getPendingChanges(this.currentPage, this.pageSize, filters).subscribe({
      next: (result) => {
        this.items = result.items;
        this.totalCount = result.totalCount;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load pending changes');
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadItems();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadItems();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadItems();
  }

  selectItem(item: PendingChangeDTO): void {
    this.selectedItem = item;
    this.rejectRemarks = '';
    this.parsedPayload = this.parseJson(item.payload);
    this.parsedPrevious = this.parseJson(item.previousValue);
    this.computeDiff();
  }

  closeDetail(): void {
    this.selectedItem = null;
    this.parsedPayload = null;
    this.parsedPrevious = null;
    this.diffKeys = [];
  }

  approve(): void {
    if (!this.selectedItem?.id) return;
    this.configService.approveChange(this.selectedItem.id).subscribe({
      next: () => {
        this.toast.success('Change approved successfully');
        this.closeDetail();
        this.loadItems();
      },
      error: (err) => {
        this.toast.error('Failed to approve: ' + (err.error?.message || err.message));
      }
    });
  }

  reject(): void {
    if (!this.selectedItem?.id) return;
    // Capture before closeDetail() nulls selectedItem — drives reject vs cancel wording.
    const cancelling = this.isOwnChange(this.selectedItem);
    this.configService.rejectChange(this.selectedItem.id, this.rejectRemarks).subscribe({
      next: () => {
        this.toast.success(cancelling ? 'Change cancelled' : 'Change rejected');
        this.closeDetail();
        this.loadItems();
      },
      error: (err) => {
        this.toast.error((cancelling ? 'Failed to cancel: ' : 'Failed to reject: ')
          + (err.error?.message || err.message));
      }
    });
  }

  getEntityLabel(type?: string): string {
    return type ? (ENTITY_LABELS[type as EntityType] || type) : '-';
  }

  /**
   * True when the logged-in user is the maker of the given change. The route is already
   * behind adminGuard, so the only remaining maker-checker rule to enforce in the UI is
   * that a maker can't review their own change (backend also enforces maker !== checker).
   */
  isOwnChange(item: PendingChangeDTO | null = this.selectedItem): boolean {
    const login = this.auth.currentUser()?.login;
    return !!login && !!item && login === item.makerUsername;
  }

  private parseJson(value?: string): Record<string, any> | null {
    if (!value) return null;
    try {
      return JSON.parse(value);
    } catch {
      return null;
    }
  }

  private computeDiff(): void {
    if (!this.parsedPayload) {
      this.diffKeys = [];
      return;
    }
    const allKeys = new Set<string>();
    Object.keys(this.parsedPayload).forEach(k => allKeys.add(k));
    if (this.parsedPrevious) {
      Object.keys(this.parsedPrevious).forEach(k => allKeys.add(k));
    }
    // Filter to keys where value differs
    this.diffKeys = Array.from(allKeys).filter(k => {
      const newVal = JSON.stringify(this.parsedPayload?.[k] ?? null);
      const oldVal = JSON.stringify(this.parsedPrevious?.[k] ?? null);
      return newVal !== oldVal;
    }).sort();
  }

  isChanged(key: string): boolean {
    return this.diffKeys.includes(key);
  }

  getPayloadKeys(): string[] {
    if (!this.parsedPayload) return [];
    return Object.keys(this.parsedPayload).filter(k => k !== 'id').sort();
  }

  formatValue(val: any): string {
    if (val == null) return '-';
    if (typeof val === 'object') return JSON.stringify(val);
    return String(val);
  }
}
