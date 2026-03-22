export default function StatusBadge({ status }: { status?: number }) {
  const isUp = status !== undefined && status >= 200 && status < 300;
  
  if (status === undefined) {
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-800 text-slate-400">
        Unknown
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
      isUp ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
    }`}>
      <span className={`w-2 h-2 mr-1.5 rounded-full ${isUp ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`} />
      {isUp ? 'UP' : 'DOWN'}
    </span>
  );
}
