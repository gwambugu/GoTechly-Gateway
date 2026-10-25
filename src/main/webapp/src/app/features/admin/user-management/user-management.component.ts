import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserManagementService } from './user-management.service';
import { AdminUserDTO } from '../../../shared/models/user.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type TabType = 'pending' | 'all' | 'online';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  activeTab = signal<TabType>('pending');

  pendingUsers = signal<AdminUserDTO[]>([]);
  allUsers = signal<AdminUserDTO[]>([]);
  onlineUsers = signal<AdminUserDTO[]>([]);

  // Pagination state
  currentPage = signal(0);
  pageSize = signal(20);
  totalUsers = signal(0);

  searchQuery = '';
  isSearching = signal(false);
  loading = signal(false);
  error = signal('');
  successMessage = signal('');

  availableRoles = signal<string[]>([]);

  constructor(private userManagementService: UserManagementService) {}

  ngOnInit(): void {
    this.loadAvailableRoles();
    this.loadPendingUsers();
  }

  private loadAvailableRoles(): void {
    this.userManagementService.getAvailableAuthorities().subscribe({
      next: roles => this.availableRoles.set(roles),
      error: () => this.error.set('Failed to load available roles')
    });
  }

  setActiveTab(tab: TabType): void {
    this.activeTab.set(tab);
    this.clearMessages();

    switch (tab) {
      case 'pending':
        this.loadPendingUsers();
        break;
      case 'all':
        this.currentPage.set(0);
        this.isSearching.set(false);
        this.searchQuery = '';
        this.loadAllUsers();
        break;
      case 'online':
        this.loadOnlineUsers();
        break;
    }
  }

  loadPendingUsers(): void {
    this.loading.set(true);
    this.userManagementService.getPendingUsers().subscribe({
      next: (users) => {
        this.pendingUsers.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load pending users');
        this.loading.set(false);
      }
    });
  }

  loadAllUsers(): void {
    this.loading.set(true);
    this.userManagementService.getAllUsers(this.currentPage(), this.pageSize()).subscribe({
      next: (result) => {
        this.allUsers.set(result.items);
        this.totalUsers.set(result.totalCount);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load users');
        this.loading.set(false);
      }
    });
  }

  loadOnlineUsers(): void {
    this.loading.set(true);
    this.userManagementService.getOnlineUsers().subscribe({
      next: (users) => {
        this.onlineUsers.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load online users');
        this.loading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    if (this.isSearching()) {
      this.search();
    } else {
      this.loadAllUsers();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    if (this.isSearching()) {
      this.search();
    } else {
      this.loadAllUsers();
    }
  }

  approveUser(user: AdminUserDTO): void {
    if (!user.id) return;

    this.loading.set(true);
    this.userManagementService.approveUser(user.id).subscribe({
      next: () => {
        this.successMessage.set(`User ${user.login} has been approved`);
        this.pendingUsers.update(users => users.filter(u => u.id !== user.id));
        this.loading.set(false);
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => {
        this.error.set('Failed to approve user');
        this.loading.set(false);
      }
    });
  }

  rejectUser(user: AdminUserDTO): void {
    if (!user.id) return;

    if (!confirm(`Are you sure you want to reject user ${user.login}?`)) {
      return;
    }

    this.loading.set(true);
    this.userManagementService.rejectUser(user.id).subscribe({
      next: () => {
        this.successMessage.set(`User ${user.login} has been rejected`);
        this.pendingUsers.update(users => users.filter(u => u.id !== user.id));
        this.loading.set(false);
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => {
        this.error.set('Failed to reject user');
        this.loading.set(false);
      }
    });
  }

  toggleUserActivation(user: AdminUserDTO): void {
    if (!user.id) return;

    const newState = !user.activated;
    this.loading.set(true);
    this.userManagementService.setUserEnabled(user.id, newState).subscribe({
      next: () => {
        this.successMessage.set(`User ${user.login} has been ${newState ? 'activated' : 'deactivated'}`);
        this.allUsers.update(users =>
          users.map(u => u.id === user.id ? { ...u, activated: newState } : u)
        );
        this.onlineUsers.update(users =>
          newState ? users : users.filter(u => u.id !== user.id)
        );
        this.loading.set(false);
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => {
        this.error.set('Failed to update user status');
        this.loading.set(false);
      }
    });
  }

  hasRole(user: AdminUserDTO, role: string): boolean {
    return user.authorities?.includes(role) ?? false;
  }

  toggleUserRole(user: AdminUserDTO, role: string, event: Event): void {
    if (!user.id) return;

    const checkbox = event.target as HTMLInputElement;
    const checked = checkbox.checked;
    const currentRoles = user.authorities ?? [];
    const newRoles = checked
      ? [...currentRoles, role]
      : currentRoles.filter(r => r !== role);

    this.loading.set(true);
    this.userManagementService.updateUserRole(user.id, newRoles).subscribe({
      next: () => {
        this.successMessage.set(`Roles updated for ${user.login}`);
        this.allUsers.update(users =>
          users.map(u => u.id === user.id ? { ...u, authorities: newRoles } : u)
        );
        this.loading.set(false);
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: () => {
        // Revert checkbox visually so the UI reflects backend truth
        checkbox.checked = !checked;
        this.error.set('Failed to update user role');
        this.loading.set(false);
      }
    });
  }

  search(): void {
    const query = this.searchQuery.trim();
    if (!query) {
      this.isSearching.set(false);
      this.currentPage.set(0);
      this.loadAllUsers();
      return;
    }

    this.isSearching.set(true);
    this.loading.set(true);
    this.userManagementService.searchUsers(query).subscribe({
      next: (users) => {
        this.allUsers.set(users);
        this.totalUsers.set(users.length);
        this.currentPage.set(0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Search failed');
        this.loading.set(false);
      }
    });
  }

  clearMessages(): void {
    this.error.set('');
    this.successMessage.set('');
  }

}
