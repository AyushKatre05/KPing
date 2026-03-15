"use client";

import { useMonitors } from "@/hooks/useMonitors";
import MonitorCard from "@/components/MonitorCard";
import { RefreshCcw, Search, Plus } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

export default function Dashboard() {
  const { monitors, loading, error, refresh, deleteMonitor } = useMonitors();
  const [search, setSearch] = useState("");

  const filteredMonitors = monitors.filter(m => 
    m.name.toLowerCase().includes(search.toLowerCase()) || 
    m.url.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white mb-2">Monitor Dashboard</h1>
          <p className="text-slate-400">Total: {monitors.length} | Healthy: {monitors.filter(m => m.status && m.status < 400).length}</p>
        </div>
        <div className="flex items-center space-x-3">
          <button 
            onClick={refresh} 
            disabled={loading}
            className="p-2.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:text-white hover:bg-slate-700 transition-all disabled:opacity-50"
          >
            <RefreshCcw className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <Link 
            href="/monitors/new"
            className="flex items-center space-x-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-lg font-semibold transition-all shadow-lg shadow-indigo-500/20"
          >
            <Plus className="h-5 w-5" />
            <span>Add New</span>
          </Link>
        </div>
      </div>

      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-slate-500" />
        </div>
        <input
          type="text"
          placeholder="Filter monitors by name or URL..."
          className="block w-full pl-10 pr-3 py-3 bg-slate-900 border border-slate-800 rounded-xl leading-5 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 border-transparent transition-all"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading && monitors.length === 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-64 bg-slate-900/50 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : error ? (
        <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-8 text-center text-rose-500">
          <p className="font-semibold mb-2">Connection Error</p>
          <p className="text-sm opacity-80">{error}</p>
        </div>
      ) : filteredMonitors.length === 0 ? (
        <div className="bg-slate-900/50 rounded-xl p-12 text-center border-2 border-dashed border-slate-800">
          <p className="text-slate-500 mb-4 text-lg">No monitors found matching your criteria.</p>
          {monitors.length === 0 && (
            <Link href="/monitors/new" className="text-indigo-400 hover:underline">
              Create your first monitor
            </Link>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredMonitors.map((monitor) => (
            <MonitorCard 
              key={monitor.id} 
              monitor={monitor} 
              onDelete={deleteMonitor}
            />
          ))}
        </div>
      )}
    </div>
  );
}
