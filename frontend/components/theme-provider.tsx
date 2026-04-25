'use client';

import { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface ThemeCtx {
  theme: Theme;
  resolvedTheme: 'light' | 'dark';
  setTheme: (theme: Theme) => void;
}

const STORAGE_KEY = 'congvan-theme';

const ThemeContext = createContext<ThemeCtx | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>('system');
  const [resolved, setResolved] = useState<'light' | 'dark'>('light');

  // Hydrate từ localStorage
  useEffect(() => {
    const saved = (localStorage.getItem(STORAGE_KEY) as Theme | null) ?? 'system';
    setThemeState(saved);
  }, []);

  // Apply theme to root <html>
  useEffect(() => {
    const root = document.documentElement;
    const apply = () => {
      const sysIsDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const r: 'light' | 'dark' =
        theme === 'system' ? (sysIsDark ? 'dark' : 'light') : theme;
      root.classList.toggle('dark', r === 'dark');
      setResolved(r);
    };
    apply();

    if (theme === 'system') {
      const mq = window.matchMedia('(prefers-color-scheme: dark)');
      mq.addEventListener('change', apply);
      return () => mq.removeEventListener('change', apply);
    }
    return undefined;
  }, [theme]);

  const setTheme = (t: Theme) => {
    localStorage.setItem(STORAGE_KEY, t);
    setThemeState(t);
  };

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme: resolved, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeCtx {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be inside ThemeProvider');
  return ctx;
}

/** Inline script chạy đồng bộ trước khi React hydrate — tránh flash. */
export const themeInitScript = `
(function(){try{
  var t=localStorage.getItem('${STORAGE_KEY}')||'system';
  var d = t==='dark' || (t==='system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  if(d) document.documentElement.classList.add('dark');
}catch(e){}})();
`;
