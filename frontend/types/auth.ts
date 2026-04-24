export interface UserDto {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phone: string | null;
  organizationId: string | null;
  departmentId: string | null;
  positionTitle: string | null;
  active: boolean;
  locked: boolean;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  roles: string[];
  permissions: string[];
}

export interface TokenPairResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: UserDto;
}
