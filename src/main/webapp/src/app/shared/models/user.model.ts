export interface LoginVM {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface JWTToken {
  id_token: string;
}

export interface ManagedUserVM {
  login: string;
  email: string;
  /** Omitted when AD is enabled — the directory owns the credential. */
  password?: string;
  firstName?: string;
  lastName?: string;
}

export interface KeyAndPasswordVM {
  key: string;
  newPassword: string;
}

export interface AdminUserDTO {
  id?: number;
  login: string;
  firstName?: string;
  lastName?: string;
  email: string;
  imageUrl?: string;
  activated?: boolean;
  langKey?: string;
  authorities?: string[];
}
