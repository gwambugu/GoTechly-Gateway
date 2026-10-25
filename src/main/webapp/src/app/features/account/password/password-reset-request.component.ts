import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../../core/services/account.service';

@Component({
  selector: 'app-password-reset-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './password-reset-request.component.html',
  styleUrl: './password-reset-request.component.scss'
})
export class PasswordResetRequestComponent {
  email = '';
  loading = false;
  error = '';
  success = false;

  constructor(private accountService: AccountService) {}

  requestReset(): void {
    this.error = '';

    if (!this.email) {
      this.error = 'Please enter your email address';
      return;
    }

    this.loading = true;

    this.accountService.requestPasswordReset(this.email).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Email address not found';
      }
    });
  }
}
