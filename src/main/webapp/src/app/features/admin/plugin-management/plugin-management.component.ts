import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PluginService } from '../../../core/services/plugin.service';
import { PluginInfo } from '../../../shared/models/plugin.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-plugin-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './plugin-management.component.html',
  styleUrl: './plugin-management.component.scss'
})
export class PluginManagementComponent implements OnInit {
  plugins = signal<PluginInfo[]>([]);
  disabledPaymentTypes = signal<string[]>([]);
  expandedPlugin = signal<string | null>(null);
  loading = signal(false);
  error = signal('');
  togglingType = signal<string | null>(null);

  searchTerm = signal('');
  modeFilter = signal<'ALL' | 'INWARD' | 'OUTWARD'>('ALL');
  statusFilter = signal<'ALL' | 'ENABLED' | 'DISABLED'>('ALL');
  typeFilter = signal<string>('ALL');

  // Pagination state (0-indexed, matches shared PaginationComponent)
  currentPage = signal(0);
  pageSize = signal(20);

  // Distinct handler types present across all loaded plugins (e.g. outward-validation,
  // iso-server, iso-listener), sorted — drives the Type filter dropdown so it stays in
  // sync with whatever plugin kinds are actually deployed.
  availableTypes = computed(() => {
    const types = new Set<string>();
    for (const p of this.plugins()) {
      for (const h of p.handlers) {
        if (h.handlerType) types.add(h.handlerType);
      }
    }
    return Array.from(types).sort();
  });

  filteredPlugins = computed(() => {
    let result = this.plugins();
    const search = this.searchTerm().toLowerCase().trim();
    const mode = this.modeFilter();
    const status = this.statusFilter();
    const type = this.typeFilter();

    if (type !== 'ALL') {
      result = result.filter(p =>
        p.handlers.some(h => h.handlerType === type)
      );
    }

    if (search) {
      result = result.filter(p =>
        p.jarFileName.toLowerCase().includes(search) ||
        p.paymentTypes.some(pt => pt.toLowerCase().includes(search)) ||
        p.handlers.some(h =>
          h.handlerClassName.toLowerCase().includes(search) ||
          h.handlerType.toLowerCase().includes(search)
        )
      );
    }

    if (mode !== 'ALL') {
      result = result.filter(p =>
        p.handlers.some(h => h.mode === mode)
      );
    }

    if (status !== 'ALL') {
      const disabled = this.disabledPaymentTypes();
      result = result.filter(p => {
        const hasDisabled = p.paymentTypes.some(pt => disabled.includes(pt));
        const hasEnabled = p.paymentTypes.some(pt => !disabled.includes(pt));
        return status === 'DISABLED' ? hasDisabled : hasEnabled;
      });
    }

    return result;
  });

  // Current page slice of the filtered list, fed to the template.
  pagedPlugins = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filteredPlugins().slice(start, start + this.pageSize());
  });

  get hasActiveFilters(): boolean {
    return this.searchTerm() !== '' || this.modeFilter() !== 'ALL'
      || this.statusFilter() !== 'ALL' || this.typeFilter() !== 'ALL';
  }

  constructor(private pluginService: PluginService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.pluginService.getPlugins().subscribe({
      next: (plugins) => {
        this.plugins.set(plugins);
        this.loadDisabledPlugins();
      },
      error: () => {
        this.error.set('Failed to load plugins');
        this.loading.set(false);
      }
    });
  }

  loadDisabledPlugins(): void {
    this.pluginService.getDisabledPlugins().subscribe({
      next: (disabled) => {
        this.disabledPaymentTypes.set(disabled);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load disabled plugins');
        this.loading.set(false);
      }
    });
  }

  toggleExpanded(jarFileName: string): void {
    this.expandedPlugin.set(this.expandedPlugin() === jarFileName ? null : jarFileName);
  }

  // Filter setters reset to page 0 so filtering never strands the view on an empty page.
  setSearch(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(0);
  }

  setModeFilter(value: 'ALL' | 'INWARD' | 'OUTWARD'): void {
    this.modeFilter.set(value);
    this.currentPage.set(0);
  }

  setStatusFilter(value: 'ALL' | 'ENABLED' | 'DISABLED'): void {
    this.statusFilter.set(value);
    this.currentPage.set(0);
  }

  setTypeFilter(value: string): void {
    this.typeFilter.set(value);
    this.currentPage.set(0);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  isDisabled(paymentType: string): boolean {
    return this.disabledPaymentTypes().includes(paymentType);
  }

  togglePlugin(paymentType: string, event: Event): void {
    event.stopPropagation();
    this.togglingType.set(paymentType);

    const action = this.isDisabled(paymentType)
      ? this.pluginService.enablePlugin(paymentType)
      : this.pluginService.disablePlugin(paymentType);

    action.subscribe({
      next: () => {
        this.togglingType.set(null);
        this.loadDisabledPlugins();
      },
      error: () => {
        this.error.set(`Failed to toggle plugin: ${paymentType}`);
        this.togglingType.set(null);
      }
    });
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.modeFilter.set('ALL');
    this.statusFilter.set('ALL');
    this.typeFilter.set('ALL');
    this.currentPage.set(0);
  }
}
