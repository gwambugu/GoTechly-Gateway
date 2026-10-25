import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../../core/services/transaction.service';
import { PaymentLogDTO } from '../../../shared/models/transaction.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, PaginationComponent],
  templateUrl: './transaction-list.component.html',
  styleUrl: './transaction-list.component.scss',
})
export class TransactionListComponent implements OnInit {
  // View state
  loading = signal(true);
  transactions = signal<PaymentLogDTO[]>([]);
  totalCount = signal(0);

  // Pagination
  currentPage = 0;
  pageSize = 20;

  // Source
  transactionSource: 'active' | 'history' | 'pending' = 'active';

  // Filters (plain properties — bound to form inputs)
  searchRequestId = '';
  filterStatus = '';
  filterPaymentType = '';

  hasActiveFilters = computed(
    () => !!(this.searchRequestId || this.filterStatus || this.filterPaymentType),
  );

  // Skeleton rows for loading state
  readonly skeletonRows = Array(8).fill(0);

  constructor(private transactionService: TransactionService) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.loading.set(true);

    const filters: Record<string, string> = {};
    if (this.searchRequestId) filters['requestId.contains'] = this.searchRequestId;
    if (this.filterStatus) filters['status.equals'] = this.filterStatus;
    if (this.filterPaymentType) filters['paymentType.equals'] = this.filterPaymentType;

    this.transactionService
      .getPaymentLogs(this.currentPage, this.pageSize, filters, this.transactionSource)
      .subscribe({
        next: (result) => {
          this.transactions.set(result.items);
          this.totalCount.set(result.totalCount);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadTransactions();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadTransactions();
  }

  switchSource(source: 'active' | 'history' | 'pending'): void {
    this.transactionSource = source;
    this.currentPage = 0;
    this.loadTransactions();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadTransactions();
  }

  clearFilters(): void {
    this.searchRequestId = '';
    this.filterStatus = '';
    this.filterPaymentType = '';
    this.currentPage = 0;
    this.loadTransactions();
  }
}
