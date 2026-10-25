import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
  duration?: number;
  dismissing?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toasts: Toast[] = [];
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  private nextId = 0;

  toasts$ = this.toastsSubject.asObservable();

  show(message: string, type: Toast['type'] = 'info', duration = 4000): void {
    const toast: Toast = {
      id: this.nextId++,
      message,
      type,
      duration
    };
    this.toasts = [...this.toasts, toast];
    this.toastsSubject.next(this.toasts);

    if (duration > 0) {
      setTimeout(() => this.dismiss(toast.id), duration);
    }
  }

  dismiss(id: number): void {
    this.toasts = this.toasts.map(t => t.id === id ? { ...t, dismissing: true } : t);
    this.toastsSubject.next(this.toasts);
    setTimeout(() => this.remove(id), 320);
  }

  success(message: string, duration = 4000): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration = 5000): void {
    this.show(message, 'error', duration);
  }

  info(message: string, duration = 4000): void {
    this.show(message, 'info', duration);
  }

  warning(message: string, duration = 4500): void {
    this.show(message, 'warning', duration);
  }

  remove(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
    this.toastsSubject.next(this.toasts);
  }

  clear(): void {
    this.toasts = [];
    this.toastsSubject.next(this.toasts);
  }
}
