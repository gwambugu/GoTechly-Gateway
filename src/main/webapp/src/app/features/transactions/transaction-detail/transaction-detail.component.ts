import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { PaymentLogDTO } from '../../../shared/models/transaction.model';
import { AuditTrailComponent } from '../../../shared/components/audit-trail/audit-trail.component';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, AuditTrailComponent],
  templateUrl: './transaction-detail.component.html',
  styleUrl: './transaction-detail.component.scss',
})
export class TransactionDetailComponent implements OnInit {
  loading = signal(true);
  transaction = signal<PaymentLogDTO | null>(null);

  transactionSource: 'active' | 'history' | 'pending' = 'history';

  expandedSections: Record<string, boolean> = {
    sender: true,
    receiver: true,
    cbs: false,
    psp: false,
    additional: false,
    audit: true,
  };

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService,
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.transactionSource =
      (this.route.snapshot.queryParamMap.get('source') as 'active' | 'history' | 'pending') ||
      'history';

    this.transactionService.getPaymentLog(id, this.transactionSource).subscribe({
      next: (tx) => {
        this.transaction.set(tx);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  toggleSection(section: string): void {
    this.expandedSections[section] = !this.expandedSections[section];
  }

  getAdditionalFieldEntries(fields?: Record<string, string>): [string, string][] {
    if (!fields) return [];
    return Object.entries(fields);
  }

  hasAdditionalFields(tx: PaymentLogDTO): boolean {
    return (
      this.getAdditionalFieldEntries(tx.detailsAdditionalFields).length > 0 ||
      this.getAdditionalFieldEntries(tx.senderAdditionalFields).length > 0 ||
      this.getAdditionalFieldEntries(tx.receiverAdditionalFields).length > 0
    );
  }
}
