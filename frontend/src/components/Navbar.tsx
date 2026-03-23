import Link from 'next/link';
import { Activity } from 'lucide-react';

export default function Navbar() {
  return (
    <nav className="bg-slate-900 border-b border-slate-800 py-4 px-6 fixed top-0 w-full z-50">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
        <Link href="/" className="flex items-center space-x-2">
          <Activity className="h-6 w-6 text-indigo-500" />
          <span className="text-xl font-bold text-white tracking-tight">KPing</span>
        </Link>
        <div className="flex space-x-6 items-center">
          <Link href="/" className="text-slate-300 hover:text-white transition-colors text-sm font-medium">Dashboard</Link>
          <Link href="/monitors/new" className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-all shadow-lg shadow-indigo-500/20">
            Add Monitor
          </Link>
        </div>
      </div>
    </nav>
  );
}
