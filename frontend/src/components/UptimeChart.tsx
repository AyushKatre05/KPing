"use client";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { MonitorLog } from '../types';
import { format } from 'date-fns';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface UptimeChartProps {
  logs: MonitorLog[];
}

export default function UptimeChart({ logs }: UptimeChartProps) {
  const sortedLogs = [...logs].reverse();
  
  const data = {
    labels: sortedLogs.map(log => format(new Date(log.checkedAt), 'HH:mm:ss')),
    datasets: [
      {
        label: 'Response Time (ms)',
        data: sortedLogs.map(log => log.responseTime),
        borderColor: 'rgb(79, 70, 229)',
        backgroundColor: 'rgba(79, 70, 229, 0.1)',
        tension: 0.4,
        fill: true,
        pointRadius: 2,
        pointHoverRadius: 5,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: 'index' as const,
        intersect: false,
        backgroundColor: '#1e293b',
        titleColor: '#94a3b8',
        bodyColor: '#f8fafc',
        borderColor: '#334155',
        borderWidth: 1,
        padding: 12,
        bodyFont: {
          family: 'Inter, sans-serif'
        },
        callbacks: {
          label: (context: any) => ` ${context.parsed.y}ms`
        }
      },
    },
    scales: {
      x: {
        display: true,
        grid: {
          display: false,
        },
        ticks: {
          maxTicksLimit: 8,
          color: '#64748b',
          font: { size: 10 }
        }
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(51, 65, 85, 0.5)',
        },
        ticks: {
          color: '#64748b',
          font: { size: 10 },
          callback: (value: any) => `${value}ms`
        }
      },
    },
  };

  return (
    <div className="h-64 w-full">
      <Line data={data} options={options} />
    </div>
  );
}
