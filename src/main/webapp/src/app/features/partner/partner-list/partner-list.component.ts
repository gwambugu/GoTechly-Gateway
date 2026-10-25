import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { PartnerService } from '../../../core/services/partner.service';
import { AuthService } from '../../../core/auth/auth.service';
import { PartnerDTO, PartnerStatus, PARTNER_STATUS_LABELS, PARTNER_TYPE_LABELS } from '../../../shared/models/partner.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type TabType = 'all' | 'pending' | 'active' | 'suspended';

@Component({
  selector: 'app-partner-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PaginationComponent],
  templateUrl: './partner-list.component.html',
  styleUrl: './partner-list.component.scss'
})
export class PartnerListComponent implements OnInit {
  activeTab = signal<TabType>('all');
  partners = signal<PartnerDTO[]>([]);

  currentPage = signal(0);
  pageSize = signal(20);
  totalItems = signal(0);

  searchQuery = '';
  loading = signal(false);
  error = signal('');
  successMessage = signal('');

  rejectPartnerId = signal<number | null>(null);
  rejectRemarks = '';

  readonly statusLabels = PARTNER_STATUS_LABELS;
  readonly typeLabels = PARTNER_TYPE_LABELS;

  constructor(
    private partnerService: PartnerService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPartners();
  }

  setActiveTab(tab: TabType): void {
    this.activeTab.set(tab);
    this.currentPage.set(0);
    this.clearMessages();
    this.loadPartners();
  }

  loadPartners(): void {
    this.loading.set(true);
    const filters: Record<string, string> = {};

    if (this.activeTab() !== 'all') {
      const statusMap: Record<string, string> = {
        pending: 'PENDING_APPROVAL',
        active: 'ACTIVE',
        suspended: 'SUSPENDED'
      };
      filters['status.equals'] = statusMap[this.activeTab()];
    }

    if (this.searchQuery.trim()) {
      filters['clientShortName.contains'] = this.searchQuery.trim();
    }

    this.partnerService.getPartners(this.currentPage(), this.pageSize(), filters).subscribe({
      next: (result) => {
        this.partners.set(result.items);
        this.totalItems.set(result.totalCount);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load partners');
        this.loading.set(false);
      }
    });
  }

  search(): void {
    this.currentPage.set(0);
    this.loadPartners();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadPartners();
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadPartners();
  }

  viewPartner(partner: PartnerDTO): void {
    this.router.navigate(['/partners', partner.id]);
  }

  editPartner(partner: PartnerDTO): void {
    this.router.navigate(['/partners', partner.id, 'edit']);
  }

  approvePartner(partner: PartnerDTO): void {
    if (!partner.id || !confirm(`Approve partner "${partner.legalName}"?`)) return;

    this.loading.set(true);
    const username = this.authService.getCurrentUser()?.login ?? '';
    this.partnerService.approvePartner(partner.id, username).subscribe({
      next: () => {
        this.successMessage.set(`Partner "${partner.legalName}" approved successfully`);
        this.loadPartners();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to approve partner');
        this.loading.set(false);
      }
    });
  }

  showRejectDialog(partner: PartnerDTO): void {
    this.rejectPartnerId.set(partner.id!);
    this.rejectRemarks = '';
  }

  cancelReject(): void {
    this.rejectPartnerId.set(null);
    this.rejectRemarks = '';
  }

  confirmReject(): void {
    const id = this.rejectPartnerId();
    if (!id || !this.rejectRemarks.trim()) return;

    this.loading.set(true);
    const username = this.authService.getCurrentUser()?.login ?? '';
    this.partnerService.rejectPartner(id, username, this.rejectRemarks.trim()).subscribe({
      next: () => {
        this.successMessage.set('Partner rejected');
        this.rejectPartnerId.set(null);
        this.rejectRemarks = '';
        this.loadPartners();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to reject partner');
        this.loading.set(false);
      }
    });
  }

  suspendPartner(partner: PartnerDTO): void {
    if (!partner.id || !confirm(`Suspend partner "${partner.legalName}"?`)) return;

    this.loading.set(true);
    const username = this.authService.getCurrentUser()?.login ?? '';
    this.partnerService.suspendPartner(partner.id, username).subscribe({
      next: () => {
        this.successMessage.set(`Partner "${partner.legalName}" suspended`);
        this.loadPartners();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to suspend partner');
        this.loading.set(false);
      }
    });
  }

  activatePartner(partner: PartnerDTO): void {
    if (!partner.id || !confirm(`Activate partner "${partner.legalName}"?`)) return;

    this.loading.set(true);
    const username = this.authService.getCurrentUser()?.login ?? '';
    this.partnerService.activatePartner(partner.id, username).subscribe({
      next: () => {
        this.successMessage.set(`Partner "${partner.legalName}" activated`);
        this.loadPartners();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to activate partner');
        this.loading.set(false);
      }
    });
  }

  deletePartner(partner: PartnerDTO): void {
    if (!partner.id || !confirm(`Delete partner "${partner.legalName}"? This cannot be undone.`)) return;

    this.loading.set(true);
    this.partnerService.deletePartner(partner.id).subscribe({
      next: () => {
        this.successMessage.set(`Partner "${partner.legalName}" deleted`);
        this.loadPartners();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => {
        this.error.set('Failed to delete partner');
        this.loading.set(false);
      }
    });
  }

  getStatusClass(status?: PartnerStatus): string {
    const classes: Record<string, string> = {
      ACTIVE: 'status-active',
      PENDING_APPROVAL: 'status-pending',
      SUSPENDED: 'status-suspended',
      REJECTED: 'status-rejected'
    };
    return status ? classes[status] || '' : '';
  }

  isMaker(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.authorities?.some(r => r === 'ROLE_PARTNER_MAKER' || r === 'ROLE_ADMIN') ?? false;
  }

  isChecker(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.authorities?.some(r => r === 'ROLE_PARTNER_CHECKER' || r === 'ROLE_ADMIN') ?? false;
  }

  isAdmin(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.authorities?.includes('ROLE_ADMIN') ?? false;
  }

  clearMessages(): void {
    this.error.set('');
    this.successMessage.set('');
  }
}
