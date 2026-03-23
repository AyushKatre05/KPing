"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMonitors } from "@/hooks/useMonitors";
import { ChevronLeft, Save, Globe, Clock, PlusCircle, CheckCircle, Search, Timer } from "lucide-react";
import Link from "next/link";

export default function AddMonitor() {
  const router = useRouter();
  const { addMonitor } = useMonitors();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    url: "",
    checkInterval: 60,
    expectedStatusCode: 200,
    expectedKeyword: "",
    timeoutMs: 10000,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      if (!formData.url.startsWith('http')) {
        throw new Error("URL must start with http:// or https://");
      }
      
      const payload = { ...formData };
      if (!payload.expectedKeyword) {
        delete (payload as any).expectedKeyword;
      }
      
      await addMonitor(payload);
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add monitor");
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto animate-in slide-in-from-bottom-4 duration-500 pb-12">
      <Link href="/" className="inline-flex items-center text-slate-500 hover:text-white mb-8 transition-colors">
        <ChevronLeft className="h-4 w-4 mr-1" /> Back to Dashboard
      </Link>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
        <div className="bg-indigo-600/10 p-8 border-b border-slate-800">
          <div className="flex items-center space-x-3 mb-2">
            <PlusCircle className="h-6 w-6 text-indigo-500" />
            <h1 className="text-2xl font-bold text-white">Add New Monitor</h1>
          </div>
          <p className="text-slate-400">Set up a new HTTP(S) endpoint to monitor its availability.</p>
        </div>

        <form onSubmit={handleSubmit} className="p-8 space-y-6">
          {error && (
            <div className="bg-rose-500/10 border border-rose-500/20 text-rose-500 p-4 rounded-xl text-sm">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-300 block">Monitor Name</label>
            <input
              type="text"
              required
              placeholder="e.g. My Website API"
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-300 block flex items-center">
              <Globe className="h-4 w-4 mr-2" /> Endpoint URL
            </label>
            <input
              type="url"
              required
              placeholder="https://example.com"
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all"
              value={formData.url}
              onChange={(e) => setFormData({ ...formData, url: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-300 block flex items-center">
                <Clock className="h-4 w-4 mr-2" /> Check Interval (sec)
              </label>
              <select
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all appearance-none"
                value={formData.checkInterval}
                onChange={(e) => setFormData({ ...formData, checkInterval: parseInt(e.target.value) })}
              >
                <option value={30}>30 seconds</option>
                <option value={60}>60 seconds</option>
                <option value={300}>5 minutes</option>
                <option value={600}>10 minutes</option>
              </select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-300 block flex items-center">
                <CheckCircle className="h-4 w-4 mr-2" /> Expected Status Code
              </label>
              <input
                type="number"
                min={100}
                max={599}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all"
                value={formData.expectedStatusCode}
                onChange={(e) => setFormData({ ...formData, expectedStatusCode: parseInt(e.target.value) })}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-300 block flex items-center">
                <Timer className="h-4 w-4 mr-2" /> Timeout (ms)
              </label>
              <input
                type="number"
                min={500}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all"
                value={formData.timeoutMs}
                onChange={(e) => setFormData({ ...formData, timeoutMs: parseInt(e.target.value) })}
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-300 block flex items-center">
                <Search className="h-4 w-4 mr-2" /> Expected Keyword (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. 'ok' or 'success'"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:ring-2 focus:ring-indigo-500/50 outline-none transition-all"
                value={formData.expectedKeyword}
                onChange={(e) => setFormData({ ...formData, expectedKeyword: e.target.value })}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 text-white font-bold py-4 rounded-xl shadow-lg shadow-indigo-500/20 transition-all flex items-center justify-center space-x-2 mt-4"
          >
            {loading ? (
              <RefreshCcw className="h-5 w-5 animate-spin" />
            ) : (
              <>
                <Save className="h-5 w-5" />
                <span>Save Monitor</span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}

function RefreshCcw({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
      <path d="M3 3v5h5"></path>
      <path d="m3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"></path>
      <path d="M16 16h5v5"></path>
    </svg>
  );
}
