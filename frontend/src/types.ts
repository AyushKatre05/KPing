export interface Monitor {
  id: string;
  name: string;
  url: string;
  checkInterval: number;
  createdAt: string;
  status?: number; // Last known status
  responseTime?: number; // Last response time
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
}
