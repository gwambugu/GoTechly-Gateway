import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiClientManagementService } from './api-client-management.service';
import { ApiClient, ApiClientCredentials, ApiClientRequest } from '../../../shared/models/api-client.model';

type FilterType = 'all' | 'active' | 'revoked';

@Component({
  selector: 'app-api-client-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './api-client-management.component.html',
  styleUrl: './api-client-management.component.scss'
})
export class ApiClientManagementComponent implements OnInit {
  clients = signal<ApiClient[]>([]);
  filter = signal<FilterType>('all');

  loading = signal(false);
  error = signal('');
  successMessage = signal('');

  showCreateForm = signal(false);
  form: ApiClientRequest = { name: '', description: '', expiresAt: null };
  submitting = signal(false);

  /**
   * The one-time secret reveal. Held only in memory and cleared when the operator dismisses it —
   * the server cannot return it a second time.
   */
  revealed = signal<ApiClientCredentials | null>(null);
  revealedIsRotation = signal(false);
  copied = signal('');

  constructor(private apiClientService: ApiClientManagementService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.apiClientService.getAll().subscribe({
      next: clients => {
        this.clients.set(clients);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load API clients');
        this.loading.set(false);
      }
    });
  }

  filteredClients(): ApiClient[] {
    const filter = this.filter();
    if (filter === 'active') {
      return this.clients().filter(c => c.enabled && !c.expired);
    }
    if (filter === 'revoked') {
      return this.clients().filter(c => !c.enabled || c.expired);
    }
    return this.clients();
  }

  setFilter(filter: FilterType): void {
    this.filter.set(filter);
  }

  openCreateForm(): void {
    this.form = { name: '', description: '', expiresAt: null };
    this.showCreateForm.set(true);
    this.clearMessages();
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
  }

  create(): void {
    if (!this.form.name?.trim()) {
      this.error.set('Name is required');
      return;
    }
    this.submitting.set(true);
    this.clearMessages();
    this.apiClientService.create(this.normalisedForm()).subscribe({
      next: credentials => {
        this.submitting.set(false);
        this.showCreateForm.set(false);
        this.revealedIsRotation.set(false);
        this.revealed.set(credentials);
        this.load();
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Failed to create API client');
      }
    });
  }

  rotate(client: ApiClient): void {
    if (!confirm(`Rotate the secret for "${client.name}"?\n\nThe current secret stops working immediately.`)) {
      return;
    }
    this.clearMessages();
    this.apiClientService.rotateSecret(client.id).subscribe({
      next: credentials => {
        this.revealedIsRotation.set(true);
        this.revealed.set(credentials);
        this.load();
      },
      error: () => this.error.set('Failed to rotate secret')
    });
  }

  revoke(client: ApiClient): void {
    if (!confirm(`Revoke "${client.name}"?\n\nIt will no longer be able to obtain a token.`)) {
      return;
    }
    this.clearMessages();
    this.apiClientService.revoke(client.id).subscribe({
      next: () => {
        this.successMessage.set(`Revoked ${client.name}`);
        this.load();
      },
      error: () => this.error.set('Failed to revoke API client')
    });
  }

  enable(client: ApiClient): void {
    this.clearMessages();
    this.apiClientService.enable(client.id).subscribe({
      next: () => {
        this.successMessage.set(`Enabled ${client.name}`);
        this.load();
      },
      error: () => this.error.set('Failed to enable API client')
    });
  }

  dismissReveal(): void {
    this.revealed.set(null);
    this.copied.set('');
    this.successMessage.set('API client saved. The secret is no longer retrievable.');
  }

  copy(value: string, label: string): void {
    navigator.clipboard.writeText(value).then(
      () => {
        this.copied.set(label);
        setTimeout(() => this.copied.set(''), 2000);
      },
      () => this.error.set('Could not copy to clipboard')
    );
  }

  statusOf(client: ApiClient): { label: string; css: string } {
    if (!client.enabled) {
      return { label: 'Revoked', css: 'revoked' };
    }
    if (client.expired) {
      return { label: 'Expired', css: 'expired' };
    }
    return { label: 'Active', css: 'active' };
  }

  clearMessages(): void {
    this.error.set('');
    this.successMessage.set('');
  }

  private normalisedForm(): ApiClientRequest {
    return {
      name: this.form.name.trim(),
      description: this.form.description?.trim() || null,
      // <input type="datetime-local"> yields a local wall-clock string; make it a real instant.
      expiresAt: this.form.expiresAt ? new Date(this.form.expiresAt).toISOString() : null
    };
  }
}
