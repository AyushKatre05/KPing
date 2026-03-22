"use client";

import { useMonitorLogs, useMonitors } from "@/hooks/useMonitors";
import { useMonitorAnalytics } from "@/hooks/useMonitorAnalytics";
import { useParams } from "next/navigation";
import UptimeChart from "@/components/UptimeChart";
import { ChevronLeft, Activity, Clock, Shield, Globe, AlertTriangle } from "lucide-react";
import Link from "next/link";
import StatusBadge from "@/components/StatusBadge";
import { format } from "date-fns";

export default function MonitorDetail() {
  const { id } = useParams();
  const { monitors } = useMonitors();
  const { logs, loading: logsLoading } = useMonitorLogs(id as string);
  const { analytics, loading: analyticsLoading } = useMonitorAnalytics(id as string, '24h');

  const monitor = monitors.find(m => m.id === id);

  if (!monitor && !logsLoading) {
    return <div className="text-center py-12 text-slate-400">Monitor not found.</div>;
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-700 pb-16">
      <Link href="/" className="inline-flex items-center text-slate-500 hover:text-white transition-colors">
        <ChevronLeft className="h-4 w-4 mr-1" /> Back to Dashboard
      </Link>

      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <div className="flex items-center space-x-3 mb-2">
            <h1 className="text-4xl font-extrabold text-white tracking-tight">{monitor?.name || "Loading..." }</h1>
            {monitor && <StatusBadge status={logs[0]?.status} />}
          </div>
          <p className="text-lg text-slate-400 flex items-center overflow-hidden">
            <Globe className="h-4 w-4 mr-2 flex-shrink-0" />
            <span className="truncate">{monitor?.url}</span>
          </p>
          <div className="flex space-x-4 mt-3">
            <span className="text-xs font-mono text-slate-500 px-2 py-1 bg-slate-900 rounded-md border border-slate-800">
              EXPECTS {monitor?.expectedStatusCode || 200}
            </span>
            <span className="text-xs font-mono text-slate-500 px-2 py-1 bg-slate-900 rounded-md border border-slate-800">
              INTERVAL {monitor?.checkInterval}s
            </span>
            {monitor?.expectedKeyword && (
              <span className="text-xs font-mono text-indigo-400 px-2 py-1 bg-indigo-500/10 rounded-md border border-indigo-500/20">
                KEYWORD "{monitor.expectedKeyword}"
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <Shield className="h-16 w-16 text-emerald-500" />
          </div>
          <p className="text-slate-500 text-sm font-medium mb-1">24h Uptime</p>
          <p className="text-3xl font-bold text-emerald-500">
            {analyticsLoading ? '--' : analytics?.uptimePercentage.toFixed(2)}%
          </p>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <Activity className="h-16 w-16 text-indigo-500" />
          </div>
          <p className="text-slate-500 text-sm font-medium mb-1">Avg. Latency (24h)</p>
          <p className="text-3xl font-bold text-white">
            {analyticsLoading ? '--' : analytics?.averageLatencyMs.toFixed(0)} <span className="text-lg font-normal text-slate-500">ms</span>
          </p>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <AlertTriangle className="h-16 w-16 text-rose-500" />
          </div>
          <p className="text-slate-500 text-sm font-medium mb-1">Total Outages (24h)</p>
          <p className="text-3xl font-bold text-white">
            {analyticsLoading ? '--' : analytics?.totalOutages}
          </p>
        </div>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-bold text-white">Performance Overview</h2>
            <p className="text-slate-500 text-sm">Response time of the last 100 checks</p>
          </div>
        </div>
        {logs.length > 0 ? (
          <UptimeChart logs={logs} />
        ) : (
          <div className="h-64 flex items-center justify-center border-2 border-dashed border-slate-800 rounded-xl">
            <p className="text-slate-600">Collecting initial data...</p>
          </div>
        )}
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="px-8 py-5 border-b border-slate-800">
          <h2 className="text-xl font-bold text-white">Recent Checks</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-950/50 text-slate-500 text-xs uppercase tracking-wider">
                <th className="px-8 py-4 font-semibold">Time</th>
                <th className="px-8 py-4 font-semibold">Status</th>
                <th className="px-8 py-4 font-semibold">Response Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-800/20 transition-colors">
                  <td className="px-8 py-4 text-slate-300 text-sm">
                    {format(new Date(log.checkedAt), 'MMM dd, HH:mm:ss')}
                  </td>
                  <td className="px-8 py-4">
                    <span className={`px-2 py-0.5 rounded-md text-xs font-medium ${
                      log.status < 400 ? 'text-emerald-400 bg-emerald-400/10' : 'text-rose-400 bg-rose-400/10'
                    }`}>
                      {log.status}
                    </span>
                  </td>
                  <td className="px-8 py-4 text-slate-400 text-sm font-mono">
                    {log.responseTime}ms
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
