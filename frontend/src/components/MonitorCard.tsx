import Link from 'next/link';
import { Monitor } from '../types';
import StatusBadge from './StatusBadge';
import { Globe, Clock, ChevronRight, Trash2 } from 'lucide-react';

interface MonitorCardProps {
  monitor: Monitor;
  onDelete: (id: string) => void;
}

export default function MonitorCard({ monitor, onDelete }: MonitorCardProps) {
  return (
    <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-5 hover:border-indigo-500/50 transition-all group shadow-sm hover:shadow-indigo-500/10">
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center space-x-3">
          <div className="bg-slate-700 p-2.5 rounded-lg group-hover:bg-indigo-500/10 transition-colors">
            <Globe className="h-5 w-5 text-slate-400 group-hover:text-indigo-400" />
          </div>
          <div>
            <h3 className="text-white font-semibold text-lg leading-tight">{monitor.name}</h3>
            <p className="text-slate-400 text-sm truncate max-w-[200px]">{monitor.url}</p>
          </div>
        </div>
        <StatusBadge status={monitor.status} />
      </div>
      
      <div className="grid grid-cols-2 gap-4 mb-6">
        <div className="bg-slate-900/50 rounded-lg p-3 border border-slate-700/50">
          <div className="flex items-center text-slate-500 text-xs mb-1">
            <Clock className="h-3 w-3 mr-1" /> Interval
          </div>
          <div className="text-white font-medium">{monitor.checkInterval}s</div>
        </div>
        <div className="bg-slate-900/50 rounded-lg p-3 border border-slate-700/50">
          <div className="flex items-center text-slate-500 text-xs mb-1">
            <Activity className="h-3 w-3 mr-1" /> Latency
          </div>
          <div className="text-white font-medium">
            {monitor.responseTime !== undefined ? `${monitor.responseTime}ms` : '--'}
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-slate-700/50">
        <button 
          onClick={() => onDelete(monitor.id)}
          className="text-slate-500 hover:text-rose-500 transition-colors p-2"
          title="Delete monitor"
        >
          <Trash2 className="h-4 w-4" />
        </button>
        <Link 
          href={`/monitors/${monitor.id}`}
          className="flex items-center text-indigo-400 hover:text-indigo-300 text-sm font-medium transition-colors"
        >
          View Details <ChevronRight className="h-4 w-4 ml-1" />
        </Link>
      </div>
    </div>
  );
}

function Activity({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
    </svg>
  );
}
