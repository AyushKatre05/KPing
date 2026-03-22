export interface UserDto {
  id: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserDto;
}

export interface Monitor {
  id: string;
  userId: string;
  name: string;
  url: string;
  checkInterval: number;
  expectedStatusCode: number;
  expectedKeyword?: string;
  timeoutMs: number;
  createdAt: string;
}

export interface MonitorLog {
  id: string;
  monitorId: string;
  status: number;
  responseTime: number;
  checkedAt: string;
}

export interface CreateMonitorRequest {
  name: string;
  url: string;
  checkInterval: number;
  expectedStatusCode?: number;
  expectedKeyword?: string;
  timeoutMs?: number;
}

export interface MonitorAnalytics {
  uptimePercentage: number;
  averageLatencyMs: number;
  totalOutages: number;
}

export interface Incident {
  id: string;
  monitorId: string;
  startedAt: string;
  resolvedAt?: string;
  errorCause?: string;
}
