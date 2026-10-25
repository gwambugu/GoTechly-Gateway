import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss'
})
export class PaginationComponent {
  currentPage = input.required<number>();
  totalItems = input.required<number>();
  pageSize = input<number>(20);

  pageChange = output<number>();
  pageSizeChange = output<number>();

  pageSizeOptions = [10, 20, 50, 100];

  totalPages = computed(() => Math.ceil(this.totalItems() / this.pageSize()) || 1);

  pages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: (number | '...')[] = [];

    if (total <= 7) {
      for (let i = 0; i < total; i++) pages.push(i);
      return pages;
    }

    pages.push(0);
    if (current > 2) pages.push('...');

    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);
    for (let i = start; i <= end; i++) pages.push(i);

    if (current < total - 3) pages.push('...');
    pages.push(total - 1);

    return pages;
  });

  startItem = computed(() => this.totalItems() === 0 ? 0 : this.currentPage() * this.pageSize() + 1);
  endItem = computed(() => Math.min((this.currentPage() + 1) * this.pageSize(), this.totalItems()));

  goToPage(page: number | '...'): void {
    if (page === '...' || page === this.currentPage()) return;
    this.pageChange.emit(page);
  }

  onPageSizeChange(event: Event): void {
    const size = +(event.target as HTMLSelectElement).value;
    this.pageSizeChange.emit(size);
  }

  previous(): void {
    if (this.currentPage() > 0) {
      this.pageChange.emit(this.currentPage() - 1);
    }
  }

  next(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.pageChange.emit(this.currentPage() + 1);
    }
  }
}
