"use client";

import { useState, useEffect } from 'react';
import { Monitor, MonitorLog, CreateMonitorRequest } from '../types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

export function useMonitors() {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMonitors = async () => {
    try {
      setLoading(true);
      const res = await fetch(`${API_URL}/monitors`);
      if (!res.ok) throw new Error('Failed to fetch monitors');
      const data: Monitor[] = await res.json();
      
      // For each monitor, fetch last status
      const monitorsWithStatus = await Promise.all(data.map(async (m) => {
        try {
          const logRes = await fetch(`${API_URL}/monitors/${m.id}/status`);
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
  };

  const addMonitor = async (req: CreateMonitorRequest) => {
    const res = await fetch(`${API_URL}/monitors`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error('Failed to create monitor');
    await fetchMonitors();
  };

  const deleteMonitor = async (id: string) => {
    const res = await fetch(`${API_URL}/monitors/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error('Failed to delete monitor');
    setMonitors(prev => prev.filter(m => m.id !== id));
  };

  useEffect(() => {
    fetchMonitors();
    const interval = setInterval(fetchMonitors, 30000); // refresh every 30s
    return () => clearInterval(interval);
  }, []);

  return { monitors, loading, error, refresh: fetchMonitors, addMonitor, deleteMonitor };
}

export function useMonitorLogs(id: string) {
  const [logs, setLogs] = useState<MonitorLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const fetchLogs = async () => {
      try {
        const res = await fetch(`${API_URL}/monitors/${id}/status`);
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
  }, [id]);

  return { logs, loading };
}
