import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AccountService } from '../../../core/services/account.service';
import { ToastService } from '../../../core/services/toast.service';
import { ManagedUserVM } from '../../../shared/models/user.model';
import { Observable, of, timer } from 'rxjs';
import { map, switchMap, catchError } from 'rxjs/operators';

/**
 * Mirrors the server-side constraint on {@code AdminUserDTO.login}: either a UPN
 * (name.surname@domain.com) or a bare directory name. Java's atomic groups have no JS
 * equivalent, so both branches are anchored individually.
 */
const LOGIN_PATTERN = /^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$/;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  // Rendered state that is set asynchronously must be a signal — the app is zoneless.
  loading = signal(false);
  success = signal(false);
  adEnabled = signal(false);
  adDomain = signal<string | null>(null);

  registerForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.maxLength(50)]],
    lastName: ['', [Validators.maxLength(50)]],
    login: ['', [
      Validators.required,
      Validators.minLength(1),
      Validators.maxLength(254),
      Validators.pattern(LOGIN_PATTERN)
    ], [this.loginValidator.bind(this)]],
    email: ['', [
      Validators.required,
      Validators.email,
      Validators.maxLength(254)
    ], [this.emailValidator.bind(this)]],
    password: ['', [
      Validators.required,
      Validators.minLength(4),
      Validators.maxLength(100)
    ]],
    confirmPassword: ['', [Validators.required]]
  }, {
    validators: this.passwordMatchValidator
  });

  ngOnInit(): void {
    this.accountService.getSecurityConfig().subscribe({
      next: cfg => {
        if (cfg.adEnabled) {
          this.adEnabled.set(true);
          this.adDomain.set(cfg.domain);
          // Under AD the directory owns the credential — registerUser() discards anything we
          // send and stores an unusable hash. Drop the controls entirely so they neither
          // render nor block submission.
          this.registerForm.removeControl('password');
          this.registerForm.removeControl('confirmPassword');
        }
      },
      // Probe failed — leave the local password fields in place rather than letting a
      // passwordless registration through on a maybe.
      error: () => {}
    });
  }

  passwordMatchValidator(group: FormGroup) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (password && confirmPassword && password !== confirmPassword) {
      group.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  loginValidator(control: AbstractControl): Observable<ValidationErrors | null> {
    if (!control.value || control.value.length < 1) {
      return of(null);
    }
    return timer(500).pipe(
      switchMap(() => this.accountService.checkLoginAvailable(control.value)),
      map(available => available ? null : { loginTaken: true }),
      catchError(() => of(null))
    );
  }

  emailValidator(control: AbstractControl): Observable<ValidationErrors | null> {
    if (!control.value || !control.value.includes('@')) {
      return of(null);
    }
    return timer(500).pipe(
      switchMap(() => this.accountService.checkEmailAvailable(control.value)),
      map(available => available ? null : { emailTaken: true }),
      catchError(() => of(null))
    );
  }

  get f() {
    return this.registerForm.controls;
  }

  /** Hint under the username field, e.g. "name.surname or name.surname@corp.example.com". */
  get loginHint(): string {
    if (!this.adEnabled()) {
      return '';
    }
    const domain = this.adDomain();
    return domain
      ? `Your Active Directory username — name.surname or name.surname@${domain}`
      : 'Your Active Directory username';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return field ? field.valid && (field.dirty || field.touched) && field.value : false;
  }

  getErrorMessage(fieldName: string): string {
    const field = this.registerForm.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.errors['required']) return 'This field is required';
    if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} characters required`;
    if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} characters allowed`;
    if (field.errors['email']) return 'Please enter a valid email address';
    if (field.errors['pattern']) {
      return this.adEnabled()
        ? 'Enter your directory username, e.g. name.surname or name.surname@domain.com'
        : 'Username can only contain letters, numbers, and . - _ @';
    }
    if (field.errors['loginTaken']) return 'Username is already taken';
    if (field.errors['emailTaken']) return 'Email is already registered';
    if (field.errors['passwordMismatch']) return 'Passwords do not match';

    return 'Invalid value';
  }

  register(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const formValue = this.registerForm.value;

    const payload: ManagedUserVM = {
      login: formValue.login,
      email: formValue.email,
      firstName: formValue.firstName,
      lastName: formValue.lastName
    };
    if (!this.adEnabled()) {
      payload.password = formValue.password;
    }

    this.accountService.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.toastService.success(this.successMessage);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 400) {
          this.toastService.error(
            this.adEnabled()
              ? 'Registration failed. Check the username exists in the directory, or it may already be registered.'
              : 'Registration failed. Login or email may already be in use.'
          );
        } else {
          this.toastService.error('Registration failed. Please try again later.');
        }
      }
    });
  }

  /** An admin approves and assigns the role — there is no self-activation email under AD. */
  get successMessage(): string {
    return this.adEnabled()
      ? 'Registration submitted. An administrator will approve your account and assign your role.'
      : 'Registration successful! Please check your email to activate your account.';
  }
}
