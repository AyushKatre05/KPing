import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "KPing | Uptime Monitoring",
  description: "Production-ready uptime monitoring for your services",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-slate-950 text-slate-200 min-h-screen`}>
        <Navbar />
        <main className="max-w-7xl mx-auto pt-24 pb-12 px-6">
          {children}
        </main>
      </body>
    </html>
  );
}
