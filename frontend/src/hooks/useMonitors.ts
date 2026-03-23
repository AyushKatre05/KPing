"use client";

import { useState, useEffect, useCallback } from 'react';
import { Monitor, MonitorLog, CreateMonitorRequest } from '../types';
import { useAuth } from '@/context/AuthContext';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

export function useMonitors() {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { token, logout } = useAuth();

  const getHeaders = useCallback(() => {
    return {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
  }, [token]);

  const fetchMonitors = useCallback(async () => {
    if (!token) return;
    try {
      setLoading(true);
      const res = await fetch(`${API_URL}/monitors`, { headers: getHeaders() });
      if (res.status === 401) {
        logout();
        return;
      }
      if (!res.ok) throw new Error('Failed to fetch monitors');
      const data: Monitor[] = await res.json();
      
      const monitorsWithStatus = await Promise.all(data.map(async (m) => {
        try {
          const logRes = await fetch(`${API_URL}/monitors/${m.id}/status?limit=1`, { headers: getHeaders() });
          if (logRes.ok) {
            const logs: MonitorLog[] = await logRes.json();
            if (logs.length > 0) {
              return { ...m, status: logs[0].status, responseTime: logs[0].responseTime };
            }
          }
        } catch (e) {
          console.error(`Error fetching logs for ${m.id}`, e);
        }
        return m;
      }));

      setMonitors(monitorsWithStatus);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [token, getHeaders, logout]);

  const addMonitor = async (req: CreateMonitorRequest) => {
    const res = await fetch(`${API_URL}/monitors`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(req),
    });
    if (res.status === 401) logout();
    if (!res.ok) throw new Error('Failed to create monitor');
    await fetchMonitors();
  };

  const deleteMonitor = async (id: string) => {
    const res = await fetch(`${API_URL}/monitors/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (res.status === 401) logout();
    if (!res.ok) throw new Error('Failed to delete monitor');
    setMonitors(prev => prev.filter(m => m.id !== id));
  };

  useEffect(() => {
    fetchMonitors();
    const interval = setInterval(fetchMonitors, 30000);
    return () => clearInterval(interval);
  }, [fetchMonitors]);

  return { monitors, loading, error, refresh: fetchMonitors, addMonitor, deleteMonitor };
}

export function useMonitorLogs(id: string) {
  const [logs, setLogs] = useState<MonitorLog[]>([]);
  const [loading, setLoading] = useState(true);
  const { token, logout } = useAuth();

  useEffect(() => {
    if (!id || !token) return;
    const fetchLogs = async () => {
      try {
        const res = await fetch(`${API_URL}/monitors/${id}/status`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (res.status === 401) logout();
        if (res.ok) {
          const data = await res.json();
          setLogs(data);
        }
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchLogs();
    const interval = setInterval(fetchLogs, 10000);
    return () => clearInterval(interval);
  }, [id, token, logout]);

  return { logs, loading };
}
