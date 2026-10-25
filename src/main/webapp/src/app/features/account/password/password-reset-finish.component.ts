import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AccountService } from '../../../core/services/account.service';

@Component({
  selector: 'app-password-reset-finish',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './password-reset-finish.component.html',
  styleUrl: './password-reset-finish.component.scss'
})
export class PasswordResetFinishComponent implements OnInit {
  key = '';
  newPassword = '';
  confirmPassword = '';
  
  loading = false;
  error = '';
  success = false;

  constructor(
    private accountService: AccountService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.key = this.route.snapshot.queryParams['key'] || '';
    if (!this.key) {
      this.error = 'Invalid reset key';
    }
  }

  finishReset(): void {
    this.error = '';

    if (!this.newPassword) {
      this.error = 'Please enter a new password';
      return;
    }

    if (this.newPassword.length < 4) {
      this.error = 'Password must be at least 4 characters';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.loading = true;

    this.accountService.finishPasswordReset({
      key: this.key,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: () => {
        this.loading = false;
        this.error = 'Password reset failed. The key may be invalid or expired.';
      }
    });
  }
}
