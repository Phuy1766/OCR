import type { Metadata } from 'next';
import { Toaster } from 'sonner';
import { QueryProvider } from '@/components/providers/query-provider';
import { ThemeProvider, themeInitScript } from '@/components/theme-provider';
import './globals.css';

export const metadata: Metadata = {
  title: {
    default: 'Hệ thống quản lý công văn',
    template: '%s · Hệ thống quản lý công văn',
  },
  description:
    'Hệ thống quản lý công văn đi/đến tích hợp OCR và chữ ký số — tuân thủ NĐ 30/2020/NĐ-CP và Luật Giao dịch điện tử 2023.',
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="min-h-screen bg-background font-sans antialiased">
        <ThemeProvider>
          <QueryProvider>
            {children}
            <Toaster richColors position="top-right" />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
