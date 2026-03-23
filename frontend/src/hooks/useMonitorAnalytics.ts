import { useState, useEffect } from 'react';
import { MonitorAnalytics } from '../types';
import { useAuth } from '@/context/AuthContext';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

export function useMonitorAnalytics(id: string, range: string = '24h') {
  const [analytics, setAnalytics] = useState<MonitorAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const { token, logout } = useAuth();

  useEffect(() => {
    if (!id || !token) return;
    const fetchAnalytics = async () => {
      try {
        setLoading(true);
        const res = await fetch(`${API_URL}/monitors/${id}/analytics?range=${range}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (res.status === 401) logout();
        if (res.ok) {
          const data = await res.json();
          setAnalytics(data);
        }
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchAnalytics();
  }, [id, range, token, logout]);

  return { analytics, loading };
}
