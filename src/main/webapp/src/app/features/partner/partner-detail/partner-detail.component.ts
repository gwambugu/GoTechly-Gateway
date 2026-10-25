import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PartnerService } from '../../../core/services/partner.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  PartnerDTO, PartnerAccountDTO, PartnerServiceDTO,
  PartnerTransactionLimitDTO, PartnerConfigDTO,
  AccountType, Direction,
  PARTNER_STATUS_LABELS, PARTNER_TYPE_LABELS, ACCOUNT_TYPE_LABELS
} from '../../../shared/models/partner.model';

type TabType = 'details' | 'accounts' | 'services' | 'limits' | 'configs';

@Component({
  selector: 'app-partner-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './partner-detail.component.html',
  styleUrl: './partner-detail.component.scss'
})
export class PartnerDetailComponent implements OnInit {
  partnerId!: number;
  partner = signal<PartnerDTO | null>(null);

  activeTab = signal<TabType>('details');
  loading = signal(false);
  error = signal('');
  successMessage = signal('');

  // Child entity data
  accounts = signal<PartnerAccountDTO[]>([]);
  services = signal<PartnerServiceDTO[]>([]);
  limits = signal<PartnerTransactionLimitDTO[]>([]);
  configs = signal<PartnerConfigDTO[]>([]);

  // Inline form state
  showAccountForm = signal(false);
  showServiceForm = signal(false);
  showLimitForm = signal(false);
  showConfigForm = signal(false);

  editingAccount: PartnerAccountDTO = {};
  editingService: PartnerServiceDTO = {};
  editingLimit: PartnerTransactionLimitDTO = {};
  editingConfig: PartnerConfigDTO = {};

  accountTypes: AccountType[] = ['SETTLEMENT', 'FEE', 'SUSPENSE', 'FLOAT'];
  directions: Direction[] = ['INWARD', 'OUTWARD'];

  accountUniquenessError = signal(false);

  readonly statusLabels = PARTNER_STATUS_LABELS;
  readonly typeLabels = PARTNER_TYPE_LABELS;
  readonly accountTypeLabels = ACCOUNT_TYPE_LABELS;

  constructor(
    private partnerService: PartnerService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.partnerId = +this.route.snapshot.paramMap.get('id')!;
    this.loadPartner();
  }

  loadPartner(): void {
    this.loading.set(true);
    this.partnerService.getPartner(this.partnerId).subscribe({
      next: (p) => {
        this.partner.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load partner');
        this.loading.set(false);
      }
    });
  }

  setActiveTab(tab: TabType): void {
    this.activeTab.set(tab);
    this.clearMessages();

    switch (tab) {
      case 'accounts': this.loadAccounts(); break;
      case 'services': this.loadServices(); break;
      case 'limits': this.loadLimits(); break;
      case 'configs': this.loadConfigs(); break;
    }
  }

  // --- Accounts ---

  loadAccounts(): void {
    this.loading.set(true);
    this.partnerService.getAccounts(this.partnerId).subscribe({
      next: (a) => { this.accounts.set(a); this.loading.set(false); },
      error: () => { this.error.set('Failed to load accounts'); this.loading.set(false); }
    });
  }

  newAccount(): void {
    this.editingAccount = { partnerId: this.partnerId, accountType: 'SETTLEMENT', currencyCode: 'KES', isPrimary: false };
    this.showAccountForm.set(true);
    this.accountUniquenessError.set(false);
  }

  editAccount(account: PartnerAccountDTO): void {
    this.editingAccount = { ...account };
    this.showAccountForm.set(true);
    this.accountUniquenessError.set(false);
  }

  checkAccountUniqueness(): void {
    if (!this.editingAccount.accountNumber?.trim()) return;
    this.partnerService.checkAccountUniqueness(this.editingAccount.accountNumber).subscribe({
      next: (exists) => this.accountUniquenessError.set(exists)
    });
  }

  saveAccount(): void {
    if (this.accountUniquenessError() && !this.editingAccount.id) return;
    this.loading.set(true);
    const obs = this.editingAccount.id
      ? this.partnerService.updateAccount(this.partnerId, this.editingAccount.id, this.editingAccount)
      : this.partnerService.createAccount(this.partnerId, this.editingAccount);

    obs.subscribe({
      next: () => {
        this.showAccountForm.set(false);
        this.successMessage.set('Account saved');
        this.loadAccounts();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => { this.error.set(err.error?.detail || 'Failed to save account'); this.loading.set(false); }
    });
  }

  deleteAccount(account: PartnerAccountDTO): void {
    if (!account.id || !confirm('Delete this account?')) return;
    this.partnerService.deleteAccount(this.partnerId, account.id).subscribe({
      next: () => {
        this.successMessage.set('Account deleted');
        this.loadAccounts();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => this.error.set('Failed to delete account')
    });
  }

  // --- Services ---

  loadServices(): void {
    this.loading.set(true);
    this.partnerService.getServices(this.partnerId).subscribe({
      next: (s) => { this.services.set(s); this.loading.set(false); },
      error: () => { this.error.set('Failed to load services'); this.loading.set(false); }
    });
  }

  newService(): void {
    this.editingService = { partnerId: this.partnerId, direction: 'INWARD', status: 'ACTIVE' };
    this.showServiceForm.set(true);
  }

  editService(service: PartnerServiceDTO): void {
    this.editingService = { ...service };
    this.showServiceForm.set(true);
  }

  saveService(): void {
    this.loading.set(true);
    const obs = this.editingService.id
      ? this.partnerService.updateService(this.partnerId, this.editingService.id, this.editingService)
      : this.partnerService.createService(this.partnerId, this.editingService);

    obs.subscribe({
      next: () => {
        this.showServiceForm.set(false);
        this.successMessage.set('Service saved');
        this.loadServices();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => { this.error.set(err.error?.detail || 'Failed to save service'); this.loading.set(false); }
    });
  }

  deleteService(service: PartnerServiceDTO): void {
    if (!service.id || !confirm('Delete this service assignment?')) return;
    this.partnerService.deleteService(this.partnerId, service.id).subscribe({
      next: () => {
        this.successMessage.set('Service deleted');
        this.loadServices();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => this.error.set('Failed to delete service')
    });
  }

  // --- Limits ---

  loadLimits(): void {
    this.loading.set(true);
    this.partnerService.getLimits(this.partnerId).subscribe({
      next: (l) => { this.limits.set(l); this.loading.set(false); },
      error: () => { this.error.set('Failed to load limits'); this.loading.set(false); }
    });
  }

  newLimit(): void {
    this.editingLimit = { partnerId: this.partnerId, direction: 'INWARD', currencyCode: 'KES' };
    this.showLimitForm.set(true);
  }

  editLimit(limit: PartnerTransactionLimitDTO): void {
    this.editingLimit = { ...limit };
    this.showLimitForm.set(true);
  }

  saveLimit(): void {
    this.loading.set(true);
    const obs = this.editingLimit.id
      ? this.partnerService.updateLimit(this.partnerId, this.editingLimit.id, this.editingLimit)
      : this.partnerService.createLimit(this.partnerId, this.editingLimit);

    obs.subscribe({
      next: () => {
        this.showLimitForm.set(false);
        this.successMessage.set('Limit saved');
        this.loadLimits();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => { this.error.set(err.error?.detail || 'Failed to save limit'); this.loading.set(false); }
    });
  }

  deleteLimit(limit: PartnerTransactionLimitDTO): void {
    if (!limit.id || !confirm('Delete this limit?')) return;
    this.partnerService.deleteLimit(this.partnerId, limit.id).subscribe({
      next: () => {
        this.successMessage.set('Limit deleted');
        this.loadLimits();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => this.error.set('Failed to delete limit')
    });
  }

  // --- Configs ---

  loadConfigs(): void {
    this.loading.set(true);
    this.partnerService.getConfigs(this.partnerId).subscribe({
      next: (c) => { this.configs.set(c); this.loading.set(false); },
      error: () => { this.error.set('Failed to load configs'); this.loading.set(false); }
    });
  }

  newConfig(): void {
    this.editingConfig = { partnerId: this.partnerId };
    this.showConfigForm.set(true);
  }

  editConfig(config: PartnerConfigDTO): void {
    this.editingConfig = { ...config };
    this.showConfigForm.set(true);
  }

  saveConfig(): void {
    this.loading.set(true);
    const obs = this.editingConfig.id
      ? this.partnerService.updateConfig(this.partnerId, this.editingConfig.id, this.editingConfig)
      : this.partnerService.createConfig(this.partnerId, this.editingConfig);

    obs.subscribe({
      next: () => {
        this.showConfigForm.set(false);
        this.successMessage.set('Config saved');
        this.loadConfigs();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => { this.error.set(err.error?.detail || 'Failed to save config'); this.loading.set(false); }
    });
  }

  deleteConfig(config: PartnerConfigDTO): void {
    if (!config.id || !confirm('Delete this config?')) return;
    this.partnerService.deleteConfig(this.partnerId, config.id).subscribe({
      next: () => {
        this.successMessage.set('Config deleted');
        this.loadConfigs();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => this.error.set('Failed to delete config')
    });
  }

  // --- Role helpers ---

  isMaker(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.authorities?.some(r => r === 'ROLE_PARTNER_MAKER' || r === 'ROLE_ADMIN') ?? false;
  }

  isChecker(): boolean {
    const user = this.authService.getCurrentUser();
    return user?.authorities?.some(r => r === 'ROLE_PARTNER_CHECKER' || r === 'ROLE_ADMIN') ?? false;
  }

  getStatusClass(status?: string): string {
    const classes: Record<string, string> = {
      ACTIVE: 'status-active',
      PENDING_APPROVAL: 'status-pending',
      SUSPENDED: 'status-suspended',
      REJECTED: 'status-rejected'
    };
    return status ? classes[status] || '' : '';
  }

  cancelForm(type: string): void {
    switch (type) {
      case 'account': this.showAccountForm.set(false); break;
      case 'service': this.showServiceForm.set(false); break;
      case 'limit': this.showLimitForm.set(false); break;
      case 'config': this.showConfigForm.set(false); break;
    }
  }

  clearMessages(): void {
    this.error.set('');
    this.successMessage.set('');
  }
}
