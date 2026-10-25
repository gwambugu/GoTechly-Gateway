import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PartnerService } from '../../../core/services/partner.service';
import { PartnerDTO, PartnerType, KycStatus } from '../../../shared/models/partner.model';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-partner-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './partner-form.component.html',
  styleUrl: './partner-form.component.scss'
})
export class PartnerFormComponent implements OnInit {
  isEdit = false;
  partnerId: number | null = null;
  saving = signal(false);
  loading = signal(false);
  error = signal('');

  partner: PartnerDTO = {
    partnerType: 'BANK',
    kycStatus: 'PENDING',
    enabled: true
  };

  partnerTypes: PartnerType[] = ['BANK', 'MTO', 'AGGREGATOR', 'MERCHANT'];
  kycStatuses: KycStatus[] = ['VERIFIED', 'PENDING', 'EXPIRED'];

  // Uniqueness validation
  uniquenessErrors = signal<Record<string, boolean>>({});
  private uniquenessCheck$ = new Subject<{ field: string; value: string }>();

  constructor(
    private partnerService: PartnerService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.partnerId = +id;
      this.loadPartner();
    }

    this.uniquenessCheck$.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, curr) => prev.field === curr.field && prev.value === curr.value),
      switchMap(({ field, value }) => {
        if (!value.trim()) return of(null);
        return this.partnerService.checkUniqueness({ [field]: value });
      })
    ).subscribe(result => {
      if (result) {
        this.uniquenessErrors.update(errors => ({ ...errors, ...result }));
      }
    });
  }

  loadPartner(): void {
    if (!this.partnerId) return;
    this.loading.set(true);
    this.partnerService.getPartner(this.partnerId).subscribe({
      next: (partner) => {
        this.partner = partner;
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load partner');
        this.loading.set(false);
      }
    });
  }

  checkUniqueness(field: string, value: string): void {
    if (this.isEdit) return;
    this.uniquenessCheck$.next({ field, value });
  }

  isFieldDuplicate(field: string): boolean {
    const errors = this.uniquenessErrors();
    return errors[field] === true;
  }

  onCertificateChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      this.partner.clientCertificate = reader.result as string;
    };
    reader.readAsText(file);
  }

  save(): void {
    this.error.set('');

    if (!this.validate()) return;

    this.saving.set(true);

    const obs = this.isEdit && this.partnerId
      ? this.partnerService.updatePartner(this.partnerId, this.partner)
      : this.partnerService.createPartner(this.partner);

    obs.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.router.navigate(['/partners', result.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.detail || err.error?.title || 'Failed to save partner');
      }
    });
  }

  validate(): boolean {
    if (!this.partner.legalName?.trim()) {
      this.error.set('Legal name is required');
      return false;
    }
    if (!this.partner.clientShortName?.trim()) {
      this.error.set('Client short name is required');
      return false;
    }
    if (!this.partner.contactEmail?.trim()) {
      this.error.set('Contact email is required');
      return false;
    }
    if (!this.partner.contactPhone?.trim()) {
      this.error.set('Contact phone is required');
      return false;
    }

    const errors = this.uniquenessErrors();
    if (errors['clientShortName'] || errors['agentId'] || errors['contactEmail'] || errors['contactPhone']) {
      this.error.set('Please fix uniqueness errors before saving');
      return false;
    }

    return true;
  }

  cancel(): void {
    if (this.isEdit && this.partnerId) {
      this.router.navigate(['/partners', this.partnerId]);
    } else {
      this.router.navigate(['/partners']);
    }
  }
}
